package sprint1;

import battlecode.common.*;
import sprint1.utils.*;

public class Carrier extends Robot {

    private final MapLocation HQ_LOC;

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

        this.HQ_LOC = hq_robot.location;
    }

    @Override
    public void run() throws GameActionException {
        MapLocation loc = this.rc.getLocation();

        int amount_adam = this.rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int amount_elix = this.rc.getResourceAmount(ResourceType.ELIXIR);
        int amount_mana = this.rc.getResourceAmount(ResourceType.MANA);

        if (amount_adam > 36 && this.rc.canTransferResource(this.HQ_LOC, ResourceType.ADAMANTIUM, amount_adam)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.ADAMANTIUM, amount_adam);
            amount_adam = 0;
        }

        if (amount_elix > 36 && this.rc.canTransferResource(this.HQ_LOC, ResourceType.ELIXIR, amount_elix)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.ELIXIR, amount_elix);
            amount_elix = 0;
        }

        if (amount_mana > 36 && this.rc.canTransferResource(this.HQ_LOC, ResourceType.MANA, amount_mana)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.MANA, amount_mana);
            amount_mana = 0;
        }

        if (amount_adam > 36 || amount_elix > 36 || amount_mana > 36) {
            System.out.println("Move back home");
            // TODO: find a way home when friendly robots are blocking path

            Direction dir = loc.directionTo(this.HQ_LOC);
            for (int i = 0; i < 8; i++) {
                if (this.rc.canMove(dir)) {
                    this.rc.move(dir);
                    return;
                }
                dir = RobotMath.getNextDirection(dir);
            }
            System.out.println("want to go home but stuck :(");
            return;
            // BFS with moveTowardTargetSlow would hit bytecode limit, so we can't use that
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

        int collect_amount = Math.min(closest_well_info.getRate(), GameConstants.CARRIER_CAPACITY - this.rc.getWeight());
        // do I even need to do the CAP-getWeight calculation?

        if (this.rc.getResourceAmount(closest_well_info.getResourceType()) == 36) {
            collect_amount = Math.min(collect_amount , 3);
            if (!this.rc.canCollectResource(closest_well.first, collect_amount)) {
                System.out.println("cannot collect?");
                return;
            }

            this.rc.collectResource(closest_well.first, collect_amount);

            Direction dir = loc.directionTo(this.HQ_LOC);
            if (this.rc.canMove(dir)) {
                this.rc.move(dir);
            }
            return;
        }

        // we can only take up to 4 per term? Idk. I haven't check documentation
        collect_amount = Math.min(collect_amount , 4);
        if (!this.rc.canCollectResource(closest_well.first, collect_amount)) {
            System.out.println("cannot collect?");
            return;
        }

        this.rc.collectResource(closest_well.first, collect_amount);
    }
}
