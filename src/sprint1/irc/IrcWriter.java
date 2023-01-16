package sprint1.irc;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprint1.data.HQMap;
import sprint1.data.LocationType;
import sprint1.utils.Tuple;

public class IrcWriter {
    private final MapLocation HQ_LOC;
    private final RobotController rc;
    private final int HQ_ID;

    private boolean buffer_has_synced_init;
    private int buffer_read_head;

    public IrcWriter(MapLocation hq_loc, RobotController rc_, int hq_id) throws GameActionException {
        HQ_LOC = hq_loc;
        rc = rc_;
        HQ_ID = hq_id;
        buffer_has_synced_init = false;
        buffer_read_head = IrcConstants.IRC_BUFFER_START;
    }

    public void sync(HQMap map) throws GameActionException {
        while (true) {
            assert buffer_read_head >= IrcConstants.IRC_BUFFER_START && buffer_read_head < IrcConstants.IRC_BUFFER_END;

            int data = rc.readSharedArray(buffer_read_head);

            if (data == 0) {
                break;
            }

            int event_hq_id = data % 2;
            data /= 2;

            IrcEvent event = IrcEvent.fromValue(data % IrcEvent.IRC_EVENT_BITS);
            data /= IrcEvent.IRC_EVENT_BITS;

            if (event_hq_id == HQ_ID) {
                // Can clear data now
                rc.writeSharedArray(buffer_read_head, 0);
            }

            switch (event) {
                case BROADCAST_LOCATION_TYPE:
                    Tuple<MapLocation, LocationType> tuple = IrcEvent.parseBroadcastLocationType(data);
                    map.updateLocationTypeAtMapLocation(tuple.first, tuple.second);
            }

            buffer_read_head = getNthBufferHead(buffer_read_head, 1 + event.getFragLength());
        }
    }

    public void writeBufferEvent(IrcEvent event, int data) throws GameActionException {
        int buffer_write_head = rc.readSharedArray(IrcConstants.IRC_WRITE_HEAD_INT);

        // TODO: Don't do this, use a queue to save for next turn
        assert rc.readSharedArray(buffer_write_head) == 0;

        data *= IrcEvent.IRC_EVENT_BITS;
        data += event.getValue();

        data *= 2;
        data += HQ_ID;

        rc.writeSharedArray(buffer_write_head, data);
        rc.writeSharedArray(IrcConstants.IRC_WRITE_HEAD_INT, getNextBufferHead(buffer_write_head));
    }

    public void writeBufferEvent(IrcEvent event, int data, int frag) throws GameActionException {
        int buffer_write_head = rc.readSharedArray(IrcConstants.IRC_WRITE_HEAD_INT) + 1;

        // TODO: Don't do this, use a queue to save for next turn
        assert rc.readSharedArray(buffer_write_head) == 0;

        writeBufferEvent(event, data);

        rc.writeSharedArray(buffer_write_head, frag << 1 + 1);
        rc.writeSharedArray(IrcConstants.IRC_WRITE_HEAD_INT, getNextBufferHead(buffer_write_head));
    }

    private void writeHqMapWithOffset(HQMap map, int offset) throws GameActionException {
        // Write encoded location to the zeroth array slot
        rc.writeSharedArray(offset++, this.HQ_LOC.x + this.HQ_LOC.y * 100); // TODO (possibly): add
                                                                            // 10000*isFoggyAtHQ?

        for (int a = 0; a < 27; a++) { // 27 = 108 / 4 = (number of viewable locations)/(locations per integer)
            int data = 0;
            int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

            for (int b = 0; b < 4; b++) { // Pack in each of the four locations that can be stored in data
                data = data << 4;
                // This is the next locationType that needs to be encoded
                data += map.getLocationTypeAtBufferIndex(aTimesFour + b).getValue();
            }

            // Write these four locations' encoding to the array and move onto the next
            rc.writeSharedArray(offset++, data);
        }
    }

    private void updateHqMapWithOffset(HQMap map, int offset) throws GameActionException {
        int encoded_hq_location = rc.readSharedArray(offset++);

        int decoded_x = encoded_hq_location % 100;
        int decoded_y = encoded_hq_location / 100;

        map.addOtherHq(new MapLocation(decoded_x, decoded_y));

        for (int a = 0; a < 27; a++) {
            int data = rc.readSharedArray(offset + a);
            int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

            for (int b = 3; b >= 0; b--) {
                map.updateLocationTypeAtBufferIndex(aTimesFour + b, LocationType.fromValue(data % 16));
                data /= 16;
            }
        }
    }

    private void clearBuffer(int start, int end) throws GameActionException {
        for (int i = start; i < end; i++) {
            rc.writeSharedArray(i, 0);
        }
    }

    private static int getNextBufferHead(int head) {
        return (head == IrcConstants.IRC_BUFFER_END - 1) ? IrcConstants.IRC_BUFFER_START : head + 1;
    }

    private static int getNthBufferHead(int head, int n) {
        assert n < IrcConstants.IRC_BUFFER_LEN;

        head += n;

        if (head >= IrcConstants.IRC_BUFFER_END) {
            return head - IrcConstants.IRC_BUFFER_LEN;
        }

        return head;
    }
}