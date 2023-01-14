package sprint1.irc;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprint1.data.HQMap;
import sprint1.data.LocationType;

public class IrcWriter {
    private final MapLocation HQ_LOC;
    private final RobotController rc;
    private final int HQ_ID;
    private boolean AT_MOST_TWO_HQ;
    private boolean DONE_INIT_STATE_SYNC;

    public IrcWriter(MapLocation HQ_LOC, RobotController rc, int HQ_ID) {
        this.HQ_LOC = HQ_LOC;
        this.rc = rc;
        this.HQ_ID = HQ_ID;
        this.AT_MOST_TWO_HQ = false;
        this.DONE_INIT_STATE_SYNC = false;
    }

    public void sync(HQMap map) throws GameActionException {
        if (!this.DONE_INIT_STATE_SYNC) {
            this.DONE_INIT_STATE_SYNC = this.broadcastInitState(map);
        }

    }

    // Return value indicates if state is fully synced
    private boolean broadcastInitState(HQMap map) throws GameActionException {
        if (DONE_INIT_STATE_SYNC) {
            System.out.println("this shouldn't be called if state sync is done!");
            return true;
        }

        int current_round = rc.getRoundNum();

        if (current_round == 1) {
            // First round
            // On the first turn, there's only enough space in the buffer for the first
            // two HQs, so we only consider HQs 0 and 1 on this turn

            if (HQ_ID >= 2) {
                return false;
            }

            int offset = HQ_ID * 28; // 0 for HQ #0, 28 for HQ #1

            this.writeHqMapWithOffset(map, offset);

        } else if (current_round == 2) {
            // Second round
            // Read in all the data on the buffer (but don't read your own)
            // Except if there's at most 2 HQs, then just finish

            int hq_count = rc.readSharedArray(63);

            if (hq_count == 1) {
                this.clearBuffer(0, 64);
                return true;
            }

            this.AT_MOST_TWO_HQ = rc.readSharedArray(63) == 2;

            if (HQ_ID != 0) {
                this.updateHqMapWithOffset(map, 0);
            } else if (HQ_ID != 1) {
                this.updateHqMapWithOffset(map, 28);
            }

            if (this.AT_MOST_TWO_HQ) {
                if (HQ_ID == 0) {
                    // Clear the HQ #1's data
                    this.clearBuffer(28, 64);
                    // Above also clears last bit to indicate empty
                    // Doesn't change logic since AT_MOST_TWO_HQ is boolean <= 2
                } else {
                    // Clear the HQ #2's data
                    this.clearBuffer(0, 28);
                }

                return true;
            }
        } else if (current_round == 3) {
            // Third round
            // Same as first round, but now it's HQs 2 and 3
            // This should only run if there's more than 2 HQs
            if (HQ_ID < 2) {
                return false;
            }

            int offset = (HQ_ID - 2) * 28; // 0 for HQ #2, 28 for HQ #3

            this.writeHqMapWithOffset(map, offset);
        } else if (current_round == 4) {
            // Fourth round
            // Read in all the data on the buffer (but don't read your own)

            if (HQ_ID != 2) {
                this.updateHqMapWithOffset(map, 0);
            } else if (HQ_ID != 3) {
                this.updateHqMapWithOffset(map, 28);
            }
        } else if (current_round == 5) {
            // Fifth round
            // Clear buffers!

            if (HQ_ID == 2) {
                this.clearBuffer(0, 28);
            } else if (HQ_ID == 3) {
                this.clearBuffer(28, 64);
            }

            return true;
        }

        return false;
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

        MapLocation hq_location = new MapLocation(decoded_x, decoded_y);

        for (int a = 0; a < 27; a++) {
            int data = rc.readSharedArray(offset + a);
            int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

            for (int b = 3; b >= 0; b--) {
                map.updateLocationTypeAtBufferIndex(aTimesFour + b, LocationType.fromValue(data % 16));
                data /= 16;
            }
        }
    }

    private void clearBuffer(int start, int end) {

    }
}