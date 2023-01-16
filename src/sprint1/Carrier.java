package sprint1;

import battlecode.common.*;
import sprint1.data.*;
import sprint1.utils.RobotMath;

public class Carrier extends Robot {

    Team friendlyTeam, enemyTeam;
    MapLocation curLocation;
    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    // Location of HQ closest to the spawn of this launcher
    final MapLocation hqLocation;

    HQCarrierOrder hqOrder;
    MapLocation hqTargetLocation; // A location referenced by hqOrder

    // A well to refuse to go to
    MapLocation avoidWellLocation;

    // For anchor-carrying carriers, this is the number of consecutive turns
    // for which no non-friendly islands have been spotted
    int fruitlessSearchTurns = 0;

    // When fruitlessSearchTurns is above this value, the carrier
    // will start wandering around the map (covering a larger area to
    // try and find an empty island)
    public static final int START_TOURING_MAP = 6;
    // Destinations to visit (go through randomly)
    public static final int NUM_TOURIST_DESTINATIONS = 9;
    MapLocation[] TOURIST_DESTINATIONS;
    // Destination we are going to right now
    int curDestination;
    // Number of turns that a "tourist" carrier has been stuck consecutively
    // When this exceeds MAX_TOURIST_STUCK_TURNS, a new destination is chosen
    int touristStuckTurns;
    public static final int MAX_TOURIST_STUCK_TURNS = 10;

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
        hqTargetLocation = null;
        avoidWellLocation = null;
        friendlyTeam = rc.getTeam();
        enemyTeam = (friendlyTeam == Team.A) ? Team.B : Team.A;
        curLocation = rc.getLocation();
        fruitlessSearchTurns = touristStuckTurns = 0;
        curDestination = -1;

