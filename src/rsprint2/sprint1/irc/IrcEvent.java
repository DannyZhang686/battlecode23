package sprint1.irc;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;
import battlecode.common.MapLocation;
import sprint1.data.LocationType;
import sprint1.utils.Tuple;

public enum IrcEvent {
    // Skip 0 since 0 indicates no data :)
    INIT_HQ_SYNC(1),
    BROADCAST_LOCATION_TYPE(2),
    HOLD_LOCATION(3);

    public static final int IRC_EVENT_BITS = 2;
    public static final int IRC_MAX_EVENTS_BITS = 14; // 16 - 2 for HQ in IrcWriter
    public static final int IRC_MAX_EVENTS_BITS_ASSERTER = 0b11111111111111;

    private final int value;

    IrcEvent(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public int getFragLength() throws GameActionException {
        switch (value) {
            case 1:
                return 27;
            case 2:
                return 0;
            case 3:
                return 1;
            default:
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                        "IrcEvent.getFraglength failed for value " + value);
        }
    }

    // Mapping from an int to the corresponding IrcEvent
    public static IrcEvent fromValue(int value) throws GameActionException {
        switch (value) {
            case 1:
                return IrcEvent.INIT_HQ_SYNC;
            case 2:
                return IrcEvent.BROADCAST_LOCATION_TYPE;
            case 3:
                return IrcEvent.HOLD_LOCATION;
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

        assert data <= IRC_MAX_EVENTS_BITS_ASSERTER;

        return data;
    }

    public static int[] serializeHoldLocation(MapLocation loc, int robot_id) {
        int data = loc.y * 64 + loc.x;

        assert data <= IRC_MAX_EVENTS_BITS_ASSERTER;
        assert robot_id <= IRC_MAX_EVENTS_BITS_ASSERTER;

        return new int[] { data, robot_id };
    }
};