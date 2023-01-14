package sprint1;

import battlecode.common.*;
import java.util.Random;

public abstract class Robot {

    protected RobotController rc;

    // Unified random object for robot usage 
    static final Random rng = new Random(62951413);

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
    }

    public abstract void run() throws GameActionException;
}