        // Places to go for anchor-carrying carriers when
        // fruitlessSearchTurns > START_TOURING_MAP
        TOURIST_DESTINATIONS = new MapLocation[NUM_TOURIST_DESTINATIONS];
        TOURIST_DESTINATIONS[0] = new MapLocation(1, 1);
        TOURIST_DESTINATIONS[1] = new MapLocation(rc.getMapWidth() / 2, 1);
        TOURIST_DESTINATIONS[2] = new MapLocation(rc.getMapWidth() - 1, 1);
        TOURIST_DESTINATIONS[3] = new MapLocation(1, rc.getMapHeight() / 2);
        TOURIST_DESTINATIONS[4] = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        TOURIST_DESTINATIONS[5] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() / 2);
        TOURIST_DESTINATIONS[6] = new MapLocation(1, rc.getMapHeight() - 1);
        TOURIST_DESTINATIONS[7] = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() - 1);
        TOURIST_DESTINATIONS[8] = new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1);

        MapLocation closestHQLocation = null;
        int closestHQDistance = 100;

        for (RobotInfo info : this.rc.senseNearbyRobots()) {
            if (info.getType() == RobotType.HEADQUARTERS) {
                int theDist = RobotMath.getChessboardDistance(info.getLocation(), curLocation);
                if (theDist < closestHQDistance) {
                    closestHQDistance = theDist;
                    closestHQLocation = info.getLocation();
                }
            }
        }
        hqLocation = closestHQLocation;
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
                if (rng.nextInt(Math.max((100 - rc.getRoundNum()) / 20, 2)) == 0) {
                    // Chance of switching away from adamantium
                    avoidWellLocation = hqTargetLocation;
                    hqTargetLocation = null;
                }
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
                while (moveTowardsTarget(hqLocation)) {}
                // System.out.println("want to go home but stuck :(");
                return;
            }

            // Maybe we can pick up an anchor?
            if (rc.canTakeAnchor(hqLocation, Anchor.STANDARD)) {
                // Get the anchor and start looking for an island to place it on
                rc.takeAnchor(hqLocation, Anchor.STANDARD);
                hqOrder = HQCarrierOrder.CARRY_ANCHOR;
                fruitlessSearchTurns = 0;
                curDestination = -1;
            }
        }
        
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            int curIslandIndex = rc.senseIsland(curLocation);
            if (rc.canPlaceAnchor()) {
                // curIslandIndex should never be -1
                if (rc.senseTeamOccupyingIsland(curIslandIndex) != friendlyTeam) {
                    // This island is neutral
                    // Note: apparently placing another anchor on a friendly island
                    // does *not* increase anchors placed for tiebreakers, so there's
                    // no reason to do that
                    rc.placeAnchor();
                    hqOrder = HQCarrierOrder.GATHER_ANY_RESOURCE;
                    fruitlessSearchTurns = 0;
                    curDestination = -1;
                }
            }
            else if (curIslandIndex != -1) {
                if (rc.senseTeamOccupyingIsland(curIslandIndex) == enemyTeam) {
                    // Sit here until the island becomes neutral (then drop anchor)
                    fruitlessSearchTurns = 0;
                    curDestination = -1;
                    return;
                }
            }
        }

        WellInfo closest_well = findClosestWell();
        // Refuse to go to the avoided well
        if ((closest_well != null) && (closest_well.getMapLocation() == avoidWellLocation)) {
            closest_well = null;
        }
        // Anchor-carrying carriers only actively seek out neutral islands
        // (but as above, will place anchors on friendly islands some of
        //  the time if they stumble across friendly islands
        MapLocation closestNeutralIslandLoc = null;

        // Only initialize closestNeutralIslandLoc if HQ order is CARRY_ANCHOR
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            int[] nearbyIslandIndices = rc.senseNearbyIslands();
            int curDist = 100;
            
            for (int islandIndex : nearbyIslandIndices) {
                if (rc.senseTeamOccupyingIsland(islandIndex) == Team.NEUTRAL) {
                    // Open island!
                    for (MapLocation theLocation : rc.senseNearbyIslandLocations(islandIndex)) {
                        int theDist = RobotMath.getChessboardDistance(theLocation, curLocation);
                        if (theDist < curDist) {
                            curDist = theDist;
                            closestNeutralIslandLoc = theLocation;
                        }
                    }
                }
            }
            if (closestNeutralIslandLoc == null) {
                fruitlessSearchTurns++;
            }
            else {
                fruitlessSearchTurns = 0;
                curDestination = -1;
            }
            // If we can't find anything, start visiting spots around the map
            if ((fruitlessSearchTurns > START_TOURING_MAP) && (curDestination == -1)) {
                curDestination = rng.nextInt(NUM_TOURIST_DESTINATIONS);
            }
        }

        if (((hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) &&
             (closest_well == null) &&
             (hqTargetLocation == null)) ||
            ((hqOrder == HQCarrierOrder.CARRY_ANCHOR) &&
             (closestNeutralIslandLoc == null) &&
             (curDestination == -1))) {
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
            if (curDestination == -1) {
                targetLocation = closestNeutralIslandLoc;
            }
            else {
                targetLocation = TOURIST_DESTINATIONS[curDestination];
            }
        }
        else { // hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE
            if (hqTargetLocation != null) {
                targetLocation = hqTargetLocation;
            }
            else {
                targetLocation = closest_well.getMapLocation();
            }
        }
        
        rc.setIndicatorString(String.valueOf(curDestination) + " " + String.valueOf(targetLocation));

        // Only stop moving if we are adjacent to the target
        // and we are not carrying an anchor (since anchor-carrying
        // carriers need to be right on top of the target)
        // Note: if we're just visiting around the map, it's okay to just be
        // adjacent to the given destination
        if ((!curLocation.isAdjacentTo(targetLocation)) ||
            ((hqOrder == HQCarrierOrder.CARRY_ANCHOR) && (curDestination == -1))) {
            if (!moveTowardsTarget(targetLocation)) {
                // Reset curDestination if stuck for too long
                if (curDestination != -1) {
                    touristStuckTurns++;
                    if (touristStuckTurns > MAX_TOURIST_STUCK_TURNS) {
                        curDestination = rng.nextInt(NUM_TOURIST_DESTINATIONS);
                        touristStuckTurns = 0;
                    }
                }
                // System.out.println("stuck sadge :(");
                return;
            } else {
                touristStuckTurns = 0;
                curLocation = rc.getLocation();
                if (!curLocation.isAdjacentTo(targetLocation) && !moveTowardsTarget(targetLocation)) {
                    // System.out.println("half stuck sadge :(");
                    return;
                }
            }
        }

        // Collect resources if possible
        if (hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) {
            if (rc.canCollectResource(targetLocation, GameConstants.WELL_STANDARD_RATE)) {
                // Set this well as target
                hqTargetLocation = targetLocation;
                rc.collectResource(targetLocation, GameConstants.WELL_STANDARD_RATE);
            }
        }
        else if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            // Reset curDestination if we've arrived
            if (curLocation.isAdjacentTo(targetLocation)) {
                if (curDestination != -1) {
                    curDestination = rng.nextInt(NUM_TOURIST_DESTINATIONS);
                }
            }
        }
    }
}
