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

    public static Triple<MapLocation, Direction, Direction> moveTowardsTarget(RobotController rc, MapLocation rc_loc,
            MapLocation[] targets) throws GameActionException {

        assert targets.length != 0;

        HashSet<MapLocation> target_set = new HashSet<>(2 * targets.length);

        for (MapLocation target_loc : targets) {
            target_set.add(target_loc);
        }

        MapLocation closest_target = null;
        boolean closest_target_path_8_long = false;

        for (int i = 0; i < Constants.BFSDeltas35.length; i++) {
            MapLocation loc = rc_loc.translate(Constants.BFSDeltas35[i][0], Constants.BFSDeltas35[i][1]);

            // TODO: optimize order for bytecode
            if ((i == 0 || (!rc.canSenseLocation(loc)
                    || (rc.sensePassability(loc) && !rc.canSenseRobotAtLocation(loc))))
                    && target_set.contains(loc)) {
                closest_target = loc;

                if (i < 25) {
                    closest_target_path_8_long = true;
                }

                break;
            }
        }

        if (closest_target == null) {
            closest_target = targets[0];
        }

        if (closest_target.equals(rc_loc)) {
            return new Triple<>(closest_target, Direction.CENTER, Direction.CENTER);
        }

        if (closest_target_path_8_long) {
            // If already within 8 distance

            Triple<MapLocation, Direction, Direction> triple = moveTowardsTarget8Path(rc, rc_loc, closest_target,
                    closest_target);

            if (triple != null) {
                return triple;
            }

            throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "impossible (but somehow possible?)");
        }

        for (int i = 0; i < Constants.BFSDeltas35.length; i++) {
            MapLocation loc = closest_target.translate(Constants.BFSDeltas35[i][0], Constants.BFSDeltas35[i][1]);

            if (loc.distanceSquaredTo(rc_loc) <= 4) {
                // MAYBE: Check if we're actually getting closer (flip flop case i.e AX. -> .XA)
                if (rc.sensePassability(loc)
                        && !rc.canSenseRobotAtLocation(loc)) {
                    return new Triple<>(loc, rc_loc.directionTo(loc), Direction.CENTER);
                }
            } else if (loc.distanceSquaredTo(rc_loc) <= 8 && rc.sensePassability(loc)
                    && !rc.canSenseRobotAtLocation(loc)) {
                Triple<MapLocation, Direction, Direction> triple = moveTowardsTarget8Path(rc, rc_loc, loc,
                        closest_target);

                if (triple != null) {
                    return triple;
                }
            }
        }

        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "no path or out of range");
    }

    private static Triple<MapLocation, Direction, Direction> moveTowardsTarget8Path(RobotController rc,
            MapLocation rc_loc, MapLocation step_loc, MapLocation target) throws GameActionException {

        Direction step1_dir = rc_loc.directionTo(step_loc);
        MapLocation step1_loc = rc_loc.add(step1_dir);

        if (!rc.sensePassability(step1_loc) || rc.canSenseRobotAtLocation(step1_loc)) {
            Direction step1_dir_alt = getNextDirection(step1_dir);
            MapLocation step1_loc_alt = rc_loc.add(step1_dir_alt);

            if (rc.sensePassability(step1_loc_alt) && !rc.canSenseRobotAtLocation(step1_loc_alt)) {
                step1_dir = step1_dir_alt;
                step1_loc = step1_loc_alt;
            } else {
                step1_dir_alt = getPreviousDirection(step1_dir);
                step1_loc_alt = rc_loc.add(step1_dir_alt);

                if (rc.sensePassability(step1_loc_alt) && !rc.canSenseRobotAtLocation(step1_loc_alt)) {
                    step1_dir = step1_dir_alt;
                    step1_loc = step1_loc_alt;
                } else {
                    // imposible bug
                    return null;
                }
            }
        }

        return new Triple<MapLocation, Direction, Direction>(target, step1_dir,
                step1_loc.directionTo(step1_loc));
    }

    // BFS from rc_loc, stop at first found MapLocation
    // Map<MapLocation, Direction> visited (backtrack and also visited)
    public static Tuple<MapLocation, Direction> moveTowardsTargetSlow(RobotController rc, MapLocation rc_loc,
            MapLocation[] targets)
            throws GameActionException {

        assert targets.length != 0;

        HashSet<MapLocation> target_set = new HashSet<>(2 * targets.length);

        for (MapLocation target_loc : targets) {
            target_set.add(target_loc);
        }

        HashMap<MapLocation, Direction> visited = new HashMap<>(128);
        LinkedList<MapLocation> queue = new LinkedList<>();

        queue.add(rc_loc);
        visited.insert(rc_loc, Direction.CENTER);

        int iterations = 0;

        while (queue.size > 0 && iterations < 500) {
            iterations++;

            MapLocation cur = queue.dequeue().val;

            // System.out.println("BFS: cur: " + cur);

            if (target_set.contains(cur)) {
                if (cur.equals(rc_loc)) {
                    return new Tuple<>(cur, Direction.CENTER);
                }

                // backtrack and return
                Direction rd = visited.get(cur);

                int backtrack_iterations = 1;

                while (true) {
                    cur = cur.subtract(rd);
                    Direction nrd = visited.get(cur);
                    if (nrd == Direction.CENTER) {
                        System.out.println("Backtrack terminates! with " + rd);
                        return new Tuple<>(cur, rd);
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

            for (Direction d : Constants.ALL_DIRECTIONS) {
                MapLocation next = cur.add(d);

                if (!visited.contains(next) && (!rc.canSenseLocation(rc_loc) || rc.sensePassability(rc_loc))) {
                    visited.insert(next, d);
                    queue.add(next);
                }
            }
        }

        if (iterations >= 500)

        {
            System.out.println("returning arbitrary location because iterations: " + iterations);
            return new Tuple<>(targets[0], rc_loc.directionTo(targets[0]));
        }

        throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "no path or out of range");
    }

    private static final Direction[] ORDERED_DIRECTIONS = { Direction.WEST, Direction.NORTHWEST, Direction.NORTH,
            Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH,
            Direction.SOUTHWEST };

    // Note for getXDirection: getDirectionOrderNum() returns orderNum + 1 (since
    // CENTER is included)

    private static Direction getNextDirection(Direction dir) {
        return ORDERED_DIRECTIONS[Math.abs(dir.getDirectionOrderNum() - 2) % ORDERED_DIRECTIONS.length];
    }

    private static Direction getPreviousDirection(Direction dir) {
        return ORDERED_DIRECTIONS[dir.getDirectionOrderNum() % ORDERED_DIRECTIONS.length];
    }
}