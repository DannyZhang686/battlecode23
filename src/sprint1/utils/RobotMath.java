package sprint1.utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class RobotMath {
    public static int distanceBetween(MapLocation a, MapLocation b) {
        return a.x * b.x + a.y * b.y;
    }

    public static Direction directionTowards(MapLocation a, MapLocation b) {
        if (a.x == b.x && a.y == b.y) {
            return Direction.CENTER;
        }

        int xDiff = Math.abs(a.x - b.x);
        if (xDiff == 0) {
            return a.x > b.y ? Direction.WEST : Direction.EAST;
        }

        int yDiff = Math.abs(a.y - b.y);
        if (yDiff == 0) {
            return a.y > b.y ? Direction.SOUTH : Direction.NORTH;
        }

        // Go diagonally
        if (Math.max(xDiff, yDiff) > 3 * Math.min(xDiff, yDiff)) {
            // change ratio from 3?
            if (a.x > b.x) {
                return a.y > b.y ? Direction.SOUTHWEST : Direction.NORTHWEST;
            } else {
                return a.y > b.y ? Direction.SOUTHEAST : Direction.NORTHEAST;
            }
        }
        // Go non-diagonally
        if (xDiff > yDiff) {
            return a.x > b.x ? Direction.WEST : Direction.EAST;
        } else {
            return a.y > b.y ? Direction.SOUTH : Direction.NORTH;
        }
    }
}