package sprint1;

import battlecode.common.*;
import java.util.Random;
import sprint1.irc.IrcReader;
import sprint1.utils.RobotMath;

public abstract class Robot {

    protected final RobotController rc;
    protected MapLocation rc_loc;
    protected final IrcReader irc_reader;

    protected final MapLocation MAP_CENTER;

    // Unified random object for robot usage
    protected final Random rng;

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
    }

    public abstract void run() throws GameActionException;

    protected void runSetup() {
        rc_loc = rc.getLocation();
    }

    protected MapLocation current_target = null;
    protected Direction bug_wall_state = null;

    protected void setCurrentTarget(MapLocation ct) {
        current_target = ct;
        bug_wall_state = null;
    }

    protected boolean moveTowardsTarget(MapLocation targetLocation) throws GameActionException {
        if (rc.isMovementReady()) {
            rc_loc = rc.getLocation();
            setCurrentTarget(targetLocation);

            Direction dir = moveTowardsTarget();
            if ((dir == null) || (dir == Direction.CENTER)) {
                return false;
            }

            tryToMoveInDirection(dir);
            return true;
        }

        return false;
    }

    private Direction moveTowardsTarget() throws GameActionException {
        assert rc.getType() != RobotType.HEADQUARTERS;
        assert current_target != null;
        assert rc_loc != null;

        Direction dir = rc_loc.directionTo(current_target);
        if (dir == Direction.CENTER) {
            return null;
        }

        int dir_raylen = 0;
        MapLocation curloc = rc_loc.add(dir);
        boolean dir_valid = true;

        assert rc.canSenseLocation(curloc);

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
            return null;
        }

        if (dir_raylen > Math.min(dirn_raylen, dirp_raylen)) {
            return dir;
        }

        if (dir_raylen == Math.min(dirn_raylen, dirp_raylen)) {
            if (rng.nextInt(2) == 0) {
                int temp = dirn_raylen;
                dirn_raylen = dirp_raylen;
                dirp_raylen = temp;
            }
        }

        if (dirn_raylen < dirp_raylen) {
            // bug_wall_state = dirn.rotateLeft();
            // Direction perp = bug_wall_state.opposite();
            bug_wall_state = dirn;
            
            return dirp;
        }

        // bug_wall_state = dirp.rotateRight();
        // Direction perp = bug_wall_state.opposite();
        bug_wall_state = dirp;
        return dirn;
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

    protected boolean tryToMoveInDirection(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            // Great :)
            rc.move(dir);
            return true;
        } else {
            Direction next = RobotMath.getNextDirection(dir);
            Direction prev = RobotMath.getPreviousDirection(dir);
            if (rng.nextInt(2) == 0) {
                if (rc.canMove(next)) {
                    rc.move(next);
                    return true;
                } else if (rc.canMove(prev)) {
                    rc.move(prev);
                    return true;
                }
            } else {
                // Opposite order
                if (rc.canMove(prev)) {
                    rc.move(prev);
                    return true;
                } else if (rc.canMove(next)) {
                    rc.move(next);
                    return true;
                }
            }
            Direction next2 = RobotMath.getNextDirection(next);
            Direction prev2 = RobotMath.getPreviousDirection(prev);
            if (rng.nextInt(2) == 0) {
                if (rc.canMove(next2)) {
                    rc.move(next2);
                    return true;
                } else if (rc.canMove(prev2)) {
                    rc.move(prev2);
                    return true;
                }
            } else {
                // Opposite order
                if (rc.canMove(prev2)) {
                    rc.move(prev2);
                    return true;
                } else if (rc.canMove(next2)) {
                    rc.move(next2);
                    return true;
                }
            }
        }
        // Couldn't move
        return false;
    }

    public Direction[] getMoveableDirections() {
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
}
