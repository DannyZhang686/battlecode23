package sprint1;

import battlecode.common.*;
import sprint1.data.*;

public class Launcher extends Robot {

    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    // Within shooting radius
    RobotInfo[] shootableEnemyRobots;
    // Within vision radius
    RobotInfo[] friendlyRobots, enemyRobots;
    MapLocation[] nearbyEnemyHQLocations = new MapLocation[4];
    int nearbyEnemyHQCount;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;

    // Note that if isLeader is false, the launcher is first and
    // foremost a follower, and only considers the hqOrder if no
    // leaders are found nearby
    HQLauncherOrder hqOrder;
    MapLocation hqTargetLocation; // A location referenced by hqOrder
    int followingCarrierID; // Specifically for the ESCORT_CARRIERS order

    // For the clumping functionality
    int leaderPriority;
    int followingLauncherID;

    // For random movement to go in the same direction for multiple turns
    Direction randomDirection = Direction.CENTER;
    int turnsRemaining = 0;
    // Number of turns for which to stick with the same direction.
    // Launchers should stick pretty tightly together
    public static final int MAX_TURNS_REMAINING = 2;

    // When the number of *offensive* enemies (launchers/destabilizers)
    // in range is not above this value, launchers target other launchers
    // over boosters
    // If the number of *offensive* enemies is above this value, launchers
    // target boosters over other launchers
    public static final int TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD = 5;

    // leaderPriority can only be in [0, MAX_LEADER_PRIORITY)
    // (to prevent one giant clump of launchers from forming)
    public static final int MAX_LEADER_PRIORITY = 10;

    // If relaxedPathfinding is true, being <= RELAXED_PATHFINDING_DISTANCE
    // units from the target is considered equivalent to being at the target
    // Note that relaxedPathfinding is only true when either following a leader
    // (i.e. don't crowd the leader) or patrolling a path
    public static final int RELAXED_PATHFINDING_DISTANCE = 2;

    // Round before which launchers don't move offensively (to defend against early rushes)
    public static final int DEFEND_BEFORE_ROUND = 0;
    
    // When the number of nearby friendly launchers is below this
    // value, there will be no random movement from launchers
    public static final int NEARBY_LAUNCHER_THRESHOLD = 4;

    // Launchers are more willing to push forward:
    // - When the round number is large, and
    // - When there are many nearby launchers.
    // The following constants regulate this behaviour.

    // First round number at which launchers want to push past the center
    public static final int MIN_PUSH_ROUND = 250;
    // Round number at which launchers want to push to the maximum
    public static final int MAX_PUSH_ROUND = 350;

    // Smallest proportion of nearby launchers at which launchers
    // want to push past the center
    public static final double MIN_LAUNCHER_PROPORTION = 0.2;
    // Proportion of nearby launchers at which launchers want to push
    // to the maximum
    public static final double MAX_LAUNCHER_PROPORTION = 0.4;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Properly initialize hqOrder by reading from shared array
        // Note: need to set curMovementTarget if order is to PATROL_PATH_TO_LOCATION
        hqOrder = HQLauncherOrder.ESCORT_CARRIERS;
        curLocation = rc.getLocation();
        followingCarrierID = followingLauncherID = -1;
        leaderPriority = getLeaderPriority(rc.getID());
        
        // hqLocation initialization
        RobotInfo[] nearbyRobots = this.rc.senseNearbyRobots();
        MapLocation[] possibleHQLocations = new MapLocation[nearbyRobots.length];
        int nearbyHQs = 0;

