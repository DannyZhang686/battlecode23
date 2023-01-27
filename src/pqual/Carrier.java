package pqual;

import battlecode.common.*;
import pqual.data.*;
import pqual.utils.RobotMath;

public class Carrier extends Robot {

    MapLocation curMovementTarget;
    boolean relaxedPathfinding;

    // Location of HQ closest to the spawn of this launcher
    MapLocation hqLocation;

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

    // When carrier health is below this value, the carrier will run from enemies
    public static final int SCARED_THRESHOLD = 10;

    // If relaxedPathfinding is true, being <= RELAXED_PATHFINDING_DISTANCE
    // units from the target is considered equivalent to being at the target
    // Note that relaxedPathfinding is only true when gathering at a specific
    // well (i.e. being close enough to collect resources)
    public static final int RELAXED_PATHFINDING_DISTANCE = 2;

    // Below copied from launcher logic:
    // For random movement to go in the same direction for multiple turns
    Direction randomDirection = Direction.CENTER;
    int exploreTurnsRemaining = 0;
    // Number of turns for which to stick with the same direction
    public static final int MAX_EXPLORE_TURNS_REMAINING = 5;

    Direction fleeDirection = Direction.CENTER;
    int fleeTurnsRemaining = 0;
    public static final int MAX_FLEE_REMAINING = 4;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);

        hqOrder = HQCarrierOrder.GATHER_ANY_RESOURCE;
        if ((rc.getID() ^ (rc.getID() >> 4)) % 2 == 0) {
            hqOrder = HQCarrierOrder.GATHER_MANA;
        }
        hqTargetLocation = null;
        avoidWellLocation = null;
        rc_loc = rc.getLocation();
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

        hqLocation = null;
    }

    private void updateLocations() throws GameActionException {
        int hqLocationDistance = hqLocation == null ? 1000 : RobotMath.getChessboardDistance(hqLocation, rc_loc);

        for (RobotInfo info : this.rc.senseNearbyRobots()) {
            if (info.getType() == RobotType.HEADQUARTERS) {
                int dist = RobotMath.getChessboardDistance(info.getLocation(), rc_loc);

                if (dist < hqLocationDistance) {
                    hqLocationDistance = dist;
                    hqLocation = info.getLocation();
                }
            }
        }
    }

    private void transferResourcesAndTakeAnchor() throws GameActionException {
        if (hqOrder != HQCarrierOrder.CARRY_ANCHOR && hqLocation != null) {
            int amount_adam = rc.getResourceAmount(ResourceType.ADAMANTIUM);
            int amount_elix = rc.getResourceAmount(ResourceType.ELIXIR);
            int amount_mana = rc.getResourceAmount(ResourceType.MANA);

            if (amount_adam > 38
                    && rc.canTransferResource(hqLocation, ResourceType.ADAMANTIUM, amount_adam)) {
                rc.transferResource(hqLocation, ResourceType.ADAMANTIUM, amount_adam);
                if (rng.nextInt(Math.max((100 - rc.getRoundNum()) / 10, 4)) == 0) {
                    // Chance of switching away from adamantium
                    avoidWellLocation = hqTargetLocation;
                    hqTargetLocation = null;
                }
                amount_adam = 0;
            }

            if (amount_elix > 38 && rc.canTransferResource(hqLocation, ResourceType.ELIXIR, amount_elix)) {
                rc.transferResource(hqLocation, ResourceType.ELIXIR, amount_elix);
                amount_elix = 0;
            }

            if (amount_mana > 38 && rc.canTransferResource(hqLocation, ResourceType.MANA, amount_mana)) {
                rc.transferResource(hqLocation, ResourceType.MANA, amount_mana);
                amount_mana = 0;
            }

            if (amount_adam > 38 || amount_elix > 38 || amount_mana > 38) {
                // It's time to go home
                while (moveTowardsTarget(hqLocation)) {
                }
                // System.out.println("want to go home but stuck :(");
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
            int curIslandIndex = rc.senseIsland(rc_loc);
            if (rc.canPlaceAnchor()) {
                // curIslandIndex should never be -1
                if (curIslandIndex == -1) {
                    throw new GameActionException(GameActionExceptionType.INTERNAL_ERROR, "get away from me");
                }

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
            } else if (curIslandIndex != -1) {
                if (rc.senseTeamOccupyingIsland(curIslandIndex) == enemyTeam) {
                    // Sit here until the island becomes neutral (then drop anchor)
                    fruitlessSearchTurns = 0;
                    curDestination = -1;
                    return;
                }
            }
        }
    }

    @Override
    public void run() throws GameActionException {
        rc.setIndicatorString(String.valueOf(hqOrder));

        runSetup();

        updateLocations();

        transferResourcesAndTakeAnchor();

        avoidThreats();

        runSetup();

        WellInfo closest_well = null;
        if (hqOrder == HQCarrierOrder.GATHER_ANY_RESOURCE) {
            closest_well = findClosestWell();
        } else if (hqOrder == HQCarrierOrder.GATHER_MANA) {
            closest_well = findClosestResourceWell(ResourceType.MANA);
        }
        // Refuse to go to avoided well
        if ((closest_well != null) && (closest_well.getMapLocation() == avoidWellLocation)) {
            closest_well = null;
        }

        // Anchor-carrying carriers only actively seek out neutral islands
        MapLocation closestNeutralIslandLoc = null;

        // Only initialize closestNeutralIslandLoc if HQ order is CARRY_ANCHOR
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            int[] nearbyIslandIndices = rc.senseNearbyIslands();
            int curDist = 100;

            for (int islandIndex : nearbyIslandIndices) {
                if (rc.senseTeamOccupyingIsland(islandIndex) == Team.NEUTRAL) {
                    // Open island!
                    for (MapLocation theLocation : rc.senseNearbyIslandLocations(islandIndex)) {
                        int theDist = RobotMath.getChessboardDistance(theLocation, rc_loc);
                        if (theDist < curDist) {
                            curDist = theDist;
                            closestNeutralIslandLoc = theLocation;
                        }
                    }
                }
            }
            if (closestNeutralIslandLoc == null) {
                fruitlessSearchTurns++;
            } else {
                fruitlessSearchTurns = 0;
                curDestination = -1;
            }
            // If we can't find anything, start visiting spots around the map
            if ((fruitlessSearchTurns > START_TOURING_MAP) && (curDestination == -1)) {
                curDestination = rng.nextInt(NUM_TOURIST_DESTINATIONS);
            }
        }

        if (((hqOrder != HQCarrierOrder.CARRY_ANCHOR) &&
                (closest_well == null) &&
                (hqTargetLocation == null)) ||
                ((hqOrder == HQCarrierOrder.CARRY_ANCHOR) &&
                        (closestNeutralIslandLoc == null) &&
                        (curDestination == -1))) {
            // No target, go explore
            if ((randomDirection != Direction.CENTER) &&
                    rc.canMove(randomDirection) &&
                    exploreTurnsRemaining != 0) {
                // Try to move in the same direction
                rc.move(randomDirection);
                super_recalibrate();
                exploreTurnsRemaining--;
            } else {
                randomDirection = Direction.CENTER;

                Direction[] moveableDirections = getMoveableDirections();
                int n = moveableDirections.length;
                if (n != 0) {
                    // Move in a random valid direction
                    Direction theDirection = moveableDirections[rng.nextInt(n)];
                    rc.move(theDirection);
                    super_recalibrate();
                    randomDirection = theDirection;
                    exploreTurnsRemaining = MAX_EXPLORE_TURNS_REMAINING;
                }
            }
            return;
        }

        MapLocation targetLocation;
        if (hqOrder == HQCarrierOrder.CARRY_ANCHOR) {
            if (curDestination == -1) {
                targetLocation = closestNeutralIslandLoc;
            } else {
                targetLocation = TOURIST_DESTINATIONS[curDestination];
            }
        } else {
            if (hqTargetLocation != null) {
                targetLocation = hqTargetLocation;
            } else {
                targetLocation = closest_well.getMapLocation();
            }
        }

        // Only stop moving if we are adjacent to the target
        // and we are not carrying an anchor (since anchor-carrying
        // carriers need to be right on top of the target)
        // Note: if we're just visiting around the map, it's okay to just be
        // adjacent to the given destination
        if ((!rc_loc.isAdjacentTo(targetLocation)) ||
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
                if (!rc_loc.isAdjacentTo(targetLocation) && !moveTowardsTarget(targetLocation)) {
                    // System.out.println("half stuck sadge :(");
                    return;
                }
            }
        }

        if (hqOrder != HQCarrierOrder.CARRY_ANCHOR &&
                rc_loc.isAdjacentTo(targetLocation) && hqLocation != null) {
            Direction heur_dir = hqLocation.directionTo(targetLocation).rotateRight().rotateRight().rotateRight();

            int total = rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA)
                    + rc.getResourceAmount(ResourceType.ELIXIR);

            if (total < 12) {
                heur_dir = heur_dir.opposite();
            } else if (total < 25) {
                heur_dir = Direction.CENTER;
            }

            MapLocation heur_loc = targetLocation.add(heur_dir);

            float r = rng.nextFloat();

            if (r < 0.33) {
                heur_loc = heur_loc.add(hqLocation.directionTo(targetLocation).opposite());
            } else if (r < 0.66) {
                heur_loc = heur_loc.add(hqLocation.directionTo(targetLocation));
            }

            Direction dir = rc_loc.directionTo(heur_loc);

            if (dir != Direction.CENTER && rc_loc.add(dir).isAdjacentTo(targetLocation) && rc.canMove(dir)) {
                rc.move(dir);
                super_recalibrate();
            }
        }

        // Collect resources if possible
        if (hqOrder != HQCarrierOrder.CARRY_ANCHOR) {
            while (rc.canCollectResource(targetLocation, GameConstants.WELL_STANDARD_RATE)) {
                // Set this well as the target
                hqTargetLocation = targetLocation;
                rc.collectResource(targetLocation, GameConstants.WELL_STANDARD_RATE);
            }
        } else {
            // Reset curDestination if we've arrived
            if (rc_loc.isAdjacentTo(targetLocation)) {
                if (curDestination != -1) {
                    curDestination = rng.nextInt(NUM_TOURIST_DESTINATIONS);
                }
            }
        }
    }

    private void avoidThreats() throws GameActionException {
        // identify threats
        RobotInfo closestThreat = closestThreat();

        if (closestThreat != null) {
            rc.setIndicatorString("Flee! Closest threat location:" + String.valueOf(closestThreat.getLocation()));
            Direction dir = rc_loc.directionTo(closestThreat.getLocation());
            fleeDirection = dir.opposite();
            fleeTurnsRemaining = MAX_FLEE_REMAINING;

            // If they're close enough to be in range, we should always attack
            if (rc.canAttack(closestThreat.getLocation())) {
                rc.attack(closestThreat.getLocation());
            }
        }

        // If we're about to die, drop payload and run
        if (rc.getHealth() <= 30 && rc.getWeight() > 0 && fleeTurnsRemaining > 0) {
            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
            closestThreat = null;
            int closestThreatDist = 100000;
            for (RobotInfo robot : enemyRobots) {
                RobotType robotType = robot.getType();
                if (robotType != RobotType.HEADQUARTERS) {
                    int threatDistance = rc_loc.distanceSquaredTo(robot.getLocation());
                    if (threatDistance < closestThreatDist) {
                        closestThreatDist = threatDistance;
                        closestThreat = robot;
                    }
                }
            }

            if ((closestThreat != null) && (rc.canAttack(closestThreat.getLocation()))) {
                rc.attack(closestThreat.getLocation());
            } else {
                // try surrounding 8 squares to dump, should be space
                for (Direction d : Constants.ALL_DIRECTIONS) {
                    MapLocation mp = rc_loc.add(d);
                    if (rc.canSenseLocation(mp) && !rc.isLocationOccupied(mp) && rc.canAttack(mp)) {
                        rc.attack(mp);
                        break;
                    }
                }
            }
        }

        while (fleeTurnsRemaining > 0 && moveTowardsDirection(fleeDirection)) {
            fleeTurnsRemaining--;
        }
    }

    // return the closest enemy robot that is a threat
    private RobotInfo closestThreat() throws GameActionException {
        int health = rc.getHealth();
        int enemyLaunchers = 0;

        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo closestThreat = null;
        int closestThreatDist = 100000;
        for (RobotInfo robot : enemyRobots) {
            RobotType robotType = robot.getType();
            if (robotType == RobotType.LAUNCHER) {
                int threatDistance = rc_loc.distanceSquaredTo(robot.getLocation());
                if (threatDistance < closestThreatDist) {
                    closestThreatDist = threatDistance;
                    closestThreat = robot;
                }
                enemyLaunchers++;
            }
        }

        // Determine if robot is under threat
        // TODO: tuning
        if ((enemyLaunchers >= 1 && health < SCARED_THRESHOLD)
                || (enemyLaunchers >= 2)) {
            return closestThreat;
        }

        return null;
    }

}
