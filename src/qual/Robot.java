package qual;

import battlecode.common.*;
import java.util.Random;
import qual.irc.IrcReader;
import qual.utils.RobotMath;

public abstract class Robot {

    protected final RobotController rc;
    protected MapLocation rc_loc;
    protected final IrcReader irc_reader;

    protected final MapLocation MAP_CENTER;

    // Unified random object for robot usage
    protected final Random rng;

    Team friendlyTeam, enemyTeam;

    protected MapLocation current_target = null;
    int turnCount;
    Direction lockedDirection;
    public static final int MAX_FRUSTRATED_TURNS = 10;

    // Within shooting radius
    RobotInfo[] shootableEnemyRobots;
    // Within vision radius
    RobotInfo[] friendlyRobots, enemyRobots; // note: enemy HQ is a robot too
    MapLocation[] nearbyEnemyHQLocations = new MapLocation[4];
    int nearbyEnemyHQCount, shootableEnemyHQCount;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        rng = new Random(31415926 ^ 271828 ^ rc.getID());
        rc_loc = rc.getLocation();
        irc_reader = new IrcReader(rc);

        // MAP_CENTER initialization
        int center_x = rc.getMapWidth() / 2;
        int center_y = rc.getMapHeight() / 2;

        if (rc_loc.x > center_x) {
            center_x++;
        }

        if (rc_loc.y > center_y) {
            center_y++;
        }

        MAP_CENTER = new MapLocation(center_x, center_y);

