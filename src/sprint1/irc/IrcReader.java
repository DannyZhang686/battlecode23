package sprint1.irc;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class IrcReader {
    private final RobotController rc;

    public IrcReader(RobotController rc) {
        this.rc = rc;
    }

    public Integer getUnitCommand(int unit_id) {
        return null;
    }

    public MapLocation readHqLocation(int HQ_ID) throws GameActionException {
        int data = rc.readSharedArray(HQ_ID) - 1;

        if (data == -1) {
            return null;
        }

        return new MapLocation(data % 64, data / 64);
    }
}