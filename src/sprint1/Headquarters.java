package sprint1;

import battlecode.common.*;
import sprint1.data.LocationType;
import sprint1.utils.BufLocation;

public class Headquarters extends Robot {

    private int HQ_ID;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        this.HQ_ID = rc.readSharedArray(63);
        rc.writeSharedArray(63, this.HQ_ID + 1);

        MapLocation loc = rc.getLocation();

        System.out.println("I AM AT " + loc.x + ", " + loc.y);

        LocationType[] ltypes = new LocationType[108];

        for (int i = 0; i < 108; i++) {
            ltypes[i] = LocationType.NONE;
        }

        int[] island_indexes = rc.senseNearbyIslands();

        for (int island_index : island_indexes) {
            MapLocation[] islands = rc.senseNearbyIslandLocations(loc, -1, island_index); // -1 seaches at max radius
            for (MapLocation island : islands) {
                System.out.println("island location: x: " + island.x + ", y: " + island.y);
                ltypes[BufLocation.mapLocationToBufIndex(island.translate(-loc.x, -loc.y))] = LocationType.ISLAND;
            }
        }

        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            System.out.println("well location: x: " + well.getMapLocation().x + ", y: " + well.getMapLocation().y);
            ltypes[BufLocation.mapLocationToBufIndex(well.getMapLocation().translate(-loc.x, -loc.y))] = LocationType
                    .fromWellResource(well.getResourceType());
        }

        if (HQ_ID < 2) {
            // on the first turn, there's only enough space in the buffer for the first two
            // HQs
            int offset = HQ_ID == 0 ? 0 : 28;

            rc.writeSharedArray(offset++, loc.x + loc.y * 100);

            for (int a = 0; a < 108 / 4; a++) {
                int data = 0;

                for (int b = 0; b < 4; b++) {
                    data = data << 4;

                    int buf_index = a * 4 + b;
                    if (ltypes[buf_index].getValue() != 0) {
                        System.out.println("found ltype at " + buf_index + ": " + ltypes[buf_index].getValue());
                        data += ltypes[buf_index].getValue();
                    }
                }

                rc.writeSharedArray(offset++, data);
            }
        }

        System.out.println("HQ ID [" + this.HQ_ID + "]");
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
            int offset = HQ_ID == 0 ? 0 : 28;

            int loc_pos = rc.readSharedArray(offset++);

            int loc_x = loc_pos % 100;
            int loc_y = loc_pos / 100;

            assert loc_x == loc.x;
            assert loc_y == loc.y;

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
        }

        System.out.println("HQ ID [" + this.HQ_ID + "]");
        for (int i = 0; i < 108; i++) {
            if (ltypes2[i] != ltypes[i]) {
                System.out.println(
                        i + ": doesn't match; 1 is " + ltypes[i].getValue() + ", 2 is " + ltypes2[i].getValue());
            }
        }

    }

    @Override
    public void run() throws GameActionException {

    }
}
