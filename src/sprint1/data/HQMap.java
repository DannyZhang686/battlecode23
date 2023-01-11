package sprint1.data;

import sprint1.utils.HashMap;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.WellInfo;
import sprint1.utils.BufLocation;

public class HQMap {
    private final MapLocation HQ_LOC;
    private final HashMap<MapLocation, LocationType> location_types;

    public HQMap(MapLocation HQ_LOC, RobotController rc) throws GameActionException {
        this.HQ_LOC = HQ_LOC;
        this.location_types = new HashMap<MapLocation, LocationType>(1024);

        // Loop through each visible island index
        for (int island_index : rc.senseNearbyIslands()) {
            // Find the actual tiles composing each island
            // Note: the second argument, -1, indicates a search at maximum radius
            MapLocation[] indexed_island_locations = rc.senseNearbyIslandLocations(this.HQ_LOC, -1, island_index);

            for (MapLocation island_location : indexed_island_locations) {
                this.location_types.insert(island_location, LocationType.ISLAND);
            }
        }

        for (WellInfo well : rc.senseNearbyWells()) {
            MapLocation well_location = well.getMapLocation();
            this.location_types.insert(well_location,
                    LocationType.fromWellResource(well.getResourceType()));
        }
    }

    public LocationType getLocationTypeAtBufferIndex(int buf_index) {
        MapLocation offset = BufLocation.bufIndexToMaplocation(buf_index);
        return this.getLocationTypeAtMapLocation(this.HQ_LOC.translate(offset.x, offset.y));
    }

    public LocationType getLocationTypeAtMapLocation(MapLocation loc) {
        return this.location_types.getOrDefault(loc, LocationType.NONE);
    }

    public void updateLocationTypeAtBufferIndex(int buf_index, LocationType type) {
        // Save bytecode
        if (type != LocationType.NONE) {
            MapLocation offset = BufLocation.bufIndexToMaplocation(buf_index);
            this.updateLocationTypeAtMapLocation(this.HQ_LOC.translate(offset.x, offset.y), type);
        }
    }

    public void updateLocationTypeAtMapLocation(MapLocation loc, LocationType type) {
        // Save bytecode
        if (type != LocationType.NONE) {
            this.location_types.insert(loc, type);
        }
    }
}
