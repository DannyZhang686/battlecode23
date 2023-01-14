package sprint1;

import battlecode.common.*;
import java.util.Random;
import sprint1.irc.HQConsumer;
import sprint1.utils.RobotMath;

public abstract class Robot {

    protected final RobotController rc;
    protected MapLocation rc_loc;
    protected final HQConsumer HQ_CONSUMER;

    // Unified random object for robot usage
    static final Random rng = new Random(62951413);

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        rc_loc = rc.getLocation();
        HQ_CONSUMER = new HQConsumer();
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
            if (dir == null) {
                return false;
            }

            assert rc.canMove(dir);

            // if (!rc.canMove(dir)) {
            // System.out.println("attempting to move to " + dir.toString() + " but can't");
            // return false;
            // }

            rc.move(dir);
            return true;
        }

        return false;
    }

    private Direction moveTowardsTarget() throws GameActionException {
        assert rc.getType() != RobotType.HEADQUARTERS;
        assert current_target != null;
        assert rc_loc != null;

        Direction dir = rc_loc.directionTo(current_target);

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
            bug_wall_state = dirn.rotateLeft();
            Direction perp = bug_wall_state.opposite();

            if (rc.canMove(perp)) {
                return perp;
            }

            Direction perpalt = RobotMath.getNextDirection(perp);
            if (rc.canMove(perpalt)) {
                return perpalt;
            }

            return null;
        }

        bug_wall_state = dirp.rotateRight();
        Direction perp = bug_wall_state.opposite();

        if (rc.canMove(perp)) {
            return perp;
        }

        Direction perpalt = RobotMath.getPreviousDirection(perp);
        if (rc.canMove(perpalt)) {
            return perpalt;
        }

        return null;
    }

    protected WellInfo findClosestWell() {
        WellInfo[] wells = rc.senseNearbyWells();

        if (wells.length == 0) {
            return null;
        } else if (wells.length == 1) {
            return wells[0];
        }

        WellInfo closest_well = wells[0];
        int closest_well_dist = rc_loc.distanceSquaredTo(closest_well.getMapLocation());

        for (int i = 1; i < wells.length; i++) {
            int dist = rc_loc.distanceSquaredTo(wells[i].getMapLocation());

            if (dist < closest_well_dist) {
                closest_well = wells[i];
                closest_well_dist = dist;
            }
        }

        return closest_well;
    }
}
