package pqual2.data;

import battlecode.common.ResourceType;

public enum LocationType {
    NONE(0),
    ENEMY_HQ(1),
    WELL_ADAM(2),
    WELL_ELIX(3),
    WELL_MANA(4),
    WALL(5),
    ISLAND(6),
    CLOUD(7),
    CURRENT_N(8),
    CURRENT_NE(9),
    CURRENT_E(10),
    CURRENT_SE(11),
    CURRENT_S(12),
    CURRENT_SW(13),
    CURRENT_W(14),
    CURRENT_NW(15);

    private final int value;

    LocationType(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    // Mapping from a well ResourceType to the corresponding LocationType
    public static LocationType fromWellResource(ResourceType resourceType) {
        if (resourceType == ResourceType.ADAMANTIUM) {
            return WELL_ADAM;
        } else if (resourceType == ResourceType.ELIXIR) {
            return WELL_ELIX;
        } else if (resourceType == ResourceType.MANA) {
            return WELL_MANA;
        } else {
            // ResourceType.NO_RESOURCE;
            return null;
        }
    }

    // Mapping from an int to the corresponding LocationType
    public static LocationType fromValue(int value) {
        switch (value) {
            case 0:
                return LocationType.NONE;
            case 1:
                return LocationType.ENEMY_HQ;
            case 2:
                return LocationType.WELL_ADAM;
            case 3:
                return LocationType.WELL_ELIX;
            case 4:
                return LocationType.WELL_MANA;
            case 5:
                return LocationType.WALL;
            case 6:
                return LocationType.ISLAND;
            case 7:
                return LocationType.CLOUD;
            case 8:
                return LocationType.CURRENT_N;
            case 9:
                return LocationType.CURRENT_NE;
            case 10:
                return LocationType.CURRENT_E;
            case 11:
                return LocationType.CURRENT_SE;
            case 12:
                return LocationType.CURRENT_S;
            case 13:
                return LocationType.CURRENT_SW;
            case 14:
                return LocationType.CURRENT_W;
            case 15:
                return LocationType.CURRENT_NW;
            default:
                return null;
        }
    }
}
