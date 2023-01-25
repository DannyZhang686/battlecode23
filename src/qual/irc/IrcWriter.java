package qual.irc;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import qual.data.HQMap;
import qual.data.LocationType;
import qual.utils.LinkedList;
import qual.utils.Tuple;

public class IrcWriter {
    private final RobotController rc;
    private final int HQ_ID;

    private int buffer_read_head;
    private int buffer_write_head;

    private final LinkedList<IrcMessage> buffer_queue;
    private boolean buffer_init_hq_synced;

    public IrcWriter(MapLocation hq_loc, RobotController rc_, int hq_id) throws GameActionException {
        rc = rc_;
        HQ_ID = hq_id;

        if (HQ_ID == 0) {
            rc.writeSharedArray(IrcConstants.IRC_WRITE_HEAD_INT, IrcConstants.IRC_BUFFER_START);
        }

        buffer_read_head = IrcConstants.IRC_BUFFER_START;
        buffer_write_head = -1;

        buffer_queue = new LinkedList<>();
        buffer_init_hq_synced = false;
    }

    public void sync(HQMap map) throws GameActionException {

        while (true) {
            assert buffer_read_head >= IrcConstants.IRC_BUFFER_START && buffer_read_head < IrcConstants.IRC_BUFFER_END;

            int data = rc.readSharedArray(buffer_read_head);

            if (data == 0) {
                break;
            }

            int event_hq_id = data % 4;
            data /= 4;

            IrcEvent event = IrcEvent.fromValue(data % IrcEvent.IRC_EVENT_BITS);
            data /= IrcEvent.IRC_EVENT_BITS;

            int data_len = 1 + event.getFragLength();

            if (event_hq_id == HQ_ID) {
                // Can clear data now
                clearBuffer(buffer_read_head, data_len);
            } else {
                switch (event) {
                    case INIT_HQ_SYNC:
                        map.addOtherHq(IrcEvent.parseInitHqSyncEvent(data));
                        processInitHqSyncEventFrag(map, IrcUtils.getNextBufferHead(buffer_read_head));
                    case BROADCAST_LOCATION_TYPE:
                        Tuple<MapLocation, LocationType> tuple = IrcEvent.parseBroadcastLocationType(data);
                        map.updateLocationTypeAtMapLocation(tuple.first, tuple.second);
                }
            }

            buffer_read_head = IrcUtils.getNthBufferHead(buffer_read_head, data_len);
        }

        buffer_write_head = rc.readSharedArray(IrcConstants.IRC_WRITE_HEAD_INT);

        if (rc.getRoundNum() <= 1) {
            return;
        }

        while (buffer_queue.size != 0) {
            IrcMessage msg = buffer_queue.head.val;

            if (!writeBufferEvent(msg.getEvent(), msg.getData(), msg.getFrag())) {
                break;
            }

            buffer_queue.dequeue();
        }

        if (!buffer_init_hq_synced && Clock.getBytecodesLeft() >= 12500) {
            buffer_queue.add(new IrcMessage(IrcEvent.INIT_HQ_SYNC, IrcEvent.serializeInitHqSyncEvent(rc.getLocation()),
                    buildInitHqSyncEventFrag(map)));
            buffer_init_hq_synced = true;
        }
    }

    private boolean writeBufferEvent(IrcEvent event, int data, int frag[]) throws GameActionException {
        for (int i = 0; i < 1 + (frag == null ? 0 : frag.length); i++) {
            if (rc.readSharedArray(IrcUtils.getNthBufferHead(buffer_write_head, i)) != 0) {
                return false;
            }
        }

        data *= IrcEvent.IRC_EVENT_BITS;
        data += event.getValue();

        data *= 4;
        data += HQ_ID;

        rc.writeSharedArray(buffer_write_head, data);

        if (frag == null) {
            buffer_write_head = IrcUtils.getNextBufferHead(buffer_write_head);
            rc.writeSharedArray(IrcConstants.IRC_WRITE_HEAD_INT, buffer_write_head);
            return true;
        }

        for (int i = 0; i < frag.length; i++) {
            assert rc.readSharedArray(IrcUtils.getNthBufferHead(buffer_write_head, 1 + i)) == 0;
            rc.writeSharedArray(IrcUtils.getNthBufferHead(buffer_write_head, 1 + i), frag[i]);
        }

        buffer_write_head = IrcUtils.getNthBufferHead(buffer_write_head, 1 + frag.length);
        rc.writeSharedArray(IrcConstants.IRC_WRITE_HEAD_INT, buffer_write_head);

        return true;
    }

    private static int[] buildInitHqSyncEventFrag(HQMap map) throws GameActionException {
        int[] frag = new int[27];

        for (int a = 0; a < 27; a++) { // 27 = 108 / 4 = (number of viewable locations)/(locations per integer)
            int data = 0;
            int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

            for (int b = 0; b < 4; b++) { // Pack in each of the four locations that can
                // be stored in data
                data = data << 4;
                // This is the next locationType that needs to be encoded
                data += map.getLocationTypeAtBufferIndex(aTimesFour + b).getValue();
            }

            // Write these four locations' encoding to the array and move onto the next
            frag[a] = data;
        }

        return frag;
    }

    private void processInitHqSyncEventFrag(HQMap map, int frag_offset) throws GameActionException {
        for (int a = 0; a < 27; a++) {
            int frag_data = rc.readSharedArray(IrcUtils.getNthBufferHead(frag_offset, a));
            int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

            for (int b = 3; b >= 0; b--) {
                map.updateLocationTypeAtBufferIndex(aTimesFour + b, LocationType.fromValue(frag_data % 16));
                frag_data /= 16;
            }
        }
    }

    private void clearBuffer(int start, int len) throws GameActionException {
        for (int i = 0; i < len; i++) {
            rc.writeSharedArray(IrcUtils.getNthBufferHead(start, i), 0);
        }
    }
}