        for (RobotInfo info : nearbyRobots) {
            if (info.getType() == RobotType.HEADQUARTERS) {
                possibleHQLocations[nearbyHQs] = info.getLocation();
                nearbyHQs++;
            }
        }
        hqLocation = possibleHQLocations[rng.nextInt(nearbyHQs)];
    }

    @Override
    public void run() throws GameActionException {
        runSetup();

        curLocation = rc.getLocation();
        // Check for new macro instructions, then try to shoot, then try to move
        // TODO: Integer macro_instruction = HQ_CONSUMER.getUnitCommand(rc.getID());

        shootableEnemyRobots = rc.senseNearbyRobots(16, enemyTeam);
        friendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);
        enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        nearbyEnemyHQCount = 0;
        for (RobotInfo robot : enemyRobots) {
            if (robot.type == RobotType.HEADQUARTERS) {
                nearbyEnemyHQLocations[nearbyEnemyHQCount++] = robot.location;
            }
        }

        if (!tryToShoot()) {
            tryToMove(true); // Try spotting enemies and move
            // Positioning has possibly changed
            curLocation = rc.getLocation();
            shootableEnemyRobots = rc.senseNearbyRobots(16, enemyTeam);
            friendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);
            enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
            tryToShoot();
        }
        else {
            // Even if we shot already, we still want to move
            // But we shouldn't (explicitly) move toward enemies
            // since we don't have another shot this turn
            tryToMove(false);
        }
    }

    // Return value: whether or not the shot was successful
    private boolean tryToShoot() throws GameActionException {
        if (!rc.isActionReady()) {
            // On cooldown
            return false;
        }
        // Else, the robot can shoot

        if (shootableEnemyRobots.length == 0) {
            // Nothing to shoot at
            return false;
        }
        // Else, the robot has a location to shoot at

        // Should we hit launchers or boosters first (after destabilizers)?
        int numOffensiveEnemyRobots = 0;
        for (int i = 0; i < shootableEnemyRobots.length; i++) {
            if ((shootableEnemyRobots[i].type == RobotType.DESTABILIZER) ||
                (shootableEnemyRobots[i].type == RobotType.LAUNCHER)) {
                numOffensiveEnemyRobots++;
            }
        }
        // If numOffensiveEnemyRobots <= TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD,
        // then we hit launchers first. Otherwise, we hit boosters first.

        RobotInfo curTarget = shootableEnemyRobots[0];
        for (int i = 1; i < shootableEnemyRobots.length; i++) {
            // Is shootableEnemyRobots[i] a better target than curTarget?
            // If so, replace curTarget with that target

            // First, check unit types
            if (curTarget.type != shootableEnemyRobots[i].type) {
                RobotType[] attackPriority = new RobotType[6];

                // Destabilizers are the scariest enemies
                attackPriority[0] = RobotType.DESTABILIZER;
                if (numOffensiveEnemyRobots <= TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD) {
                    // Try to win the fight quickly by disabling launchers
                    attackPriority[1] = RobotType.LAUNCHER;
                    attackPriority[2] = RobotType.BOOSTER;
                } else {
                    // Might be a long fight (or might just be a doomed fight);
                    // hit boosters over launchers first
                    attackPriority[1] = RobotType.BOOSTER;
                    attackPriority[2] = RobotType.LAUNCHER;
                }
                attackPriority[3] = RobotType.CARRIER;
                attackPriority[4] = RobotType.AMPLIFIER;
                attackPriority[5] = RobotType.HEADQUARTERS;

                for (int j = 0; j < attackPriority.length; j++) {
                    if (curTarget.type == attackPriority[j]) {
                        // The current target is higher priority
                        break;
                    } else if (shootableEnemyRobots[i].type == attackPriority[j]) {
                        // The new target is higher priority
                        curTarget = shootableEnemyRobots[i];
                        break;
                    }
                }
            }
            // If curTarget and enemyRobot[i] are the same type, we change
            // the target if and only if enemyRobot[i] has lower health
            // This is technically a bit inefficient, but we want to kill
            // off units as much as possible to minimize counterattacks
            else if (shootableEnemyRobots[i].health < curTarget.health) {
                curTarget = shootableEnemyRobots[i];
            }
            // If the health of the robots is the same, we change the target
            // if and only of enemyRobot[i] is strictly closer to us than
            // curTarget (so it's more likely that other launchers are also
            // in range to shoot at our chosen target)
            else if ((shootableEnemyRobots[i].health == curTarget.health) &&
                     (curLocation.distanceSquaredTo(shootableEnemyRobots[i].location) <
                      curLocation.distanceSquaredTo(curTarget.location))) {
                curTarget = shootableEnemyRobots[i];
            }
        }

        if (rc.canAttack(curTarget.location)) {
            // Attack!
            rc.attack(curTarget.location);
            return true;
        }
        // Else, the only enemies in range are headquarters
        // We should then be moving toward the headquarters in tryToMove
        return false;
    }


    private boolean isSafeLocation(MapLocation location) {
        for (int i = 0; i < nearbyEnemyHQCount; i++) {
            if (location.distanceSquaredTo(nearbyEnemyHQLocations[i]) <= 9)
                return false;
        }
        return true;
    }

    private void tryToMove(boolean shouldSpotEnemies) throws GameActionException {
        if (!rc.isMovementReady()) {
            return;
        }
        
        if (shouldSpotEnemies) {
            // Spot enemies and go and fight them (even if order is STOP_MOVING)
            if ((shootableEnemyRobots.length == 0) &&
                (enemyRobots.length != 0) &&
                (rc.isActionReady())) {
                // There are enemies to engage (and we can shoot this turn)!
                Direction dir = curLocation.directionTo(enemyRobots[0].getLocation());
                rc.setIndicatorString(String.valueOf(enemyRobots[0].getLocation()));

                MapLocation newLoc = curLocation.add(dir);
                if (!isSafeLocation(newLoc)) {
                    // if dir is not safe, try a nearby square? (instead of just returning)
                    return;
                }

                if (rc.canMove(dir)) {
                    rc.move(dir);
                    return;
                }
                // Else, we can not step into battle
            }
        }

        if ((rc.getRoundNum() <= DEFEND_BEFORE_ROUND) || (hqOrder == HQLauncherOrder.STOP_MOVING)) {
            // No movement before given round
            return;
        }

        // Changing orders
        // TODO (post-sprint1): add more of these!


        // TODO: edge case when our HQ is next to theirs. We should not just stop moving
        // Rushing enemy headquarters (if spotted)
        for (MapLocation location : nearbyEnemyHQLocations) {
            int dis = curLocation.distanceSquaredTo(location);
            if (dis <= 16 || dis == 18) {
                // Sit as close as possible to HQ without getting shot at (distance 9)
                // watch out: can go from 18 -> 8 in one move
                hqOrder = HQLauncherOrder.STOP_MOVING;
                break;
            }
            else if (curLocation.isWithinDistanceSquared(location, 20)) {
                // Rush headquarters
                hqOrder = HQLauncherOrder.MASS_ASSAULT_LOCATION;
                hqTargetLocation = location;
                break;
            }
        }

        relaxedPathfinding = false; // By default

        // If not a leader, try to follow a leader
        // leaderPriority == 0 implies leader
        // leaderPriority == MAX_LEADER_PRIORITY - 1 implies the robot is "independent"
        // (defends base)
        if ((leaderPriority != 0) &&
            (leaderPriority != MAX_LEADER_PRIORITY-1) &&
            (hqOrder != HQLauncherOrder.MASS_ASSAULT_LOCATION)) {
            MapLocation[] leaderLocations = new MapLocation[friendlyRobots.length];
            int numLeaders = 0;
            boolean foundOldLauncher = false;

            for (int curPriority = 0; curPriority < Math.min(leaderPriority, MAX_LEADER_PRIORITY); ++curPriority) {
                for (RobotInfo robot : friendlyRobots) {
                    if ((robot.type == RobotType.LAUNCHER) &&
                        (curPriority == getLeaderPriority(robot.ID))) {
                        // This robot is a leader (and is a launcher)!
                        if (robot.getID() == followingLauncherID) {
                            // Old launcher is still in range; follow this one
                            // over other launchers
                            foundOldLauncher = true;
                            relaxedPathfinding = true;
                            curMovementTarget = robot.getLocation();
                            break;
                        }
                        leaderLocations[numLeaders] = robot.getLocation();
                        numLeaders++;
                    }
                }
                if ((!foundOldLauncher) && (numLeaders != 0)) {
                    // Pick a random new leader to follow
                    relaxedPathfinding = true;
                    curMovementTarget = leaderLocations[rng.nextInt(numLeaders)];
                    // Note: we re-find the ID to make the leaderLocations array smaller
                    followingLauncherID = rc.senseRobotAtLocation(curMovementTarget).ID;
                    break;
                }
                else if (foundOldLauncher) {
                    // Pretty big bytecode save
                    break;
                }
            }
        }

        // Now, find the correct movementTarget
        if (!relaxedPathfinding) {
            // We do not have a leader to follow, so we can freely follow HQ orders.
            if (hqOrder == HQLauncherOrder.ESCORT_CARRIERS) {
                // Try to pick a random friendly carrier to follow
                MapLocation[] carrierLocations = new MapLocation[friendlyRobots.length];
                int numCarriers = 0;
                boolean foundOldCarrier = false;

                for (RobotInfo robot : friendlyRobots) {
                    if (robot.type == RobotType.CARRIER) {
                        // This robot is a carrier!
                        if (robot.ID == followingCarrierID) {
                            // Old carrier is still in range; follow this one
                            // over other carriers
                            foundOldCarrier = true;
                            break;
                        }
                        carrierLocations[numCarriers] = robot.location;
                        numCarriers++;
                    }
                }
                if (!foundOldCarrier && (numCarriers != 0)) {
                    // Pick a random new carrier to follow
                    relaxedPathfinding = true;
                    curMovementTarget = carrierLocations[rng.nextInt(numCarriers)];
                    // Note: we re-find the ID to make the carrierLocations array smaller
                    followingCarrierID = rc.senseRobotAtLocation(curMovementTarget).ID;
                } else if (!foundOldCarrier && (numCarriers == 0)) {
                    // No carriers in range
                    // Launchers should generally avoid exploring because carriers
                    // are better at it, but we'll let them follow relaxed pathfinding
                    // logic and move randomly
                    curMovementTarget = curLocation;
                }
                if ((rng.nextInt(3) != 0) && (leaderPriority != MAX_LEADER_PRIORITY-1)) {
                    // Special logic for attacking: slowly move toward opposite
                    // corner of HQ location so that launchers are between
                    // enemy and carriers
                    // 1/10 of launchers won't be affected by this
                    int effectiveRoundNum = rc.getRoundNum();
                    int friendlyLauncherCount = 0;

                    effectiveRoundNum = Math.max(effectiveRoundNum, MIN_PUSH_ROUND);
                    effectiveRoundNum = Math.min(effectiveRoundNum, MAX_PUSH_ROUND);
                    for (RobotInfo robot : friendlyRobots) {
                        if (robot.getType() == RobotType.LAUNCHER) {
                            friendlyLauncherCount++;
                        }
                    }

                    double roundNumFactor = (effectiveRoundNum - MIN_PUSH_ROUND) * 1.0f /
                                            (MAX_PUSH_ROUND - MIN_PUSH_ROUND);
                    double launcherCountFactor = friendlyLauncherCount * 1.0f / friendlyRobots.length;
                    launcherCountFactor = Math.max(launcherCountFactor, MIN_LAUNCHER_PROPORTION);
                    launcherCountFactor = Math.min(launcherCountFactor, MAX_LAUNCHER_PROPORTION);
                    launcherCountFactor = (launcherCountFactor - MIN_LAUNCHER_PROPORTION) /
                                          (MAX_LAUNCHER_PROPORTION - MIN_LAUNCHER_PROPORTION);
                    double healthFactor = rc.getHealth() * 1.0f / 20; // Launcher max health

                    // Between -1 and 1
                    double overallFactor = (roundNumFactor + launcherCountFactor + 2 * healthFactor) / 2 - 1;

                    int targetX = (rc.getMapWidth() / 2) + (int) (overallFactor * (rc.getMapWidth() / 2 - hqLocation.x));
                    int targetY = (rc.getMapHeight() / 2) + (int) (overallFactor * (rc.getMapHeight() / 2 - hqLocation.y));

                    curMovementTarget = new MapLocation(targetX, targetY);
                }
            } else if ((hqOrder == HQLauncherOrder.HOLD_LOCATION) ||
                       (hqOrder == HQLauncherOrder.MASS_ASSAULT_LOCATION)) {
                // Note that these orders mean the same thing, "go somewhere"
                curMovementTarget = hqTargetLocation;
            } else if (hqOrder == HQLauncherOrder.PATROL_PATH_TO_LOCATION) {
                // If this condition is false, it's OK to just pathfind to
                // curMovementTarget with no further complications
                if (withinRelaxed(curMovementTarget, curLocation)) {
                    // curMovementTarget should be either hqLocation or hqTargetLocation
                    // We want to set it to whichever one it is not
                    curMovementTarget = (curMovementTarget == hqTargetLocation) ? hqLocation : hqTargetLocation;
                }
            }
        }

        if (hqOrder != HQLauncherOrder.STOP_MOVING) {
            if (relaxedPathfinding && withinRelaxed(curLocation, curMovementTarget)) {
                // We've (essentially) arrived at our destination
                // Do random movement (just for this turn) to avoid blockages
                int friendlyLauncherCount = 0;
                for (RobotInfo robot : friendlyRobots) {
                    if (robot.getType() == RobotType.LAUNCHER) {
                        friendlyLauncherCount++;
                    }
                }
                if (friendlyLauncherCount < NEARBY_LAUNCHER_THRESHOLD) {
                    // Not enough support; we shouldn't move randomly to
                    // ensure we can better defend ourselves
                    return;
                }

                if ((randomDirection != Direction.CENTER) &&
                    rc.canMove(randomDirection) &&
                    (turnsRemaining != 0) &&
                    (rc.senseMapInfo(curLocation.add(randomDirection)).getCurrentDirection() != Direction.CENTER)) {
                    // Try to move in the same direction
                    rc.move(randomDirection);
                    turnsRemaining--;
                }
                else {
                    randomDirection = Direction.CENTER;

                    Direction[] moveableDirections = getMoveableDirections();
                    int n = moveableDirections.length;
                    if (n != 0) {
                        // Move in a random valid direction
                        Direction theDirection = moveableDirections[rng.nextInt(n)];
                        // Avoid currents
                        while ((rc.senseMapInfo(curLocation.add(randomDirection)).getCurrentDirection() != Direction.CENTER)
                               && rng.nextInt(5) == 0) {
                            theDirection = moveableDirections[rng.nextInt(n)];
                        }
                        rc.move(theDirection);
                        randomDirection = theDirection;
                        turnsRemaining = MAX_TURNS_REMAINING;
                    }
                }
            } else {
                // Pathfind to curMovementTarget
                moveTowardsTarget(curMovementTarget);
            }
        }
    }

    public static int getLeaderPriority(int robotID) throws GameActionException {
        return (robotID ^ (robotID << 4) ^ 232733) % MAX_LEADER_PRIORITY;
    }

    public static boolean withinRelaxed(MapLocation a, MapLocation b) throws GameActionException {
        return a.isWithinDistanceSquared(b, RELAXED_PATHFINDING_DISTANCE);
    }
}
