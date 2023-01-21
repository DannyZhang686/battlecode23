package sprint1.data;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;

public enum HQLauncherOrder {
    // ESCORT_CARRIERS: follow carriers around, prioritizing
    // carriers holding anchors.
    // This is the default order when no other orders are given.
    // HOLD_LOCATION: holds a location specified by a pair
    // of coordinates provided along with this order. The
    // launcher will constantly aim to move toward the
    // given coordinates.
    // MASS_ASSAULT_LOCATION: same effect as HOLD_LOCATION,
    // but differentiated by the fact that one particular
    // communication from the HQ triggers its activation
    // PATROL_PATH_TO_LOCATION: patrols back and forth between
    // the HQ identified by the launcher as its "home" and
    // the given coordinates.
    // STOP_MOVING: an order the launcher can give itself if
    // it senses favourable conditions (ex. enemy HQ 1 distance
    // away, or standing on an island tile owned by the friendly
    // team). Can still be overriden by a mass assault order.
    ESCORT_CARRIERS(0),
    HOLD_LOCATION(1),
    MASS_ASSAULT_LOCATION(2),
    PATROL_PATH_TO_LOCATION(3),
    STOP_MOVING(4);

    private final int value;

    HQLauncherOrder(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public static HQLauncherOrder fromValue(int value) throws GameActionException {
        switch (value) {
            case 0:
                return HQLauncherOrder.ESCORT_CARRIERS;
            case 1:
                return HQLauncherOrder.HOLD_LOCATION;
            case 2:
                return HQLauncherOrder.MASS_ASSAULT_LOCATION;
            case 3:
                return HQLauncherOrder.PATROL_PATH_TO_LOCATION;
            case 4:
                return HQLauncherOrder.STOP_MOVING;
            default:
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                        "Invalid launcher fromValue call");
        }
    }
}
