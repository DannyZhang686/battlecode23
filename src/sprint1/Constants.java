package sprint1;

import battlecode.common.Direction;
import battlecode.common.GameConstants;

public class Constants {
        public static final int CLOSEST_DIST = GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_WIDTH;

        /** Array containing all the possible movement directions. */
        public static final Direction[] ALL_DIRECTIONS = {
                        Direction.NORTH,
                        Direction.NORTHEAST,
                        Direction.EAST,
                        Direction.SOUTHEAST,
                        Direction.SOUTH,
                        Direction.SOUTHWEST,
                        Direction.WEST,
                        Direction.NORTHWEST,
        };

        public static int[][] reverseArray(int[][] array) {
                int[][] ret = new int[array.length][array[0].length];
                for (int i = 0; i < ret.length; i++) {
                        for (int j = 0; j < array[0].length; j++)
                                ret[ret.length - 1 - i][j] = array[i][j];
                }
                return ret;
        }

        public static final int[][] BFSDeltas35 = {
                        { 0, 0 }, { -1, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, -1 },
                        { -1, 1 }, { 1, -1 }, { 1, 1 }, { -2, 0 }, { 0, -2 }, { 0, 2 }, { 2, 0 }, { -2, -1 }, { -2, 1 },
                        { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 }, { 2, -1 }, { 2, 1 }, { -2, -2 }, { -2, 2 },
                        { 2, -2 }, { 2, 2 }, { -3, 0 }, { 0, -3 }, { 0, 3 }, { 3, 0 }, { -3, -1 }, { -3, 1 },
                        { -1, -3 }, { -1, 3 }, { 1, -3 }, { 1, 3 }, { 3, -1 }, { 3, 1 }, { -3, -2 }, { -3, 2 },
                        { -2, -3 }, { -2, 3 }, { 2, -3 }, { 2, 3 }, { 3, -2 }, { 3, 2 }, { -4, 0 }, { 0, -4 }, { 0, 4 },
                        { 4, 0 }, { -4, -1 }, { -4, 1 }, { -1, -4 }, { -1, 4 }, { 1, -4 }, { 1, 4 }, { 4, -1 },
                        { 4, 1 }, { -3, -3 }, { -3, 3 }, { 3, -3 }, { 3, 3 }, { -4, -2 }, { -4, 2 }, { -2, -4 },
                        { -2, 4 }, { 2, -4 }, { 2, 4 }, { 4, -2 }, { 4, 2 }, { -5, 0 }, { -4, -3 }, { -4, 3 },
                        { -3, -4 }, { -3, 4 }, { 0, -5 }, { 0, 5 }, { 3, -4 }, { 3, 4 }, { 4, -3 }, { 4, 3 }, { 5, 0 },
                        { -5, -1 }, { -5, 1 }, { -1, -5 }, { -1, 5 }, { 1, -5 }, { 1, 5 }, { 5, -1 }, { 5, 1 },
                        { -5, -2 }, { -5, 2 }, { -2, -5 }, { -2, 5 }, { 2, -5 }, { 2, 5 }, { 5, -2 }, { 5, 2 },
                        { -4, -4 }, { -4, 4 }, { 4, -4 }, { 4, 4 }, { -5, -3 }, { -5, 3 }, { -3, -5 }, { -3, 5 },
                        { 3, -5 }, { 3, 5 }, { 5, -3 }, { 5, 3 } };

        public static final int[][] BFSDeltas35Reversed = reverseArray(BFSDeltas35);

        public static final int[][] BFSDeltas24 = {
                        { 0, 0 }, { 1, 0 }, { 0, -1 }, { -1, 0 }, { 0, 1 }, { 2, 0 },
                        { 1, -1 },
                        { 0, -2 }, { -1, -1 }, { -2, 0 }, { -1, 1 }, { 0, 2 }, { 1, 1 }, { 3, 0 }, { 2, -1 }, { 1, -2 },
                        { 0, -3 },
                        { -1, -2 }, { -2, -1 }, { -3, 0 }, { -2, 1 }, { -1, 2 }, { 0, 3 }, { 1, 2 }, { 2, 1 }, { 4, 0 },
                        { 3, -1 },
                        { 2, -2 }, { 1, -3 }, { 0, -4 }, { -1, -3 }, { -2, -2 }, { -3, -1 }, { -4, 0 }, { -3, 1 },
                        { -2, 2 },
                        { -1, 3 }, { 0, 4 }, { 1, 3 }, { 2, 2 }, { 3, 1 }, { 4, -1 }, { 3, -2 }, { 2, -3 }, { 1, -4 },
                        { -1, -4 },
                        { -2, -3 }, { -3, -2 }, { -4, -1 }, { -4, 1 }, { -3, 2 }, { -2, 3 }, { -1, 4 }, { 1, 4 },
                        { 2, 3 },
                        { 3, 2 }, { 4, 1 }, { 4, -2 }, { 3, -3 }, { 2, -4 }, { -2, -4 }, { -3, -3 }, { -4, -2 },
                        { -4, 2 },
                        { -3, 3 }, { -2, 4 }, { 2, 4 }, { 3, 3 }, { 4, 2 } };
}
