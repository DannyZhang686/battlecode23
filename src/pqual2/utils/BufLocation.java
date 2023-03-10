package pqual2.utils;

import battlecode.common.MapLocation;

public class BufLocation {

    private static final int[] bufIndexToMapX = {
            -3, -2, -1, 0, 1, 2, 3,
            -4, -3, -2, -1, 0, 1, 2, 3, 4,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5,
            -4, -3, -2, -1, 0, 1, 2, 3, 4,
            -3, -2, -1, 0, 1, 2, 3,
    };

    private static final int[] bufIndexToMapY = {
            5, 5, 5, 5, 5, 5, 5,
            4, 4, 4, 4, 4, 4, 4, 4, 4,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -4, -4, -4, -4, -4, -4, -4, -4, -4,
            -5, -5, -5, -5, -5, -5, -5
    };

    private static final int[][] coordsToIndex = {
            { -1, -1, 101, 102, 103, 104, 105, 106, 107, -1, -1 },
            { -1, 92, 93, 94, 95, 96, 97, 98, 99, 100, -1 },
            { 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91 },
            { 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80 },
            { 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 },
            { 49, 50, 51, 52, 53, -1, 54, 55, 56, 57, 58 },
            { 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48 },
            { 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37 },
            { 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 },
            { -1, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1 },
            { -1, -1, 0, 1, 2, 3, 4, 5, 6, -1, -1 }
    };

    public static MapLocation bufIndexToMaplocation(int index) {
        assert index >= 0 && index < 128;
        
        return new MapLocation(bufIndexToMapX[index], bufIndexToMapY[index]);
    }

    // Maps a relative location (with respect to HQ) to the buffer index
    public static int mapLocationToBufIndex(MapLocation loc) {
        // Add 5 to each coordinate to ensure indices are non-negative
        return coordsToIndex[loc.y + 5][loc.x + 5];
    }
}
