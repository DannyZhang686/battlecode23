package rsprint2.irc;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class IrcReader {
    private final RobotController rc;

    public IrcReader(RobotController rc) {
        this.rc = rc;
    }

    public Integer getUnitCommand(int unit_id) {
        return null;
    }

    public int addAndGetHQID() throws GameActionException {
        int hq_id = rc.readSharedArray(IrcConstants.IRC_HQ_ID_INT);
        rc.writeSharedArray(IrcConstants.IRC_HQ_ID_INT, hq_id + 1);
        return hq_id;
    }
}