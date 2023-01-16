package sprint1.utils;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Direction;

public class RobotMath {
    private static final int MAX_DISTANCE = GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_WIDTH;

    public static boolean isNextTo(MapLocation a, MapLocation b) {
        int xDiff = Math.abs(a.x - b.x);
        int yDiff = Math.abs(a.y - b.y);

        return (xDiff == 0 && yDiff == 1) || (yDiff == 0 && xDiff == 1);
    }

    // https://github.com/mvpatel2000/Battlecode2020/blob/master/src/qual/Robot.java#L94
    // will approach dest on the diagonal
    // this helps avoid clumping because when too many robots bunch on the diagonal,
    // they will move N/S/E/W to "unclump"
    public Direction toward(MapLocation me, MapLocation dest) throws GameActionException {
        switch (Integer.compare(me.x, dest.x) + 3 * Integer.compare(me.y, dest.y)) {
            case -4:
                return Direction.NORTHEAST;
            case -3:
                return Direction.NORTH;
            case -2:
                return Direction.NORTHWEST;
            case -1:
                return Direction.EAST;
            case 0:
                return Direction.CENTER;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.SOUTHEAST;
            case 3:
                return Direction.SOUTH;
            case 4:
                return Direction.SOUTHWEST;
            default:
                throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "wha?");
        }
    }

    // want to place something at target
    // pass in unit_location to reduce bytecode
    public static MapLocation closestActionablePlacement(RobotController rc, MapLocation unit_location,
            MapLocation target, int action_radius) throws GameActionException {
        MapLocation[] candidates = rc.getAllLocationsWithinRadiusSquared(unit_location, action_radius); // 100 bytecode
        // or use the sorted precompute thing instead

        MapLocation ret = null;

        int best_distance = MAX_DISTANCE;

        for (MapLocation candidate : candidates) {
            int dist = target.distanceSquaredTo(candidate); // costs 2 bytecode

            if (dist < best_distance
                    && rc.sensePassability(candidate) // costs 5 bytecode
                    && !rc.isLocationOccupied(candidate) // 5 bytecode
            ) {
                // get the closest candidate where we can place
                best_distance = dist;
                ret = candidate;
            }
        }
        return ret;
    }

    private static final Direction[] ORDERED_DIRECTIONS = { Direction.WEST, Direction.NORTHWEST, Direction.NORTH,
            Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH,
            Direction.SOUTHWEST };

    // Note for getXDirection: getDirectionOrderNum() returns orderNum + 1 (since
    // CENTER is included)

    public static Direction getNextDirection(Direction dir) {
        return ORDERED_DIRECTIONS[(dir.getDirectionOrderNum() - 2 + ORDERED_DIRECTIONS.length)
                % ORDERED_DIRECTIONS.length];
    }

    public static Direction getPreviousDirection(Direction dir) {
        return ORDERED_DIRECTIONS[dir.getDirectionOrderNum() % ORDERED_DIRECTIONS.length];
    }

    public static Direction offsetDirection(Direction dir, int x) {
        return ORDERED_DIRECTIONS[(dir.getDirectionOrderNum() - 1 + x + ORDERED_DIRECTIONS.length)
                % ORDERED_DIRECTIONS.length];
    }

    // This is the correct distance function to use when we can travel
    // diagonally just as fast as horizontally or vertically
    public static int getChessboardDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
}