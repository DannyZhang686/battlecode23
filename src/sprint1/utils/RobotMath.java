package sprint1.utils;

import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import sprint1.Constants;
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
            int dist = unit_location.distanceSquaredTo(candidate); // costs 2 bytecode

            if (dist < best_distance
                    && rc.sensePassability(candidate) // costs 5 bytecode
            ) {
                // get the closest candidate where we can place
                best_distance = dist;
                ret = candidate;
            }
        }
        return ret;
    }

    // BFS from rc_loc, stop at first found MapLocation
    // Map<MapLocation, Direction> visited (backtrack and also visited)
    public static Tuple<MapLocation, Direction> moveTowardsTarget(RobotController rc, MapLocation rc_loc,
            MapLocation[] targets)
            throws GameActionException {

        assert targets.length != 0;

        HashMap<MapLocation, Direction> visited = new HashMap<>(64);
        LinkedList<MapLocation> queue = new LinkedList<>();

        queue.add(rc_loc);
        visited.insert(rc_loc, Direction.CENTER);

        int iterations = 0;

        while (queue.size > 0 && iterations < 500) {
            iterations++;

            MapLocation cur = queue.dequeue().val;

            System.out.println("BFS: cur: " + cur);

            for (MapLocation target_loc : targets) {
                if (target_loc.equals(cur)) {
                    if (target_loc.equals(rc_loc)) {
                        return new Tuple<>(target_loc, Direction.CENTER);
                    }

                    // backtrack and return
                    Direction rd = visited.get(cur);

                    int backtrack_iterations = 1;

                    while (true) {
                        cur = cur.subtract(rd);
                        Direction nrd = visited.get(cur);
                        if (nrd == Direction.CENTER) {
                            System.out.println("Backtrack terminates! with " + rd);
                            return new Tuple<>(target_loc, rd);
                        }
                        rd = nrd;

                        if (backtrack_iterations > 100) {
                            throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                                    "someone broke bfs");
                        }

                        backtrack_iterations++;
                    }
                    // unreachable
                }
            }

            for (Direction d : Constants.ALL_DIRECTIONS) {
                MapLocation next = cur.add(d);

                if (!visited.contains(next) && (!rc.canSenseLocation(rc_loc) || rc.sensePassability(rc_loc))) {
                    visited.insert(next, d);
                    queue.add(next);
                }
            }
        }

        if (iterations >= 500) {
            System.out.println("returning arbitrary location because iterations: " + iterations);
            return new Tuple<>(targets[0], rc_loc.directionTo(targets[0]));
        }

        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                "no path or out of range");
    }
}