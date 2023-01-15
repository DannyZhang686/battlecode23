package sprint1;

import battlecode.common.*;
import sprint1.data.*;

public class Carrier extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;

    HQCarrierOrder hqOrder;
    MapLocation hqTargetLocation; // A location referenced by hqOrder

    // When carrier health is above this value, the carrier will not shoot
    public static final int SHOOT_THRESHOLD = 6;

    // If relaxedPathfinding is true, being <= RELAXED_PATHFINDING_DISTANCE
    // units from the target is considered equivalent to being at the target
    // Note that relaxedPathfinding is only true when gathering at a specific
    // well (i.e. being close enough to collect resources)
    public static final int RELAXED_PATHFINDING_DISTANCE = 2;
    
    // Below copied from launcher logic:
    // For random movement to go in the same direction for multiple turns
    Direction randomDirection = Direction.CENTER;
    int turnsRemaining = 0;
    // Number of turns for which to stick with the same direction
    public static final int MAX_TURNS_REMAINING = 5;


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
        runSetup();

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
            while (moveTowardsTarget(hqLocation)) {
            }
            // System.out.println("want to go home but stuck :(");
            return;
        }

        WellInfo closest_well = findClosestWell();

        if (closest_well == null) {
            // No well, go explore
            if ((randomDirection != Direction.CENTER) &&
                rc.canMove(randomDirection) &&
                turnsRemaining != 0) {
                // Try to move in the same direction
                rc.move(randomDirection);
                turnsRemaining--;
            }
            else {
                randomDirection = Direction.CENTER;

                Direction[] moveableDirections = getMoveableDirections();
                int n = moveableDirections.length;
                if (n != 0) {
                    // Move in a random valid direction
                    Direction theDirection = moveableDirections[rng.nextInt(n)];
                    rc.move(theDirection);
                    randomDirection = theDirection;
                    turnsRemaining = MAX_TURNS_REMAINING;
                }
            }
            return;
        }

        MapLocation closest_well_loc = closest_well.getMapLocation();

        if (!rc_loc.isAdjacentTo(closest_well_loc)) {
            if (!moveTowardsTarget(closest_well_loc)) {
                // System.out.println("stuck sadge :(");
                return;
            } else if (!rc_loc.isAdjacentTo(closest_well_loc) && !moveTowardsTarget(closest_well_loc)) {
                // System.out.println("half stuck sadge :(");
                return;
            }
        }

        if (!rc.canCollectResource(closest_well_loc, GameConstants.WELL_STANDARD_RATE)) {
            // System.out.println("cannot collect?");
            return;
        }

        rc.collectResource(closest_well_loc, GameConstants.WELL_STANDARD_RATE);
    }
}
