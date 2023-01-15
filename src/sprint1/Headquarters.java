package sprint1;

import battlecode.common.*;
import sprint1.data.HQMap;
import sprint1.irc.IrcWriter;
import sprint1.utils.RobotMath;

public class Headquarters extends Robot {

    // Specification constants
    private final int ACTION_RADIUS = 9;
    private final int VISION_RADIUS = 34;
    private final int CARRIER_COST_AD = 50; // Adamantium
    private final int LAUNCHER_COST_MN = 60; // Mana

    private final int ANCHOR_COST_AD = 100;
    private final int ANCHOR_COST_MN = 100;
    private final int ACCEL_ANCHOR_COST_EX = 300;

    private final Direction[] directions = { Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST,
            Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

    private final HQMap map;
    private final IrcWriter irc_writer;

    // Final HQ fields
    private final int HQ_ID;
    private final MapLocation HQ_LOC;
    private final Team friendlyTeam, enemyTeam;

    // Initialize constants
    private int spawnedCarriers = 0;
    private int spawnedLaunchers = 0;
    WellInfo[] nearbyWells;
    int allowedCarriersInRange;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        int hq_id = rc.readSharedArray(0) == 0 ? 0 : 1;

        if (hq_id != 0 && rc.readSharedArray(1) != 0) {
            hq_id++;

            if (rc.readSharedArray(2) != 0) {
                hq_id++;
            }
        }

        this.HQ_ID = hq_id;

        // Set location
        this.HQ_LOC = rc.getLocation();
        // System.out.println("HQ #" + this.HQ_ID + " IS AT " + this.HQ_LOC.x + ", " +
        // this.HQ_LOC.y); // DEBUG

        // Initialize HQ Channel
        this.irc_writer = new IrcWriter(this.HQ_LOC, rc, HQ_ID);

        // Initialize the map
        this.map = new HQMap(HQ_LOC, rc);

        friendlyTeam = rc.getTeam();
        enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;

        nearbyWells = rc.senseNearbyWells();
        allowedCarriersInRange = 8;
        for (WellInfo x : nearbyWells) {
            int dis = x.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (dis >= 16) {
                allowedCarriersInRange += 4;
            } else if (dis >= 9) {
                allowedCarriersInRange += 2;
            }
        }
    }

    @Override
    public void run() throws GameActionException {
        runSetup();

        this.irc_writer.sync(this.map);

        if (rc.isActionReady())
            tryToSpawn();
    }

    private void tryToSpawn() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, friendlyTeam);

        int nearbyCarrierCount = 0;
        for (RobotInfo x : nearbyRobots) {
            if (x.type == RobotType.CARRIER)
                nearbyCarrierCount++;
        }

        if (rc.getRoundNum() < 9) {
            // In the opening, try to spawn carrier first
            if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= CARRIER_COST_AD) {
                tryToSpawnCarrier();
            }
        }
        // try to spawn launcher first
        if (rc.getResourceAmount(ResourceType.MANA) >= LAUNCHER_COST_MN) {
            tryToSpawnLauncher();
        }
        // try to spawn carrier next
        if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= CARRIER_COST_AD
                && nearbyCarrierCount < allowedCarriersInRange) {
            tryToSpawnCarrier();
        }
    }

    private static final int[][] farPlaces = { { 3, 2 }, { 3, 1 }, { 3, 0 }, { 2, 2 }, { 2, 1 }, { 2, 0 }, { 1, 1 },
            { 1, 0 } };

    private void tryToSpawnCarrier() throws GameActionException {
        // Find wells within action radius of headquarters
        // (only needs to be run the first time) - possible to store all wells in this
        // HQ, distribute units between them
        WellInfo[] wells = rc.senseNearbyWells(VISION_RADIUS);

        // TODO: Choose between wells and well resource types
        if (wells.length > 0) {
            for (WellInfo well : wells) {
                MapLocation loc = RobotMath.closestActionablePlacement(rc, HQ_LOC, well.getMapLocation(),
                        ACTION_RADIUS);
                if (loc != null && rc.canBuildRobot(RobotType.CARRIER, loc)) {
                    rc.buildRobot(RobotType.CARRIER, loc);
                    this.spawnedCarriers++;
                    return;
                }
            }
        } else {
            MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(HQ_LOC, ACTION_RADIUS);
            // try to spawn in further squares
            // TODO: try to make it spawn closer to centre of board or wherever it's needed
            // (whatever locaiton broadcasted to launchers)
            for (int i = 0; i < farPlaces.length; i++) {
                int j = rng.nextInt(2); // 0 or 1
                int dx = farPlaces[i][j] * (rng.nextBoolean() ? 1 : -1);
                int dy = farPlaces[i][1 - j] * (rng.nextBoolean() ? 1 : -1);
                MapLocation location = rc.getLocation().translate(dx, dy);
                if (rc.canBuildRobot(RobotType.CARRIER, location)) {
                    rc.buildRobot(RobotType.CARRIER, location);
                    this.spawnedCarriers++;
                    return;
                }
            }
            for (MapLocation location : locations) {
                if (rc.canBuildRobot(RobotType.CARRIER, location)) {
                    rc.buildRobot(RobotType.CARRIER, location);
                    this.spawnedCarriers++;
                    return;
                }
            }
        }
    }

    private void tryToSpawnLauncher() throws GameActionException {
        // TODO: Incorporate placement logic (i.e. spawn near fights)

        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(HQ_LOC, VISION_RADIUS);
        // TODO: change this to prefer spawning further squares?
        for (MapLocation location : locations) {
            if (rc.canBuildRobot(RobotType.LAUNCHER, location)) {
                rc.buildRobot(RobotType.LAUNCHER, location);
                this.spawnedLaunchers++;
                return;
            }
        }
    }

    private void tryToSpawnAnchor() throws GameActionException {
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            rc.buildAnchor(Anchor.STANDARD);
        }
    }
}
