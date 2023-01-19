package rsprint2.data;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.WellInfo;
import rsprint2.utils.BufLocation;

public class HQMap {
    private final MapLocation HQ_LOC;
    private final MapLocation[] HQ_OTHER_LOCS;
    private int HQ_OTHER_COUNT;
    private final LocationType[][] location_types;

    public HQMap(MapLocation HQ_LOC, RobotController rc) throws GameActionException {
        this.HQ_LOC = HQ_LOC;
        this.HQ_OTHER_LOCS = new MapLocation[3];
        this.HQ_OTHER_COUNT = 0;
        this.location_types = new LocationType[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];

        // Loop through each visible island index
        for (int island_index : rc.senseNearbyIslands()) {
            // Find the actual tiles composing each island
            // Note: the second argument, -1, indicates a search at maximum radius
            MapLocation[] indexed_island_locations = rc.senseNearbyIslandLocations(this.HQ_LOC, -1, island_index);

            for (MapLocation island_location : indexed_island_locations) {
                this.location_types[island_location.x][island_location.y] = LocationType.ISLAND;
            }
        }

        for (WellInfo well : rc.senseNearbyWells()) {
            MapLocation well_location = well.getMapLocation();
            this.location_types[well_location.x][well_location.y] = LocationType
                    .fromWellResource(well.getResourceType());
        }
    }

    public LocationType getLocationTypeAtBufferIndex(int buf_index) {
        MapLocation offset = BufLocation.bufIndexToMaplocation(buf_index);
        return this.getLocationTypeAtMapLocation(this.HQ_LOC.translate(offset.x, offset.y));
    }

    public LocationType getLocationTypeAtMapLocation(MapLocation loc) {
        int locx = loc.x;
        int locy = loc.y;

        if (locx < 0 || locy < 0 || locx >= GameConstants.MAP_MAX_WIDTH || locy >= GameConstants.MAP_MAX_HEIGHT) {
            return LocationType.NONE;
        }

        LocationType type = this.location_types[locx][locy];

        if (type == null) {
            return LocationType.NONE;
        }

        return type;
    }

    public void updateLocationTypeAtBufferIndex(int buf_index, LocationType type) {
        // Save bytecode
        if (type != LocationType.NONE) {
            MapLocation offset = BufLocation.bufIndexToMaplocation(buf_index);
            this.updateLocationTypeAtMapLocation(this.HQ_LOC.translate(offset.x, offset.y), type);
        }
    }

    public void updateLocationTypeAtMapLocation(MapLocation loc, LocationType type) {
        int locx = loc.x;
        int locy = loc.y;

        if (locx < 0 || locy < 0 || locx >= GameConstants.MAP_MAX_WIDTH || locy >= GameConstants.MAP_MAX_HEIGHT) {
            return;
        }

        this.location_types[locx][locy] = type;
    }

    public void addOtherHq(MapLocation hq_loc) {
        assert HQ_OTHER_COUNT < 3;

        HQ_OTHER_LOCS[HQ_OTHER_COUNT++] = hq_loc;
    }
}
