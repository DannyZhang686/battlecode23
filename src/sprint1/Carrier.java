package sprint1;

import battlecode.common.*;
import sprint1.utils.*;

public class Carrier extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;

    // Note that if isLeader is false, the launcher is first and
    // foremost a follower, and only considers the hqOrder if no
    // leaders are found nearby
    // HQLauncherOrder hqOrder;
    // MapLocation hqTargetLocation; // A location referenced by hqOrder
    // final boolean isLeader;
    // int followingCarrierID; // Specifically for the ESCORT_CARRIERS order

    // // When the number of *offensive* enemies (launchers/destabilizers)
    // // in range is not above this value, launchers target other launchers
    // // over boosters
    // // If the number of *offensive* enemies is above this value, launchers
    // // target boosters over other launchers
    // public static final int TARGET_LAUNCHERS_FIRST_ENEMY_THRESHOLD = 5;

    // // 1/LAUNCHER_LEADER_PROPORTION of all launchers are leaders
    // public static final int LAUNCHER_LEADER_PROPORTION = 2;

    // // If relaxedPathfinding is true, being <= RELAXED_PATHFINDING_DISTANCE
    // // units from the target is considered equivalent to being at the target
    // // Note that relaxedPathfinding is only true when either following a leader
    // // (i.e. don't crowd the leader) or patrolling a path (i.e. 5 units is
    // // close enough to the target)
    // public static final int RELAXED_PATHFINDING_DISTANCE = 5;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);

        RobotInfo[] nearby_robots = this.rc.senseNearbyRobots();

        if (nearby_robots.length == 0) {
            throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                    "could not find nearby robots, where is hq?");
        }

        RobotInfo hq_robot = null;

        for (RobotInfo info : nearby_robots) {
            // TODO: Find closest, not just one HQ!
            if (info.type == RobotType.HEADQUARTERS) {
                hq_robot = info;
                break;
            }
        }

        if (hq_robot == null) {
            throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR,
                    "could not find hq in nearby robots :(");
        }

        this.hqLocation = hq_robot.location;
    }

    @Override
    public void run() throws GameActionException {
        if (rc.getRoundNum() > 10) {
            return;
        }

        MapLocation loc = this.rc.getLocation();

        int amount_adam = this.rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int amount_elix = this.rc.getResourceAmount(ResourceType.ELIXIR);
        int amount_mana = this.rc.getResourceAmount(ResourceType.MANA);

        if (amount_adam > 36 && this.rc.canTransferResource(this.hqLocation, ResourceType.ADAMANTIUM, amount_adam)) {
            this.rc.transferResource(this.hqLocation, ResourceType.ADAMANTIUM, amount_adam);
            amount_adam = 0;
        }

        if (amount_elix > 36 && this.rc.canTransferResource(this.hqLocation, ResourceType.ELIXIR, amount_elix)) {
            this.rc.transferResource(this.hqLocation, ResourceType.ELIXIR, amount_elix);
            amount_elix = 0;
        }

        if (amount_mana > 36 && this.rc.canTransferResource(this.hqLocation, ResourceType.MANA, amount_mana)) {
            this.rc.transferResource(this.hqLocation, ResourceType.MANA, amount_mana);
            amount_mana = 0;
        }

        if (amount_adam > 36 || amount_elix > 36 || amount_mana > 36) {
            // It's time to go home
            // Triple<MapLocation, Direction, Direction> ret =
            // RobotMath.moveTowardsTarget(this.rc, this.rc.getLocation(),
            // new MapLocation[] { this.hqLocation });
            // if (this.rc.canMove(ret.second)) {
            // this.rc.move(ret.second);
            // if (this.rc.canMove(ret.third)) {
            // this.rc.move(ret.third);
            // }
            // }
            Direction dir = loc.directionTo(this.hqLocation);
            // Integer offsets[] = {0, 1, -1, 2, -2, 3, -3, 4};
            for (int i = 0; i < 8; i++) {
                if (this.rc.canMove(dir)) {
                    this.rc.move(dir);
                    return;
                }
                dir = RobotMath.getNextDirection(dir);
            }
            System.out.println("want to go home but stuck :(");
            return;
        }

        WellInfo[] wells = this.rc.senseNearbyWells();

        if (wells.length == 0) {
            System.out.println("no wells found, sadge :(");
            // let's explore (and not block others)
            return;
        }

        MapLocation[] well_locations = new MapLocation[wells.length];

        for (int i = 0; i < wells.length; i++) {
            well_locations[i] = wells[i].getMapLocation();
        }

        Triple<MapLocation, Direction, Direction> closest_well = RobotMath.moveTowardsTarget(this.rc,
                this.rc.getLocation(),
                well_locations);

        if (closest_well == null) {
            System.out.print("idk which well to go to, sadge :(");
            return;
        }

        System.out.println("going to well: x: " + closest_well.first.x + ", y: " + closest_well.first.y + " - dir1: "
                + closest_well.second.toString() + " - dir2: " + closest_well.third.toString());

        if (!loc.isAdjacentTo(closest_well.first)) {
            if (this.rc.canMove(closest_well.second)) {
                this.rc.move(closest_well.second);

                if (this.rc.canMove(closest_well.third)) {
                    this.rc.move(closest_well.third);
                } else {
                    System.out.println("half stuck sadge :(");
                }
            } else {
                System.out.println("stuck sadge :(");
                // do something
            }

            return;
        }

        System.out.println("next to a well, collecting ...");

        WellInfo closest_well_info = this.rc.senseWell(closest_well.first);

        assert closest_well_info != null;

        if (this.rc.getResourceAmount(closest_well_info.getResourceType()) == 36) {
            if (!this.rc.canCollectResource(closest_well.first, GameConstants.WELL_STANDARD_RATE)) {
                System.out.println("cannot collect?");
                return;
            }

            this.rc.collectResource(closest_well.first, GameConstants.WELL_STANDARD_RATE);

            // This is logic to go back home? This is also done earlier in the function
            Direction dir = loc.directionTo(this.hqLocation);
            if (this.rc.canMove(dir)) {
                this.rc.move(dir);
            }
            return;
        }

        if (!this.rc.canCollectResource(closest_well.first, GameConstants.WELL_STANDARD_RATE)) {
            System.out.println("cannot collect?");
            return;
        }

        this.rc.collectResource(closest_well.first, GameConstants.WELL_STANDARD_RATE);
    }
}
