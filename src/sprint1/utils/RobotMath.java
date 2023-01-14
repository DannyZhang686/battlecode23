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

    // public static Triple<MapLocation, Direction, Direction>
    // moveTowardsTarget(RobotController rc, MapLocation rc_loc,
    // MapLocation[] targets) throws GameActionException {

    // assert targets.length != 0;

    // HashSet<MapLocation> target_set = new HashSet<>(2 * targets.length);

    // for (MapLocation target_loc : targets) {
    // target_set.add(target_loc);
    // }

    // MapLocation closest_target = null;
    // int closest_target_dist = 0;

    // for (int i = 0; i < Constants.BFSDeltas35.length; i++) {
    // MapLocation loc = rc_loc.translate(Constants.BFSDeltas35[i][0],
    // Constants.BFSDeltas35[i][1]);

    // // TODO: optimize order for bytecode
    // if ((i == 0 || (!rc.canSenseLocation(loc)
    // || (rc.sensePassability(loc) && !rc.canSenseRobotAtLocation(loc))))
    // && target_set.contains(loc)) {
    // closest_target = loc;
    // closest_target_dist = loc.distanceSquaredTo(rc_loc);

    // break;
    // }
    // }

    // if (closest_target == null) {
    // assert true == false;
    // closest_target = targets[0];
    // }

    // // Zero moves
    // if (closest_target.equals(rc_loc)) {
    // return new Triple<>(closest_target, Direction.CENTER, Direction.CENTER);
    // }

    // // One move
    // if (closest_target_dist <= 2) {
    // if (rc.sensePassability(closest_target) &&
    // !rc.canSenseRobotAtLocation(closest_target)) {
    // return new Triple<>(closest_target, rc_loc.directionTo(closest_target),
    // Direction.CENTER);
    // } else {
    // return null;
    // }
    // }

    // // Two moves
    // if (closest_target_dist <= 8) {
    // // If already within 8 distance

    // Triple<MapLocation, Direction, Direction> triple = moveTowardsTarget8Path(rc,
    // rc_loc, closest_target,
    // closest_target);

    // if (triple != null) {
    // return triple;
    // }

    // throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
    // "impossible (but somehow possible?)");
    // }

    // // i = 25 -> skip all where closest_target_dist <= 8
    // for (int i = 25; i < Constants.BFSDeltas35.length; i++) {
    // MapLocation loc = closest_target.translate(Constants.BFSDeltas35[i][0],
    // Constants.BFSDeltas35[i][1]);

    // if (loc.distanceSquaredTo(rc_loc) <= 8) {
    // throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
    // "wha?");
    // }

    // if (rc.sensePassability(loc)
    // && !rc.canSenseRobotAtLocation(loc)) {
    // Triple<MapLocation, Direction, Direction> triple = moveTowardsTarget8Path(rc,
    // rc_loc, loc,
    // closest_target);

    // if (triple != null) {
    // return triple;
    // }
    // }
    // }

    // return null;
    // }

    // private static Triple<MapLocation, Direction, Direction>
    // moveTowardsTarget8Path(RobotController rc,
    // MapLocation rc_loc, MapLocation step_loc, MapLocation target) throws
    // GameActionException {

    // Direction step1_dir = rc_loc.directionTo(step_loc);
    // MapLocation step1_loc = rc_loc.add(step1_dir);

    // if (!rc.sensePassability(step1_loc) || rc.canSenseRobotAtLocation(step1_loc))
    // {
    // Direction step1_dir_alt = getNextDirection(step1_dir);
    // MapLocation step1_loc_alt = rc_loc.add(step1_dir_alt);

    // if (rc.sensePassability(step1_loc_alt) &&
    // !rc.canSenseRobotAtLocation(step1_loc_alt)) {
    // step1_dir = step1_dir_alt;
    // step1_loc = step1_loc_alt;
    // } else {
    // step1_dir_alt = getPreviousDirection(step1_dir);
    // step1_loc_alt = rc_loc.add(step1_dir_alt);

    // if (rc.sensePassability(step1_loc_alt) &&
    // !rc.canSenseRobotAtLocation(step1_loc_alt)) {
    // step1_dir = step1_dir_alt;
    // step1_loc = step1_loc_alt;
    // } else {
    // // imposible bug
    // return null;
    // }
    // }
    // }

    // return new Triple<MapLocation, Direction, Direction>(target, step1_dir,
    // step1_loc.directionTo(step1_loc));
    // }

    // BFS from rc_loc, stop at first found MapLocation
    // Map<MapLocation, Direction> visited (backtrack and also visited)

    final static int visited[][] = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
    final static Direction backtrack[][] = new Direction[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
    static int last = Integer.MIN_VALUE;

    public static Triple<MapLocation, Direction, Direction> moveTowardsTarget(RobotController rc, MapLocation rc_loc,
            MapLocation[] targets)
            throws GameActionException {

        assert targets.length != 0;

        HashSet<MapLocation> target_set = new HashSet<>(2 * targets.length);

        for (MapLocation target_loc : targets) {
            target_set.add(target_loc);
        }

        int map_width = rc.getMapWidth();
        int map_height = rc.getMapHeight();

        LinkedList<MapLocation> queue = new LinkedList<>();

        queue.add(rc_loc);
        visited[rc_loc.x][rc_loc.y] = ++last;
        backtrack[rc_loc.x][rc_loc.y] = Direction.CENTER;

        int iterations = 0;

        while (queue.size > 0 && iterations < 128) {
            iterations++;

            MapLocation cur = queue.dequeue().val;

            // System.out.println("BFS: cur: " + cur);

            if (target_set.contains(cur)) {
                if (cur.equals(rc_loc)) {
                    return new Triple<>(cur, Direction.CENTER, Direction.CENTER);
                }

                // backtrack and return
                MapLocation curb = cur;
                Direction prd = Direction.CENTER;
                Direction rd = backtrack[cur.x][cur.y];

                int backtrack_iterations = 1;

                while (true) {
                    curb = curb.subtract(rd);
                    Direction nrd = backtrack[curb.x][curb.y];
                    if (nrd == Direction.CENTER) {
                        System.out.println("Backtrack terminates! with " + rd);
                        return new Triple<>(cur, rd, prd);
                    }
                    prd = rd;
                    rd = nrd;

                    if (backtrack_iterations > 32) {
                        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                                "someone broke bfs");
                    }

                    backtrack_iterations++;
                }
                // unreachable
            }

            for (Direction d : Constants.ALL_DIRECTIONS) {
                MapLocation next = cur.add(d);
                int next_x = next.x;
                int next_y = next.y;
                // do we need to check bounds of next?
                if (next_x >= 0 && next_y >= 0 && next_x < map_width && next_y < map_height
                        && visited[next_x][next_y] != last
                        && (!rc.canSenseLocation(next) || rc.sensePassability(next))) {
                    visited[next_x][next_y] = last;
                    backtrack[next_x][next_y] = d;
                    queue.add(next);
                }
            }
        }

        if (iterations >= 128) {
            System.out.println("returning arbitrary location because iterations: " + iterations);
            Direction ret = rc_loc.directionTo(targets[0]);
            return new Triple<>(targets[0], ret, rc_loc.add(ret).directionTo(targets[0]));
        }

        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "no path or out of range");
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
}