package sprint1;

import battlecode.common.*;

public class Launcher extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean underHQInfluence;
    final boolean isLeader;

    // When the number of enemies in range is not above this value,
    // launchers target other launchers first
    // If the number of enemies is above this value, launchers
    // target destabilizers and boosters first
    final int TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD = 8;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        this.curLocation = rc.getLocation();
        this.friendlyTeam = rc.getTeam();
        this.enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;
        this.underHQInfluence = false;
        this.isLeader = (((rc.getID() ^ 232733) + 8008 + 135) % 6) == 0;
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
        // TODO: Interpret sharedArrayContents, update underHQInfluence
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

        RobotInfo curTarget = enemyRobots[0];
        for (int i = 1; i < enemyRobots.length; i++) {
            // Is enemyRobots[i] a better target than curTarget?
            // If so, replace curTarget with that target

            // First, check unit types
            if (curTarget.type != enemyRobots[i].type) {
                RobotType[] attackPriority = new RobotType[6];
                if (enemyRobots.length <= TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD) {
                    // Try to win the fight quickly by disabling launchers
                    attackPriority[0] = RobotType.LAUNCHER;
                    attackPriority[1] = RobotType.DESTABILIZER;
                    attackPriority[2] = RobotType.BOOSTER;
                } else {
                    // Might be a long fight (or might just be a doomed fight);
                    // hit buffing towers first
                    attackPriority[0] = RobotType.DESTABILIZER;
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
        }

        if (rc.canAttack(curTarget.location)) {
            // Attack!
            rc.attack(curTarget.location);
        } else {
            // Can't attack; the only enemies in range are headquarters
            // TODO: trigger special behaviour? (ex. swarm enemy headquarters)
        }
    }

    private void tryToMove() throws GameActionException {
        if (!rc.isMovementReady()) {
            // On cooldown
            return;
        }
        // Else, the robot can move

        // If not a leader, try to follow a leader
        if (!this.isLeader) {
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(16, friendlyTeam);
            for (RobotInfo robot : friendlyRobots) {
                if ((((robot.ID ^ 232733) + 8008 + 135) % 6) == 0) {
                    // This robot is a leader!
                    Direction dir = this.curLocation.directionTo(robot.location);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
            }
        }

        // Try to find an island
        if (rc.senseIsland(this.curLocation) == -1) {
            return; // Already on island, no movement
        }
        if (rc.senseNearbyIslands().length == 0) {
            // Sadness
            int dir = rc.getID() % 4;
            if ((dir == 0) && (rc.canMove(Direction.EAST))) {
                rc.move(Direction.EAST);
            } else if ((dir == 1) && (rc.canMove(Direction.NORTH))) {
                rc.move(Direction.NORTH);
            } else if ((dir == 2) && (rc.canMove(Direction.WEST))) {
                rc.move(Direction.WEST);
            } else if ((dir == 3) && (rc.canMove(Direction.SOUTH))) {
                rc.move(Direction.SOUTH);
            }
            return;
        }
        int island_index = rc.senseNearbyIslands()[0];
        MapLocation island_loc = rc.senseNearbyIslandLocations(this.curLocation, -1, island_index)[0];
        Direction dir = this.curLocation.directionTo(island_loc);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        return;
    }
}
