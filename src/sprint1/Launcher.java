package sprint1;

import battlecode.common.*;
import sprint1.data.*;

public class Launcher extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    RobotInfo[] enemyRobots; // Within shooting radius
    RobotInfo[] friendlyRobots; // Within vision radius

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

    // Round at which launchers push toward the opposite corner
    // instead of just the center (for ESCORT_CARRIERS mission)
    public static final int PUSH_FARTHER_ROUND = 350;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Properly initialize hqOrder by reading from shared array
        // Note: need to set curMovementTarget if order is to PATROL_PATH_TO_LOCATION
        hqOrder = HQLauncherOrder.ESCORT_CARRIERS;
        curLocation = rc.getLocation();
        followingCarrierID = followingLauncherID = -1;
        friendlyTeam = rc.getTeam();
        enemyTeam = (friendlyTeam == Team.A) ? Team.B : Team.A;
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

        // rc.setIndicatorString(String.valueOf(leaderPriority));

        this.curLocation = rc.getLocation();
        // Check for new macro instructions, then try to shoot, then try to move
        // TODO: Integer macro_instruction = HQ_CONSUMER.getUnitCommand(rc.getID());

        // enemyRobots is used by both tryToShoot and tryToMove
        enemyRobots = rc.senseNearbyRobots(16, enemyTeam);
        friendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);
        tryToShoot();
        tryToMove();
    }

    private void tryToShoot() throws GameActionException {
        if (!rc.isActionReady()) {
            // On cooldown
            return;
        }
        // Else, the robot can shoot

        if (enemyRobots.length == 0) {
            // Nothing to shoot at
            return;
        }
        // Else, the robot has a location to shoot at

        // Should we hit launchers or boosters first (after destabilizers)?
        int numOffensiveEnemyRobots = 0;
        for (int i = 0; i < enemyRobots.length; i++) {
            if ((enemyRobots[i].type == RobotType.DESTABILIZER) ||
                (enemyRobots[i].type == RobotType.LAUNCHER)) {
                numOffensiveEnemyRobots++;
            }
        }
        // If numOffensiveEnemyRobots <= TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD,
        // then we hit launchers first. Otherwise, we hit boosters first.

        RobotInfo curTarget = enemyRobots[0];
        for (int i = 1; i < enemyRobots.length; i++) {
            // Is enemyRobots[i] a better target than curTarget?
            // If so, replace curTarget with that target

            // First, check unit types
            if (curTarget.type != enemyRobots[i].type) {
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
                    } else if (enemyRobots[i].type == attackPriority[j]) {
                        // The new target is higher priority
                        curTarget = enemyRobots[i];
                        break;
                    }
                }
            }
            // If curTarget and enemyRobot[i] are the same type, we change
            // the target if and only if enemyRobot[i] has lower health
            // This is technically a bit inefficient, but we want to kill
            // off units as much as possible to minimize counterattacks
            else if (enemyRobots[i].health < curTarget.health) {
                curTarget = enemyRobots[i];
            }
            // If the health of the robots is the same, we change the target
            // if and only of enemyRobot[i] is strictly closer to us than
            // curTarget (so it's more likely that other launchers are also
            // in range to shoot at our chosen target)
            else if ((enemyRobots[i].health == curTarget.health) &&
                     (curLocation.distanceSquaredTo(enemyRobots[i].location) <
                      curLocation.distanceSquaredTo(curTarget.location))) {
                curTarget = enemyRobots[i];
            }
        }

        if (rc.canAttack(curTarget.location)) {
            // Attack!
            rc.attack(curTarget.location);
        } else {
            // Can't attack; the only enemies in range are headquarters
            // TODO (post-sprint1): trigger special behaviour? (ex. swarm enemy
            // headquarters)
        }
    }

    private void tryToMove() throws GameActionException {
        if ((!rc.isMovementReady()) || (hqOrder == HQLauncherOrder.STOP_MOVING)) {
            // No movement
            return;
        }
        // Else, the robot can move

        // Changing orders
        // TODO (post-sprint1): add more of these!

        // Rushing enemy headquarters (if spotted)
        if (hqOrder != HQLauncherOrder.MASS_ASSAULT_LOCATION) {
            for (RobotInfo robot : rc.senseNearbyRobots(-1, enemyTeam)) {
                // Sit next to enemy headquarters (spawn camping)
                if ((robot.type == RobotType.HEADQUARTERS) &&
                    (curLocation.isWithinDistanceSquared(robot.location, 20))) {
                    hqOrder = HQLauncherOrder.MASS_ASSAULT_LOCATION;
                    hqTargetLocation = robot.location;
                    break;
                }
            }
        }

        relaxedPathfinding = false; // By default

        // If not a leader, try to follow a leader
        if ((leaderPriority != 0) && (hqOrder != HQLauncherOrder.MASS_ASSAULT_LOCATION)) {
            MapLocation[] leaderLocations = new MapLocation[friendlyRobots.length];
            int numLeaders = 0;
            boolean foundOldLauncher = false;

            for (int curPriority = 0; curPriority < Math.min(leaderPriority, MAX_LEADER_PRIORITY); ++curPriority) {
                for (RobotInfo robot : friendlyRobots) {
                    if ((robot.type == RobotType.LAUNCHER) &&
                        (curPriority == getLeaderPriority(robot.ID))) {
                        // This robot is a leader (and is a launcher)!
                        if (robot.ID == followingLauncherID) {
                            // Old carrier is still in range; follow this one
                            // over other carriers
                            foundOldLauncher = true;
                            break;
                        }
                        leaderLocations[numLeaders] = robot.location;
                        numLeaders++;
                    }
                }
                if (!foundOldLauncher && (numLeaders != 0)) {
                    // Pick a random new leader to follow
                    relaxedPathfinding = true;
                    curMovementTarget = leaderLocations[rng.nextInt(numLeaders)];
                    // Note: we re-find the ID to make the leaderLocations array smaller
                    followingLauncherID = rc.senseRobotAtLocation(curMovementTarget).ID;
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
                if (rng.nextInt(3) == 0) {
                    // Special logic for keeping carriers safe: slowly move toward
                    // opposite corner of HQ location so that launchers are between
                    // enemy and carriers
                    if (rc.getRoundNum() < PUSH_FARTHER_ROUND) {
                        // Be a bit safer (only go for center)
                        curMovementTarget = new MapLocation(rc.getMapWidth() / 2,
                                                            rc.getMapHeight() / 2);
                    }
                    else {
                        curMovementTarget = new MapLocation(rc.getMapWidth() - hqLocation.x,
                                                            rc.getMapHeight() - hqLocation.y);
                    }
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
