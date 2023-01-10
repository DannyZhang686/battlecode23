package sprint1;

import battlecode.common.*;
import sprint1.data.LocationType;
import sprint1.utils.BufLocation;

public class Headquarters extends Robot {

    // Headquarters ID and location
    private final int HQ_ID;
    private final MapLocation HQ_LOC;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        // Take a unique HQ number from the 63rd array element,
        //   and increment that element so the next HQ will have
        //   a different HQ number
        this.HQ_ID = rc.readSharedArray(63);
        rc.writeSharedArray(63, this.HQ_ID + 1);

        // Set location
        this.HQ_LOC = rc.getLocation();

        System.out.println("I AM AT " + this.HQ_LOC.x + ", " + this.HQ_LOC.y); // DEBUG

        // Types of the locations surrounding the HQ
        LocationType[] ltypes = new LocationType[108];

        for (int i = 0; i < 108; i++) {
            ltypes[i] = LocationType.NONE;
        }

        // Array of internal indices of nearby islands
        int[] island_indices = rc.senseNearbyIslands();
        for (int island_index : island_indices) {
            // Find the actual tiles composing each islands
            MapLocation[] island_tiles = rc.senseNearbyIslandLocations(this.HQ_LOC, -1, island_index); // -1 seaches at max radius
            for (MapLocation island_tile : island_tiles) {
                System.out.println("island location: x: " + island_tile.x + ", y: " + island_tile.y); // DEBUG
                ltypes[BufLocation.mapLocationToBufIndex(island_tile.translate(-this.HQ_LOC.x, -this.HQ_LOC.y))] = LocationType.ISLAND;
            }
        }

        // Array of information about nearby wells
        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            System.out.println("well location: x: " + well.getMapLocation().x + ", y: " + well.getMapLocation().y); // DEBUG

            // Set this well's buffer index to this well's LocationType
            int wellLocationBufIndex = BufLocation.mapLocationToBufIndex(well.getMapLocation().translate(-this.HQ_LOC.x, -this.HQ_LOC.y));
            ltypes[wellLocationBufIndex] = LocationType.fromWellResource(well.getResourceType());
        }

        // On the first turn, there's only enough space in the buffer for the first two HQs
        // So we only consider HQs 0 and 1 on this turn
        if (HQ_ID < 2) {
            int offset = HQ_ID * 28; // 0 for HQ #0, 28 for HQ #1

            // Write encoded location to the zeroth array slot
            rc.writeSharedArray(offset, this.HQ_LOC.x + this.HQ_LOC.y * 100); // TODO (possibly): add 10000*isFoggyAtHQ?
            offset++;
            
            for (int a = 0; a < 27; a++) { // 27 = 108 / 4 = (number of viewable locations)/(locations per integer)
                int data = 0;
                int aTimesFour = a * 4; // Saves a few bytecodes (recomputation avoided)

                for (int b = 0; b < 4; b++) { // Pack in each of the four locations that can be stored in data
                    data = data << 4;
                    // This is the next locationType that needs to be encoded
                    data += ltypes[aTimesFour + b].getValue();

                    // DEBUG
                    int buf_index = a * 4 + b;
                    if (ltypes[buf_index].getValue() != 0) {
                        System.out.println("found ltype at " + buf_index + ": " + ltypes[buf_index].getValue());
                    }
                }

                // Write these four locations' encoding to the array and move onto the next
                rc.writeSharedArray(offset, data);
                offset++;
            }
        }

        // DEBUG
        System.out.println("HQ ID [" + this.HQ_ID + "] array (format: '{index}: {value}')");
        for (int i = 0; i < 64; i++) {
            if (rc.readSharedArray(i) != 0) {
                System.out.println(i + ": " + rc.readSharedArray(i));
            }
        }

        // test decoding
        LocationType[] ltypes2 = new LocationType[108];

        for (int i = 0; i < 108; i++) {
            ltypes2[i] = LocationType.NONE;
        }

        if (HQ_ID < 2) {
            int offset = HQ_ID * 28;
            
            int loc_pos = rc.readSharedArray(offset++);

            int decodedLocX = loc_pos % 100;
            int decodedLocY = loc_pos / 100;

            assert decodedLocX == this.HQ_LOC.x;
            assert decodedLocY == this.HQ_LOC.y;

            for (int a = 0; a < 108 / 4; a++) {
                int data = rc.readSharedArray(offset + a);

                for (int b = 3; b >= 0; b--) {
                    int ltype_data = data % 16;
                    data /= 16;

                    LocationType ltd = LocationType.fromValue(ltype_data);

                    int buf_index = a * 4 + b;

                    ltypes2[buf_index] = ltd;
                }
            }
            System.out.println("HQ ID [" + this.HQ_ID + "] mistakes:");
            boolean existsMistakes = false;
            for (int i = 0; i < 108; i++) {
                if (ltypes2[i] != ltypes[i]) {
                    // Mismatch between encoded and decoded data
                    System.out.println(
                            i + ": doesn't match; 1 is " + ltypes[i].getValue() + ", 2 is " + ltypes2[i].getValue());
                    existsMistakes = true;
                }
            }
            if (existsMistakes) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "Encoding error detected in HQ with id " + this.HQ_ID);
            else System.out.println("No mistakes found! :)");
        }
    }

    @Override
    public void run() throws GameActionException {

    }
}
