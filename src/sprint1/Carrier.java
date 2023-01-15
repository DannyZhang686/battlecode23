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

        hqOrder = HQCarrierOrder.GATHER_ANY_RESOURCE;
        friendlyTeam = rc.getTeam();
        enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;

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
        curLocation = rc.getLocation();

        if (hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) {
            int amount_adam = this.rc.getResourceAmount(ResourceType.ADAMANTIUM);
            int amount_elix = this.rc.getResourceAmount(ResourceType.ELIXIR);
            int amount_mana = this.rc.getResourceAmount(ResourceType.MANA);

            if (amount_adam > 38 && this.rc.canTransferResource(this.hqLocation, ResourceType.ADAMANTIUM, amount_adam)) {
                this.rc.transferResource(this.hqLocation, ResourceType.ADAMANTIUM, amount_adam);
                amount_adam = 0;
            }

            if (amount_elix > 38 && this.rc.canTransferResource(this.hqLocation, ResourceType.ELIXIR, amount_elix)) {
                this.rc.transferResource(this.hqLocation, ResourceType.ELIXIR, amount_elix);
                amount_elix = 0;
            }

            if (amount_mana > 38 && this.rc.canTransferResource(this.hqLocation, ResourceType.MANA, amount_mana)) {
                this.rc.transferResource(this.hqLocation, ResourceType.MANA, amount_mana);
                amount_mana = 0;
            }

            if (amount_adam > 38 || amount_elix > 38 || amount_mana > 38) {
                // It's time to go home
                while (moveTowardsTarget(hqLocation)) {
                }
                // System.out.println("want to go home but stuck :(");
                return;
            }

            // Maybe we can pick up an anchor?
            if (rc.canTakeAnchor(hqLocation, Anchor.STANDARD)) {
                // Get the anchor and start looking for an island to place it on
                rc.takeAnchor(hqLocation, Anchor.STANDARD);
                hqOrder = HQCarrierOrder.CARRY_ANCHOR;
            }
        }
        
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            if (rc.canPlaceAnchor()) {
                int curIslandIndex = rc.senseIsland(curLocation); // Should never be -1
                if ((rc.senseTeamOccupyingIsland(curIslandIndex) != friendlyTeam) ||
                    (rng.nextInt(10) == 0)) {
                    // Either this island is neutral, or we decided that we should
                    // re-place an anchor on a friendly island anyway (tiebreaks)
                    rc.placeAnchor();
                    hqOrder = HQCarrierOrder.GATHER_ANY_RESOURCE;
                }
            }
        }

        WellInfo closest_well = findClosestWell();
        // Anchor-carrying carriers only actively seek out neutral islands
        // (but as above, will place anchors on friendly islands some of
        //  the time if they stumble across friendly islands
        MapLocation closestNeutralIslandLoc = null;

        // Only initialize closestNeutralIslandLoc if the carrier
        // order is CARRY_ANCHOR
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            int[] nearbyIslandIndices = rc.senseNearbyIslands();
            int curDist = 100;
            
            for (int islandIndex : nearbyIslandIndices) {
                if (rc.senseTeamOccupyingIsland(islandIndex) == Team.NEUTRAL) {
                    // Open island!
                    for (MapLocation theLocation : rc.senseNearbyIslandLocations(islandIndex)) {
                        // This is the correct distance function to use when we can travel
                        // diagonally just as fast as horizontally or vertically
                        int lInfinityNorm = Math.max(Math.abs(theLocation.x - curLocation.x),
                                                     Math.abs(theLocation.y - curLocation.y));
                        if (lInfinityNorm < curDist) {
                            curDist = lInfinityNorm;
                            closestNeutralIslandLoc = theLocation;
                        }
                    }
                }
            }
        }

        if (((hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) && (closest_well == null)) ||
            ((hqOrder == HQCarrierOrder.CARRY_ANCHOR) && (closestNeutralIslandLoc == null))) {
            // No target, go explore
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

        MapLocation targetLocation;
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            targetLocation = closestNeutralIslandLoc;
        }
        else { // hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE
            targetLocation = closest_well.getMapLocation();
        }

        // Only stop moving if we are adjacent to the target
        // and we are not carrying an anchor (since anchor-carrying
        // carriers need to be right on top of the target)
        if (!rc_loc.isAdjacentTo(targetLocation) || (hqOrder == HQCarrierOrder.CARRY_ANCHOR)) {
            if (!moveTowardsTarget(targetLocation)) {
                // System.out.println("stuck sadge :(");
                return;
            } else if (!rc_loc.isAdjacentTo(targetLocation) && !moveTowardsTarget(targetLocation)) {
                // System.out.println("half stuck sadge :(");
                return;
            }
        }

        // Collect resources if possible
        if (hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) {
            if (rc.canCollectResource(targetLocation, GameConstants.WELL_STANDARD_RATE)) {
                rc.collectResource(targetLocation, GameConstants.WELL_STANDARD_RATE);
            }
        }
    }
}
