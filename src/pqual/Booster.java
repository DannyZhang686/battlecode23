package pqual;

import battlecode.common.*;

public class Booster extends Robot {
    
    public Booster(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        runSetup();
    }
}
