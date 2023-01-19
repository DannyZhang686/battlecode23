package rsprint2;

import battlecode.common.*;
import rsprint2.data.*;

public class Launcher extends Robot {
    // Hmm, it really feels like there should be more here...

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        runSetup();
    }
}
