package sprint1;

import battlecode.common.*;
import sprint1.utils.*;
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
    final boolean isLeader;
    int followingCarrierID; // Specifically for the ESCORT_CARRIERS order

    // When the number of *offensive* enemies (launchers/destabilizers)
    // in range is not above this value, launchers target other launchers
    // over boosters
    // If the number of *offensive* enemies is above this value, launchers
    // target boosters over other launchers
    public static final int TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD = 5;

    // 1/LAUNCHER_LEADER_PROPORTION of all launchers are leaders
    public static final int LAUNCHER_LEADER_PROPORTION = 2;

    // If relaxedPathfinding is true, being <= RELAXED_PATHFINDING_DISTANCE
    // units from the target is considered equivalent to being at the target
    // Note that relaxedPathfinding is only true when either following a leader
    // (i.e. don't crowd the leader) or patrolling a path (i.e. 5 units is
    // close enough to the target)
    public static final int RELAXED_PATHFINDING_DISTANCE = 5;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Properly initialize hqOrder by reading from shared array
        // Note: need to set curMovementTarget if order is to PATROL_PATH_TO_LOCATION
        if (true) {
            hqOrder = HQLauncherOrder.ESCORT_CARRIERS;
        }
        curLocation = rc.getLocation();
        hqLocation = new MapLocation(0, 0); // TODO: initialize HQ location properly
        followingCarrierID = -1;
        friendlyTeam = rc.getTeam();
        enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;
        isLeader = isLeader(rc.getID());
    }

    @Override
    public void run() throws GameActionException {
        rc.setIndicatorString(isLeader ? "L" : "F");

        if (rc.getRoundNum() > 10) {
            return;
        }

        this.curLocation = rc.getLocation();
        // Check for new macro instructions, then try to shoot, then try to move
        checkForInstructions();

        // enemyRobots is used by both tryToShoot and tryToMove
        enemyRobots = rc.senseNearbyRobots(16, enemyTeam);
        friendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);
        tryToShoot();
        tryToMove();
    }

    private void checkForInstructions() throws GameActionException {
        // Read every slot of the shared array
        int[] sharedArrayContents = new int[64];
        for (int i = 0; i < 64; i++) {
            sharedArrayContents[i] = rc.readSharedArray(i);
        }
        // TODO: Interpret sharedArrayContents, possibly update orders
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
                    (curLocation.distanceSquaredTo(enemyRobots[i].location) < curLocation
                            .distanceSquaredTo(curTarget.location))) {
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

        relaxedPathfinding = false; // By default

        // If not a leader, try to follow a leader
        if (!this.isLeader) {
            MapLocation[] leaderLocations = new MapLocation[friendlyRobots.length];
            int numLeaders = 0;

            for (RobotInfo robot : friendlyRobots) {
                if (isLeader(robot.ID)) {
                    // This robot is a leader!
                    leaderLocations[numLeaders] = robot.location;
                    numLeaders++;
                }
            }
            if (numLeaders != 0) {
                // Pick a random leader to follow
                relaxedPathfinding = true;
                curMovementTarget = leaderLocations[rng.nextInt(numLeaders)];
            }
        }

        // Now, find the correct movementTarget
        if (!relaxedPathfinding) {
            // Either we are a leader, or we are a follower without a leader
            // In both cases, we can freely follow HQ orders.
            if (hqOrder == HQLauncherOrder.ESCORT_CARRIERS) {
                // Try to pick a random friendly carrier to follow
                MapLocation[] carrierLocations = new MapLocation[friendlyRobots.length];
                int numCarriers = 0;
                boolean foundOldCarrier = false;

                for (RobotInfo robot : friendlyRobots) {
                    if (robot.type == RobotType.CARRIER) {
                        // This robot is a carrier!
                        if (robot.ID == followingCarrierID) {
                            // Old carrier is still in range; follow that one
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

        // STOP_MOVING checks
        // TODO (post-sprint1): add more checks for the launcher to STOP_MOVING
        // ex. on (enemy/any) island
        // Can also add checks to rescind STOP_MOVING order

        // This check shouldn't overrule direct HQ orders
        if (hqOrder == HQLauncherOrder.MASS_ASSAULT_LOCATION) {
            for (RobotInfo robot : enemyRobots) {
                // Sitting right next to enemy headquarters (spawn camping)
                if ((robot.type == RobotType.HEADQUARTERS) &&
                        (curLocation.isWithinDistanceSquared(robot.location, 2))) {
                    hqOrder = HQLauncherOrder.STOP_MOVING;
                    break;
                }
            }
        }

        if (hqOrder != HQLauncherOrder.STOP_MOVING) {
            if (relaxedPathfinding && withinRelaxed(curLocation, curMovementTarget)) {
                // Do random movement (just for this turn) to avoid blockages
                int numMoveableDirections = 0;
                Direction[] moveableDirections = new Direction[8];

                for (Direction dir : Constants.ALL_DIRECTIONS) {
                    if (rc.canMove(dir)) {
                        moveableDirections[numMoveableDirections] = dir;
                        numMoveableDirections++;
                    }
                }
                if (numMoveableDirections != 0) {
                    // Move in a random valid direction
                    rc.move(moveableDirections[rng.nextInt(numMoveableDirections)]);
                }
            } else {
                // TODO: pathfind to curMovementTarget
                // Triple<MapLocation, Direction, Direction> path =
                // RobotMath.moveTowardsTarget(this.rc,
                // this.rc.getLocation(),
                // new MapLocation[] { curMovementTarget });

                // if (path == null) {
                // return;
                // }

                // if (rc.canMove(path.second)) {
                // rc.move(path.second);
                // }
            }
        }
    }

    public static boolean isLeader(int robotID) throws GameActionException {
        return ((robotID ^ (robotID << 4) ^ 232733) % LAUNCHER_LEADER_PROPORTION) == 0;
    }

    public static boolean withinRelaxed(MapLocation a, MapLocation b) throws GameActionException {
        return a.isWithinDistanceSquared(b, RELAXED_PATHFINDING_DISTANCE);
    }
}
