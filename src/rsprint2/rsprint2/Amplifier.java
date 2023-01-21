package rsprint2;

import battlecode.common.*;

public class Amplifier extends Robot {

    public Amplifier(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        runSetup();
    }
}
