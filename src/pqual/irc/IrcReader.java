package pqual.irc;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class IrcReader {
    private final RobotController rc;

    private int buffer_read_head;

    public IrcReader(RobotController rc) throws GameActionException {
        this.rc = rc;

        buffer_read_head = rc.readSharedArray(IrcConstants.IRC_WRITE_HEAD_INT);
    }

    public Integer getUnitCommand(int unit_id) {
        return null;
    }

    public int addAndGetHQID() throws GameActionException {
        int hq_id = rc.readSharedArray(IrcConstants.IRC_HQ_ID_INT);
        rc.writeSharedArray(IrcConstants.IRC_HQ_ID_INT, hq_id + 1);
        return hq_id;
    }

    public void sync() throws GameActionException {
        while (true) {
            assert buffer_read_head >= IrcConstants.IRC_BUFFER_START && buffer_read_head < IrcConstants.IRC_BUFFER_END;

            int data = rc.readSharedArray(buffer_read_head);

            if (data == 0) {
                break;
            }

            // int event_hq_id = data % 4;
            data /= 4;

            IrcEvent event = IrcEvent.fromValue(data % IrcEvent.IRC_EVENT_BITS);
            data /= IrcEvent.IRC_EVENT_BITS;

            int data_len = 1 + event.getFragLength();

            switch (event) {
                case INIT_HQ_SYNC:
                    break;
                case BROADCAST_LOCATION_TYPE:
                    break;
                case HOLD_LOCATION:
                    System.out.println("HOLD LOCATION RECV");
            }

            buffer_read_head = IrcUtils.getNthBufferHead(buffer_read_head, data_len);
        }
    }
}