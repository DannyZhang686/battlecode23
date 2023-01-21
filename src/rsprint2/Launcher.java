package rsprint2;

import battlecode.common.*;
import rsprint2.data.*;

public class Launcher extends Robot {

    MapLocation curMovementTarget;
    boolean hqSpotted;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;

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

    // When the number of nearby friendly launchers is below this
    // value, there will be no random movement from launchers
    public static final int NEARBY_LAUNCHER_THRESHOLD = 0;

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

        curMovementTarget = null;
        hqSpotted = false;
        followingLauncherID = -1;
        leaderPriority = getLeaderPriority(rc.getID());

        // hqLocation initialization
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);
        MapLocation[] possibleHQLocations = new MapLocation[nearbyFriendlyRobots.length];
        int nearbyHQs = 0;

        for (RobotInfo info : nearbyFriendlyRobots) {
            if (info.getType() == RobotType.HEADQUARTERS) {
                possibleHQLocations[nearbyHQs] = info.getLocation();
                nearbyHQs++;
            }
        }
        hqLocation = possibleHQLocations[rng.nextInt(nearbyHQs)];
    }

    public void recalibrate() throws GameActionException {
        super_recalibrate();
        hqSpotted = false;
        for (int i = 0; i < nearbyEnemyHQCount; i++) { // or just an if-stmt
            hqSpotted = true;
            curMovementTarget = nearbyEnemyHQLocations[0];
        }
    }

    @Override
    public void run() throws GameActionException {
        runSetup();
        recalibrate();
        rc.setIndicatorString(String.valueOf(leaderPriority));

        if ((enemyRobots.length == nearbyEnemyHQCount) && (rc.isMovementReady())) {
            // No enemies in vision
            macroMove();
            recalibrate();
        }

        if ((shootableEnemyRobots.length > shootableEnemyHQCount) && (rc.isActionReady())) {
            // If enemies in shooting range, shoot
            shoot();
            recalibrate();
            // Later code tries to retreat out of range
        } else if ((enemyRobots.length > nearbyEnemyHQCount) && (rc.isActionReady())) {
            // If enemies in vision range, step and shoot (if possible)
            if (rc.isMovementReady()) {
                microMove(true);
                recalibrate();
                if (shootableEnemyRobots.length > shootableEnemyHQCount) {
                    shoot();
                    recalibrate();
                }
            }
        }

        // It's possible we still haven't moved; do so now
        if ((enemyRobots.length == nearbyEnemyHQCount) && (rc.isMovementReady())) {
            macroMove();
            recalibrate();
        } else if ((enemyRobots.length > nearbyEnemyHQCount) && (rc.isMovementReady())) {
            // Back away
            microMove(false);
            recalibrate();
        }
    }

    private void shoot() throws GameActionException {
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
                    (rc_loc.distanceSquaredTo(shootableEnemyRobots[i].location) < rc_loc
                            .distanceSquaredTo(curTarget.location))) {
                curTarget = shootableEnemyRobots[i];
            }
        }

        assert rc.canAttack(curTarget.location);

        rc.attack(curTarget.location);
    }

    private void macroMove() throws GameActionException {
        if (hqSpotted) {
            // curMovementTarget already set correctly
        } else if ((leaderPriority != 0) && (leaderPriority != MAX_LEADER_PRIORITY - 1)) {
            MapLocation[] leaderLocations = new MapLocation[friendlyRobots.length];
            int numLeaders = 0;
            boolean foundOldLauncher = false;
            curMovementTarget = null;

            for (int curPriority = 0; curPriority < Math.min(leaderPriority, MAX_LEADER_PRIORITY); ++curPriority) {
                for (RobotInfo robot : friendlyRobots) {
                    if ((robot.type == RobotType.LAUNCHER) &&
                            (curPriority == getLeaderPriority(robot.ID))) {
                        // This robot is a leader (and a launcher)!
                        if (robot.getID() == followingLauncherID) {
                            // Old launcher is still in range; follow this one
                            // over other launchers
                            foundOldLauncher = true;
                            curMovementTarget = robot.getLocation();
                            break;
                        }
                        leaderLocations[numLeaders] = robot.getLocation();
                        numLeaders++;
                    }
                }
                if ((!foundOldLauncher) && (numLeaders != 0)) {
                    // Pick a random new leader to follow
                    curMovementTarget = leaderLocations[rng.nextInt(numLeaders)];
                    // Note: we re-find the ID to make the leaderLocations array smaller
                    followingLauncherID = rc.senseRobotAtLocation(curMovementTarget).ID;
                    break;
                } else if (foundOldLauncher) {
                    // Pretty big bytecode save
                    break;
                }
            }
        } else if (leaderPriority != MAX_LEADER_PRIORITY - 1) {
            // Slowly move toward opposite corner of HQ location
            // 1/10 of launchers won't be affected by this
            int effectiveRoundNum = rc.getRoundNum();
            effectiveRoundNum = Math.max(effectiveRoundNum, MIN_PUSH_ROUND);
            effectiveRoundNum = Math.min(effectiveRoundNum, MAX_PUSH_ROUND);
            double roundNumFactor = (effectiveRoundNum - MIN_PUSH_ROUND) * 1.0f /
                    (MAX_PUSH_ROUND - MIN_PUSH_ROUND);

            int friendlyLauncherCount = 0;
            for (RobotInfo robot : friendlyRobots) {
                if (robot.getType() == RobotType.LAUNCHER) {
                    friendlyLauncherCount++;
                }
            }
            double launcherCountFactor = friendlyRobots.length == 0 ? MIN_LAUNCHER_PROPORTION
                    : friendlyLauncherCount * 1.0f / friendlyRobots.length;
            launcherCountFactor = Math.max(Math.min(launcherCountFactor, MAX_LAUNCHER_PROPORTION),
                    MIN_LAUNCHER_PROPORTION);
            launcherCountFactor = (launcherCountFactor - MIN_LAUNCHER_PROPORTION) /
                    (MAX_LAUNCHER_PROPORTION - MIN_LAUNCHER_PROPORTION);

            double healthFactor = rc.getHealth() / 200.0f; // Launcher max health

            // Between -1 and 1
            double overallFactor = (roundNumFactor + launcherCountFactor + 2 * healthFactor) / 2 - 1;

            assert overallFactor >= -1 && overallFactor <= 1;

            int targetX = (rc.getMapWidth() / 2)
                    + (int) (overallFactor * (rc.getMapWidth() / 2 - hqLocation.x));
            int targetY = (rc.getMapHeight() / 2)
                    + (int) (overallFactor * (rc.getMapHeight() / 2 - hqLocation.y));

            curMovementTarget = new MapLocation(targetX, targetY);
        }

        if (curMovementTarget == null) {
            // Random movement to avoid blockages
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
                    (rc.senseMapInfo(rc_loc.add(randomDirection)).getCurrentDirection() != Direction.CENTER)) {
                // Try to move in the same direction
                curMovementTarget = rc_loc.add(randomDirection);
                turnsRemaining--;
            } else {
                randomDirection = Direction.CENTER;

                Direction[] moveableDirections = getMoveableDirections();
                int n = moveableDirections.length;
                if (n != 0) {
                    // Move in a random valid direction
                    Direction theDirection = moveableDirections[rng.nextInt(n)];
                    // Avoid currents
                    while ((rc.senseMapInfo(rc_loc.add(randomDirection)).getCurrentDirection() != Direction.CENTER)
                            && rng.nextInt(5) == 0) {
                        theDirection = moveableDirections[rng.nextInt(n)];
                    }
                    curMovementTarget = rc_loc.add(theDirection);
                    randomDirection = theDirection;
                    turnsRemaining = MAX_TURNS_REMAINING;
                }
            }
        } else {
            moveTowardsTarget(curMovementTarget);
        }
    }

    private void microMove(boolean towardEnemy) throws GameActionException {
        // Get away from those in outer vision ring, plus the 13 square (>= 13)
        MapLocation target = rc_loc;

        for (RobotInfo ri : enemyRobots) { // including enemy HQ
            if (((ri.getType() == RobotType.LAUNCHER) || (ri.getType() == RobotType.DESTABILIZER))
                    && (ri.location.distanceSquaredTo(rc_loc) >= 13)) {
                // Consider this robot in the calculations
                target = target.add(ri.location.directionTo(rc_loc));
            }
        }

        Direction d = rc_loc.directionTo(target);

        if (d != Direction.CENTER) {
            if (towardEnemy) {
                d = d.opposite();
            }
            moveTowardsTarget(rc_loc.add(d));
        }
    }

    public static int getLeaderPriority(int robotID) throws GameActionException {
        return (robotID ^ (robotID << 4) ^ 232733) % MAX_LEADER_PRIORITY;
    }
}
