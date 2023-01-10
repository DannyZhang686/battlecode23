package sprint1;

import battlecode.common.*;

public abstract class Robot {

    protected RobotController rc;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
    }

    public abstract void run() throws GameActionException;
}
