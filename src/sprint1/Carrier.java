package sprint1;

import battlecode.common.*;
import sprint1.utils.RobotMath;

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

        if (this.rc.canTransferResource(this.HQ_LOC, ResourceType.ADAMANTIUM, amount_adam)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.ADAMANTIUM, amount_adam);
            amount_adam = 0;
        }

        if (this.rc.canTransferResource(this.HQ_LOC, ResourceType.ELIXIR, amount_elix)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.ELIXIR, amount_elix);
            amount_elix = 0;
        }

        if (this.rc.canTransferResource(this.HQ_LOC, ResourceType.MANA, amount_mana)) {
            this.rc.transferResource(this.HQ_LOC, ResourceType.MANA, amount_mana);
            amount_mana = 0;
        }

        if (amount_adam >= 36 || amount_elix >= 36 || amount_mana >= 36) {
            Direction dir = RobotMath.directionTowards(loc, this.HQ_LOC);

            if (this.rc.canMove(dir)) {
                this.rc.move(dir);
            }
            return;
        }

        WellInfo[] wells = this.rc.senseNearbyWells();

        if (wells.length == 0) {
            System.out.println("no wells found, sadge :(");
            return;
        }

        WellInfo closest_well = wells[0];

        for (int i = 1; i < wells.length; i++) {
            if (RobotMath.distanceBetween(loc, closest_well.getMapLocation()) > RobotMath.distanceBetween(loc,
                    wells[i].getMapLocation())) {
                closest_well = wells[i];
            }
        }

        if (!RobotMath.isNextTo(loc, closest_well.getMapLocation())) {
            Direction dir = RobotMath.directionTowards(loc, closest_well.getMapLocation());
            if (this.rc.canMove(dir)) {
                this.rc.move(dir);
            }
            return;
        }

        System.out.println("next to a well, collecting ...");

        if (!this.rc.canCollectResource(closest_well.getMapLocation(), 4)) {
            System.out.println("cannot collect?");
            return;
        }

        if (this.rc.getResourceAmount(closest_well.getResourceType()) == 36) {
            this.rc.collectResource(closest_well.getMapLocation(), 3);
            Direction dir = RobotMath.directionTowards(loc, this.HQ_LOC);
            if (this.rc.canMove(dir)) {
                this.rc.move(dir);
            }
            return;
        }

        this.rc.collectResource(closest_well.getMapLocation(), 4);
    }
}