        friendlyTeam = rc.getTeam();
        enemyTeam = (friendlyTeam == Team.A) ? Team.B : Team.A;
    }

    public abstract void run() throws GameActionException;

    protected void runSetup() throws GameActionException {
        super_recalibrate();
    }

    protected void super_recalibrate() throws GameActionException {
        rc_loc = rc.getLocation();

        enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        shootableEnemyRobots = rc.senseNearbyRobots(16, enemyTeam);
        friendlyRobots = rc.senseNearbyRobots(-1, friendlyTeam);

        nearbyEnemyHQCount = shootableEnemyHQCount = 0;

        for (RobotInfo robot : enemyRobots) {
            if (robot.type == RobotType.HEADQUARTERS) {
                nearbyEnemyHQLocations[nearbyEnemyHQCount++] = robot.getLocation();
                if (rc_loc.distanceSquaredTo(robot.getLocation()) <= 16) {
                    shootableEnemyHQCount++;
                }
            }
        }
    }

    protected WellInfo findClosestWell() {
        WellInfo[] wells = rc.senseNearbyWells();
        WellInfo closestWell = null;
        int closestWellDistance = 100000;

        for (WellInfo well : wells) {
            int wellDistance = rc_loc.distanceSquaredTo(well.getMapLocation());
            if (wellDistance < closestWellDistance) {
                closestWellDistance = wellDistance;
                closestWell = well;
            }
        }

        return closestWell;
    }

    protected WellInfo findClosestResourceWell(ResourceType rType) {
        WellInfo[] wells = rc.senseNearbyWells();
        WellInfo closestWell = null;
        int closestWellDistance = 100000;

        for (WellInfo well : wells) {
            // Only consider wells with that resource
            if (well.getResourceType() == rType) {
                int wellDistance = rc_loc.distanceSquaredTo(well.getMapLocation());
                if (wellDistance < closestWellDistance) {
                    closestWellDistance = wellDistance;
                    closestWell = well;
                }
            }
        }

        return closestWell;
    }

    protected Direction[] getMoveableDirections() {
        int n = 0;
        Direction[] moveableDirections = new Direction[8];

        for (Direction dir : Constants.ALL_DIRECTIONS) {
            if (rc.canMove(dir)) {
                moveableDirections[n++] = dir;
            }
        }
        // resize it
        Direction[] ret = new Direction[n];
        while (n-- > 0)
            ret[n] = moveableDirections[n];

        return ret;
    }

    // Returns the optimal direction to move in; does *not* do any actual moving and
    // does *not* check that the optimal direction to move in is valid
    // Assumption: vision range is either 4, 20, or 34
    protected Direction optimalDirection(MapLocation targetLocation) throws GameActionException {
        boolean[] isRayPassable = new boolean[Constants.rays.length];
        for (int i = 0; i < Constants.rays.length; i++) {
            for (int[] tile : Constants.rays[i]) {
                // Note that tile has coordinates for relative locations
                MapLocation curTile = rc.getLocation().translate(tile[0], tile[1]);
                if (!rc.canSenseLocation(curTile)) {
                    break;
                } else if (!rc.sensePassability(curTile)) {
                    isRayPassable[i] = false;
                    break;
                }
            }
        }

        // TODO: check if the direction we want to go is passable
        // If so, return that direction
        // If not, do the oPoint logic below, and take the oPoint minimizing
        // distance (with distance given by max(dx, dy), *not* distanceSquared!)
        int bestOPointIndex = -1;
        int oPointDistance = 10000;

        for (int i = 0; i < Constants.rays.length; i++) {
            boolean isOPoint = isRayPassable[i] &&
                    ((isRayPassable[i] != isRayPassable[(i + 1) % Constants.rays.length]) ||
                            (isRayPassable[(i + Constants.rays.length - 1)
                                    % Constants.rays.length] != isRayPassable[i]));
        }

        if (bestOPointIndex == -1) {
            // Uhhhh (:
            return null;
        }
        return null;
    }

    protected void setCurrentTarget(MapLocation ct) {
        current_target = ct;
    }

    protected boolean moveTowardsTarget(MapLocation targetLocation) throws GameActionException {
        if (rc.isMovementReady()) {
            if (targetLocation != current_target) {
                turnCount = 0;
                lockedDirection = Direction.CENTER;
            }

            setCurrentTarget(targetLocation);

            Direction dir = pathTowardsTarget();

            if ((dir == null) || (dir == Direction.CENTER)) {
                return false;
            }

            boolean moved = tryToMoveInDirection(dir);

            if (moved) {
                super_recalibrate();
            }

            return moved;
        }

        return false;
    }

    // move towards a direction without a destination
    protected boolean moveTowardsDirection(Direction target_dir) throws GameActionException {
        if (rc.isMovementReady()) {
            Direction dir = pathTowardsDirection(target_dir);

            if ((dir == null) || (dir == Direction.CENTER)) {
                return false;
            }

            boolean moved = tryToMoveInDirection(dir);

            if (moved) {
                super_recalibrate();
            }

            return moved;
        }

        return false;
    }

    private Direction pathTowardsTarget() throws GameActionException {
        assert rc.getType() != RobotType.HEADQUARTERS;
        assert current_target != null;
        assert rc_loc != null;

        Direction dir = rc_loc.directionTo(current_target);

        if (dir == Direction.CENTER) {
            return null;
        }

        if ((turnCount >= MAX_FRUSTRATED_TURNS) && (lockedDirection == Direction.CENTER)) {
            turnCount = MAX_FRUSTRATED_TURNS;
            lockedDirection = rc.getID() % 2 == 0 ? dir.rotateLeft().rotateLeft() : dir.rotateRight().rotateRight();
        } else if ((turnCount > 0) && (lockedDirection != Direction.CENTER)) {
            turnCount--;
            return lockedDirection;
        } else if ((turnCount <= 0) && (lockedDirection != Direction.CENTER)) {
            turnCount = 0;
            lockedDirection = Direction.CENTER;
        }

        if ((turnCount >= MAX_FRUSTRATED_TURNS) && (lockedDirection == Direction.CENTER)) {
            turnCount = MAX_FRUSTRATED_TURNS;
            lockedDirection = rc.getID() % 2 == 0 ? dir.rotateLeft().rotateLeft() : dir.rotateRight().rotateRight();
        } else if ((turnCount > 0) && (lockedDirection != Direction.CENTER)) {
            turnCount--;
            return lockedDirection;
        } else if ((turnCount <= 0) && (lockedDirection != Direction.CENTER)) {
            turnCount = 0;
            lockedDirection = Direction.CENTER;
        }

        return pathTowardsDirection(dir);
    }

    private Direction pathTowardsDirection(Direction dir) throws GameActionException {
        int dir_raylen = 0;
        MapLocation curloc = rc_loc.add(dir);
        boolean dir_valid = true;

        if (!rc.canSenseLocation(curloc))
            return null;

        while (rc.canSenseLocation(curloc)) {
            if (rc.sensePassability(curloc) && (dir_raylen != 0 || !rc.canSenseRobotAtLocation(curloc))) {
                dir_raylen++;
                curloc = curloc.add(dir);
            } else {
                dir_valid = false;
                break;
            }
        }

        if (dir_valid) {
            turnCount = 0;
            return dir;
        }

        Direction dirp = RobotMath.getPreviousDirection(dir);
        Direction dirn = RobotMath.getNextDirection(dir);

        int dirp_raylen = 0;
        int dirn_raylen = 0;

        curloc = rc_loc.add(dirp);
        while (rc.canSenseLocation(curloc) && rc.sensePassability(curloc) && !rc.canSenseRobotAtLocation(curloc)) {
            dirp_raylen++;
            curloc = curloc.add(dirp);
        }

        curloc = rc_loc.add(dirn);
        while (rc.canSenseLocation(curloc) && rc.sensePassability(curloc) && !rc.canSenseRobotAtLocation(curloc)) {
            dirn_raylen++;
            curloc = curloc.add(dirn);
        }

        if (dir_raylen == 0 && dirn_raylen == 0 && dirp_raylen == 0) {
            turnCount++;
            return rc.getID() % 2 == 0 ? dirp.rotateLeft() : dirn.rotateRight();
        }

        if (dir_raylen > Math.min(dirn_raylen, dirp_raylen)) {
            turnCount = 0;
            return dir;
        }

        if (dir_raylen == Math.min(dirn_raylen, dirp_raylen)) {
            if (dirn_raylen == dirp_raylen) {
                turnCount = 0;
                return dir;
            }
        }

        // Guaranteed to return a suboptimal direction
        turnCount++;
        if (dirn_raylen < dirp_raylen) {
            return dirp;
        }
        return dirn;
    }

    private boolean tryToMoveInDirection(Direction dir) throws GameActionException {
        if (okMove(dir)) {
            // Great :)
            rc.move(dir);
            return true;
        } else {
            Direction next = RobotMath.getNextDirection(dir);
            Direction prev = RobotMath.getPreviousDirection(dir);
            if (rng.nextInt(2) == 0) {
                if (okMove(next)) {
                    rc.move(next);
                    return true;
                } else if (okMove(prev)) {
                    rc.move(prev);
                    return true;
                }
            } else {
                // Opposite order
                if (okMove(prev)) {
                    rc.move(prev);
                    return true;
                } else if (okMove(next)) {
                    rc.move(next);
                    return true;
                }
            }
            Direction next2 = RobotMath.getNextDirection(next);
            Direction prev2 = RobotMath.getPreviousDirection(prev);
            if (rng.nextInt(2) == 0) {
                if (okMove(next2)) {
                    rc.move(next2);
                    return true;
                } else if (okMove(prev2)) {
                    rc.move(prev2);
                    return true;
                }
            } else {
                // Opposite order
                if (okMove(prev2)) {
                    rc.move(prev2);
                    return true;
                } else if (okMove(next2)) {
                    rc.move(next2);
                    return true;
                }
            }
            Direction next3 = RobotMath.getNextDirection(next2);
            Direction prev3 = RobotMath.getPreviousDirection(prev2);
            if (rng.nextInt(2) == 0) {
                if (okMove(next3)) {
                    rc.move(next3);
                    return true;
                } else if (okMove(prev3)) {
                    rc.move(prev3);
                    return true;
                }
            } else {
                // Opposite order
                if (okMove(prev3)) {
                    rc.move(prev3);
                    return true;
                } else if (okMove(next3)) {
                    rc.move(next3);
                    return true;
                }
            }
            if ((rng.nextInt(2) == 0) && okMove(dir.opposite())) {
                rc.move(dir.opposite()); // (:
                return true;
            }
        }
        // Couldn't move
        return false;
    }

    private boolean okMove(Direction dir) throws GameActionException {
        if (!rc.canSenseLocation(rc_loc.add(dir))) {
            // Moving off the map, maybe?
            return false;
        } else {
            MapInfo curLocation = rc.senseMapInfo(rc_loc);
            MapInfo theLocation = rc.senseMapInfo(rc_loc.add(dir));

            if ((rng.nextInt(3) != 0) &&
                    (curLocation.getCurrentDirection() == Direction.CENTER) &&
                    (theLocation.getCurrentDirection() != Direction.CENTER)) {
                return false;
            }

            if ((rng.nextInt(10) != 0) &&
                    (curLocation.hasCloud()) &&
                    theLocation.hasCloud()) {
                return false;
            }
        }

        return rc.canMove(dir) && (!isSafeLocation(rc_loc) || (isSafeLocation(rc_loc.add(dir)) || justMove()));
    }

    // Just move to avoid traffic
    private boolean justMove() throws GameActionException {
        for (Direction d : Constants.ALL_DIRECTIONS) {
            if (rc.canMove(d) && isSafeLocation(rc_loc.add(d))) {
                return false;
            }
        }

        return rng.nextInt(400) == 0;
    }

    private boolean isSafeLocation(MapLocation location) {
        for (int i = 0; i < nearbyEnemyHQCount; i++) {
            if (location.isWithinDistanceSquared(nearbyEnemyHQLocations[i], 9)) {
                return false;
            }
        }

        return true;
    }
}
