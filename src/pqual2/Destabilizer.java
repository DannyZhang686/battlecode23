package pqual2;

import battlecode.common.*;

public class Destabilizer extends Robot {

    public Destabilizer(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        runSetup();
    }
}
