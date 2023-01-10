package sprint1;

import battlecode.common.*;

public abstract class Robot {
    
    public Robot(RobotController rc) throws GameActionException {}

    public abstract void run() throws GameActionException;
}
