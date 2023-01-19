package rsprint2.data;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;

public enum HQCarrierOrder {
    // GATHER_ANY_RESOURCE: find any resource wells and gather resources.
    // This is the default order when no other orders are given.
    // CARRY_ANCHOR: picks up carrier from spawning HQ, brings it
    // to a target island, and places it. Reverts to GATHER_ANY_RESOURCE
    // after placement.
    // GATHER_ADAMANTIUM: gathers only adamantium.
    // GATHER_MANA: gathers only mana.
    // GATHER_AT_WELL: gathers resources from a specific well.
    // EXPLORE: Explore in the given direction, regularly reporting
    // back to headquarters.
    GATHER_ANY_RESOURCE(0),
    CARRY_ANCHOR(1),
    GATHER_ADAMANTIUM(2),
    GATHER_MANA(3),
    GATHER_AT_WELL(4),
    EXPLORE(5);

    private final int value;

    HQCarrierOrder(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public static HQCarrierOrder fromValue(int value) throws GameActionException {
        switch (value) {
            case 0:
                return HQCarrierOrder.GATHER_ANY_RESOURCE;
            case 1:
                return HQCarrierOrder.CARRY_ANCHOR;
            case 2:
                return HQCarrierOrder.GATHER_ADAMANTIUM;
            case 3:
                return HQCarrierOrder.GATHER_MANA;
            case 4:
                return HQCarrierOrder.GATHER_AT_WELL;
            case 5:
                return HQCarrierOrder.EXPLORE;
            default:
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "Invalid carrier fromValue call");
        }
    }
}
