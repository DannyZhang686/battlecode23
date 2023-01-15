package sprint1;

import battlecode.common.*;

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

        // This array describes the rays from (0, 0) to every point on the
        // boundary of the visible area of a 34-square-unit radius centered
        // at (0, 0). This array is not unique, but it's an okay "best
        // approximation" of linear rays. There are 40 paths, each with
        // length 5 except for those with absolute coordinates within 4
        // which have length 4.
        // So this is an int[40][5][2] (not a rectangular array though).
        // The entries are in clockwise order from the negative x-axis.
        public static int[][][] rays = new int[][][] {
                        { { -1, 0 }, { -2, 0 }, { -3, 0 }, { -4, 0 }, { -5, 0 } }, // Negative x-axis
                        { { -1, 0 }, { -2, 0 }, { -3, 0 }, { -4, 1 }, { -5, 1 } },
                        { { -1, 0 }, { -2, 1 }, { -3, 1 }, { -4, 1 }, { -5, 2 } },
                        { { -1, 1 }, { -2, 1 }, { -3, 1 }, { -4, 2 }, { -5, 3 } },
                        { { -1, 1 }, { -2, 2 }, { -3, 2 }, { -4, 3 } },
                        { { -1, 1 }, { -2, 2 }, { -3, 3 }, { -4, 4 } },
                        { { -1, 1 }, { -2, 2 }, { -2, 3 }, { -3, 4 } },
                        { { -1, 1 }, { -1, 2 }, { -1, 3 }, { -2, 4 }, { -5, 3 } },
                        { { 0, 1 }, { -1, 2 }, { -1, 3 }, { -1, 4 }, { -2, 5 } },
                        { { 0, 1 }, { 0, 2 }, { 0, 3 }, { -1, 4 }, { -1, 5 } },
                        { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 } }, // Positive y-axis
                        { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 4 }, { 1, 5 } },
                        { { 0, 1 }, { 1, 2 }, { 1, 3 }, { 1, 4 }, { 2, 5 } },
                        { { 1, 1 }, { 1, 2 }, { 1, 3 }, { 2, 4 }, { 5, 3 } },
                        { { 1, 1 }, { 2, 2 }, { 2, 3 }, { 3, 4 } },
                        { { 1, 1 }, { 2, 2 }, { 3, 3 }, { 4, 4 } },
                        { { 1, 1 }, { 2, 2 }, { 3, 2 }, { 4, 3 } },
                        { { 1, 1 }, { 2, 1 }, { 3, 1 }, { 4, 2 }, { 5, 3 } },
                        { { 1, 0 }, { 2, 1 }, { 3, 1 }, { 4, 1 }, { 5, 2 } },
                        { { 1, 0 }, { 2, 0 }, { 3, 0 }, { 4, 1 }, { 5, 1 } },
                        { { 1, 0 }, { 2, 0 }, { 3, 0 }, { 4, 0 }, { 5, 0 } }, // Positive x-axis
                        { { 1, 0 }, { 2, 0 }, { 3, 0 }, { 4, -1 }, { 5, -1 } },
                        { { 1, 0 }, { 2, -1 }, { 3, -1 }, { 4, -1 }, { 5, -2 } },
                        { { 1, -1 }, { 2, -1 }, { 3, -1 }, { 4, -2 }, { 5, -3 } },
                        { { 1, -1 }, { 2, -2 }, { 3, -2 }, { 4, -3 } },
                        { { 1, -1 }, { 2, -2 }, { 3, -3 }, { 4, -4 } },
                        { { 1, -1 }, { 2, -2 }, { 2, -3 }, { 3, -4 } },
                        { { 1, -1 }, { 1, -2 }, { 1, -3 }, { 2, -4 }, { 5, -3 } },
                        { { 0, -1 }, { 1, -2 }, { 1, -3 }, { 1, -4 }, { 2, -5 } },
                        { { 0, -1 }, { 0, -2 }, { 0, -3 }, { 1, -4 }, { 1, -5 } },
                        { { 0, -1 }, { 0, -2 }, { 0, -3 }, { 0, -4 }, { 0, -5 } }, // Negative y-axis
                        { { 0, -1 }, { 0, -2 }, { 0, -3 }, { -1, -4 }, { -1, -5 } },
                        { { 0, -1 }, { -1, -2 }, { -1, -3 }, { -1, -4 }, { -2, -5 } },
                        { { -1, -1 }, { -1, -2 }, { -1, -3 }, { -2, -4 }, { -5, -3 } },
                        { { -1, -1 }, { -2, -2 }, { -2, -3 }, { -3, -4 } },
                        { { -1, -1 }, { -2, -2 }, { -3, -3 }, { -4, -4 } },
                        { { -1, -1 }, { -2, -2 }, { -3, -2 }, { -4, -3 } },
                        { { -1, -1 }, { -2, -1 }, { -3, -1 }, { -4, -2 }, { -5, -3 } },
                        { { -1, 0 }, { -2, -1 }, { -3, -1 }, { -4, -1 }, { -5, -2 } },
                        { { -1, 0 }, { -2, 0 }, { -3, 0 }, { -4, -1 }, { -5, -1 } },
        };

        // Points at which each ray ends for a robot with 34 vision
        // Just the last entries of each rays[i]
        public static int[][] rayEndpoints34 = new int[][] {
                        { -5, 0 }, // Negative x-axis
                        { -5, 1 },
                        { -5, 2 },
                        { -5, 3 },
                        { -4, 3 },
                        { -4, 4 },
                        { -3, 4 },
                        { -5, 3 },
                        { -2, 5 },
                        { -1, 5 },
                        { 0, 5 }, // Positive y-axis
                        { 1, 5 },
                        { 2, 5 },
                        { 5, 3 },
                        { 3, 4 },
                        { 4, 4 },
                        { 4, 3 },
                        { 5, 3 },
                        { 5, 2 },
                        { 5, 1 },
                        { 5, 0 }, // Positive x-axis
                        { 5, -1 },
                        { 5, -2 },
                        { 5, -3 },
                        { 4, -3 },
                        { 4, -4 },
                        { 3, -4 },
                        { 5, -3 },
                        { 2, -5 },
                        { 1, -5 },
                        { 0, -5 }, // Negative y-axis
                        { -1, -5 },
                        { -2, -5 },
                        { -5, -3 },
                        { -3, -4 },
                        { -4, -4 },
                        { -4, -3 },
                        { -5, -3 },
                        { -5, -2 },
                        { -5, -1 },
        };
}
