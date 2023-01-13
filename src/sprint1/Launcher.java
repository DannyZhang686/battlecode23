package sprint1;

import javax.tools.DocumentationTool.Location;

import battlecode.common.*;

public class Launcher extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget; // Where we are aiming to move
    boolean relaxedPathfinding;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;
    
    // Note that if isLeader is false, the launcher is first and
    // foremost a follower, and only considers the hqOrder if no
    // leaders are found nearby
    HQLauncherOrder hqOrder;
    MapLocation hqTargetLocation; // The location referenced in hqOrder
    final boolean isLeader;

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
        if (true) {
            hqOrder = HQLauncherOrder.ESCORT_CARRIERS;
        }
        this.curLocation = rc.getLocation();
        this.hqLocation = new MapLocation(0, 0); // TODO: initialize HQ location properly
        this.friendlyTeam = rc.getTeam();
        this.enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;
        this.isLeader = isLeader(rc.getID());
    }

    @Override
    public void run() throws GameActionException {
        this.curLocation = rc.getLocation();
        // Check for new macro instructions, then try to shoot, then try to move
        checkForInstructions();
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

        RobotInfo[] enemyRobots = rc.senseNearbyRobots(16, enemyTeam);
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
                     (true)) {
                // TODO: replace 'true' with distance function check
                // (between curLocation and the locations for
                //  enemyRobots[i] and curTarget)
                curTarget = enemyRobots[i];
            }
        }

        if (rc.canAttack(curTarget.location)) {
            // Attack!
            rc.attack(curTarget.location);
        } else {
            // Can't attack; the only enemies in range are headquarters
            // TODO: (post-sprint1) trigger special behaviour? (ex. swarm enemy headquarters)
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
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(16, friendlyTeam);
            for (RobotInfo robot : friendlyRobots) { // TODO: iterate in random order?
                if (isLeader(robot.ID)) {
                    // This robot is a leader!
                    curMovementTarget = robot.location;
                    relaxedPathfinding = true;
                    break;
                }
            }
        }
        if (!relaxedPathfinding) {
            // Either we are a leader, or we are a follower without a leader
            // In both cases, we can freely follow HQ orders.
            if (hqOrder == HQLauncherOrder.ESCORT_CARRIERS) {
                // TODO: implement
            }
            else if ((hqOrder == HQLauncherOrder.HOLD_LOCATION) ||
                    (hqOrder == HQLauncherOrder.MASS_ASSAULT_LOCATION)) {
                // Note that these orders mean the same thing, "go somewhere"
                curMovementTarget = hqTargetLocation;
            }
            else if (hqOrder == HQLauncherOrder.PATROL_PATH_TO_LOCATION) {
                // TODO: implement
            }
        }

        // TODO: add checks for the launcher to make itself STOP_MOVING (ex. island)
        // Else if relaxedPathfinding && withinRelaxed(curLocation, curMovementTarget),
        // no need to move (maybe some random motion?)
        // Else, pathfind to curMovementTarget, which should always be initialized
        return;
    }
    
    public static boolean isLeader(int robotID) throws GameActionException {
        return ((robotID ^ (robotID << 4) ^ 232733) % LAUNCHER_LEADER_PROPORTION) == 0;
    }

    public static boolean withinRelaxed(MapLocation a, MapLocation b) throws GameActionException {
        // TODO: replace with builtin distance function (which presumably exists)
        return (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y) <= RELAXED_PATHFINDING_DISTANCE;
    }
}
