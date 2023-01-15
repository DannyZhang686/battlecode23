package sprint1.irc;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;
import battlecode.common.MapLocation;
import sprint1.data.LocationType;
import sprint1.utils.Tuple;

public enum IrcEvent {
    // Skip 0 since 0 indicates no data :)
    BROADCAST_LOCATION_TYPE(1);

    public static final int IRC_EVENT_BITS = 2;

    private final int value;

    IrcEvent(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    // Mapping from an int to the corresponding IrcEvent
    public static IrcEvent fromValue(int value) throws GameActionException {
        switch (value) {
            case 1:
                return IrcEvent.BROADCAST_LOCATION_TYPE;
            default:
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                        "IrcEvent.fromValue failed for value " + value);
        }
    }

    public static Tuple<MapLocation, LocationType> parseBroadcastLocationType(int data) {
        LocationType loc_type = LocationType.fromValue(data % 16);
        data /= 16;

        int loc_x = data % 64;
        data /= 64;

        int loc_y = data % 64;

        return new Tuple<>(new MapLocation(loc_x, loc_y), loc_type);
    }

    public static int serializeBroadcastLocationType(MapLocation loc, LocationType type) {
        int data = loc.y;

        data *= 64;
        data += loc.x;

        data *= 16;
        data += type.getValue();

        return data;
    }
};