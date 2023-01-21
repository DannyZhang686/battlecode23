package rsprint2;

import battlecode.common.*;

public strictfp class RobotPlayer {

    public static void run(RobotController rc) throws GameActionException {
        Robot robot;
        
        switch (rc.getType()) {
            case AMPLIFIER:
                robot = new Amplifier(rc);
                break;
            case BOOSTER:
                robot = new Booster(rc);
                break;
            case CARRIER:
                robot = new Carrier(rc);
                break;
            case DESTABILIZER:
                robot = new Destabilizer(rc);
                break;
            case HEADQUARTERS:
                robot = new Headquarters(rc);
                break;
            case LAUNCHER:
                robot = new Launcher(rc);
                break;
            default:
                System.out.println("The unit type " + rc.getType() + " is not implemented.");
                return;
        }

        while (true) {
            try {
                robot.run();
                Clock.yield();
            }
            catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
