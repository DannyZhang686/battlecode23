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
    private int HQ_COUNT;

    private boolean DONE_INIT_STATE_SYNC;
    private int buffer_head;

    public IrcWriter(MapLocation hq_loc, RobotController rc_, int hq_id) throws GameActionException {
        HQ_LOC = hq_loc;
        rc = rc_;
        HQ_ID = hq_id;
        HQ_COUNT = 0;

        DONE_INIT_STATE_SYNC = false;
        buffer_head = IrcConstants.IRC_BUFFER_START;

        // +1 to prevent (0,0) HQ
        rc.writeSharedArray(HQ_ID, 1 + HQ_LOC.x + HQ_LOC.y * 64);
    }

    public void sync(HQMap map) throws GameActionException {
        if (!DONE_INIT_STATE_SYNC) {
            DONE_INIT_STATE_SYNC = broadcastInitState(map);
        } else { // keep this an else, need to wait for rest of HQ to finish as well :)
            syncBufferEvents(map);
        }
    }

    // Return value indicates if state is fully synced
    private boolean broadcastInitState(HQMap map) throws GameActionException {
        assert !DONE_INIT_STATE_SYNC;

        int current_round = rc.getRoundNum();

        if (current_round == 1) {
            // First round
            // On the first turn, there's only enough space in the buffer for the first
            // two HQs, so HQ 0 and HQ 1 write, 2 and 3 read

            if (HQ_ID > 0) {
                this.updateHqMapWithOffset(map, 4); // HQ 0 data
            }

            if (HQ_ID > 1) {
                this.updateHqMapWithOffset(map, 32); // HQ 1 data
            }

            if (HQ_ID < 2) {
                // 4 for HQ #0, 32 for HQ #1
                int offset = 4 + HQ_ID * 28;

                this.writeHqMapWithOffset(map, offset);
            }

        } else if (current_round == 2) {
            // Second round
            // Get HQ Count

            for (int i = 0; i < 4; i++) {
                if (rc.readSharedArray(i) != 0) {
                    HQ_COUNT++;
                } else {
                    break;
                }
            }

            if (HQ_COUNT == 1) {
                this.clearBuffer(4, 64);
                return true;
            }

            if (HQ_ID == 0) {
                this.clearBuffer(4, 32);
                this.updateHqMapWithOffset(map, 32);
            }

            if (HQ_ID == 1) {
                // make 1 do this for cheaper bytecode on 0
                this.clearBuffer(32, 64);
            }

            if (HQ_COUNT == 2) {
                return true;
            }

            if (HQ_ID >= 2) {
                // 4 for HQ #2, 32 for HQ #3
                int offset = 4 + (HQ_ID - 2) * 28;

                this.writeHqMapWithOffset(map, offset);
            }

            if (HQ_ID == 3) {
                this.updateHqMapWithOffset(map, 4);
            }

        } else if (current_round == 3) {
            // Third round

            if (HQ_ID < 2) {
                this.updateHqMapWithOffset(map, 4);
            }

            if (HQ_ID < 3) {
                this.updateHqMapWithOffset(map, 32);
            }

            if (HQ_ID == 3) {
                this.clearBuffer(4, 64);
            }

            return true;
        }

        return false;
    }

    private void syncBufferEvents(HQMap map) throws GameActionException {
        while (true) {
            assert buffer_head >= IrcConstants.IRC_BUFFER_START && buffer_head < IrcConstants.IRC_BUFFER_END;

            int data = rc.readSharedArray(buffer_head);

            if (data == 0) {
                break;
            }

            int event_hq_id = data % 2;
            data /= 2;

            IrcEvent event = IrcEvent.fromValue(data % IrcEvent.IRC_EVENT_BITS);
            data /= IrcEvent.IRC_EVENT_BITS;

            switch (event) {
                case BROADCAST_LOCATION_TYPE:
                    Tuple<MapLocation, LocationType> tuple = IrcEvent.parseBroadcastLocationType(data);
                    map.updateLocationTypeAtMapLocation(tuple.first, tuple.second);
            }

            if (event_hq_id == HQ_ID) {
                // Can clear data now
                rc.writeSharedArray(buffer_head, 0);
            }

            buffer_head = (buffer_head == IrcConstants.IRC_BUFFER_END - 1) ? IrcConstants.IRC_BUFFER_START
                    : buffer_head + 1;
        }
    }

    public void writeBufferEvent(IrcEvent event, int data) throws GameActionException {
        // TODO: Don't do this, use a queue to save for next turn
        assert rc.readSharedArray(buffer_head) == 0;

        data *= IrcEvent.IRC_EVENT_BITS;
        data += event.getValue();

        data *= 2;
        data += HQ_ID;

        rc.writeSharedArray(buffer_head, data);
        buffer_head++;
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
}