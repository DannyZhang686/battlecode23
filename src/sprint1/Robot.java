package sprint1;

import battlecode.common.*;
import java.util.Random;
import sprint1.irc.HQConsumer;

public abstract class Robot {

    protected final RobotController rc;
    protected final HQConsumer HQ_CONSUMER;

    // Unified random object for robot usage
    static final Random rng = new Random(62951413);

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        this.HQ_CONSUMER = new HQConsumer();
    }

    public abstract void run() throws GameActionException;
}
