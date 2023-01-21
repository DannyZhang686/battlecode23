package rsprint2;

import battlecode.common.*;
import rsprint2.data.HQMap;
import rsprint2.irc.IrcEvent;
import rsprint2.irc.IrcWriter;
import rsprint2.utils.RobotMath;

public class Headquarters extends Robot {
    private final HQMap map;
    private final IrcWriter irc_writer;

    // Final HQ fields
    private final int HQ_ID;
    private final Team friendlyTeam, enemyTeam;

    // Initialize constants
    private int spawnedCarriers = 0;
    private int spawnedLaunchers = 0;
    WellInfo[] nearbyWells;
    WellInfo[] nearbyAdamantiumWells, nearbyManaWells;
    int allowedCarriersInRange;

    // Favour spawning carriers near adamantium before
    // this round, and near mana after this round
    public static final int MORE_ADAMANTIUM_BEFORE_ROUND = 4;
    public static final double CURRENT_DISTANCE_PENALTY_FACTOR = 1;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        HQ_ID = irc_reader.addAndGetHQID();

        // Initialize the map
        map = new HQMap(rc_loc, rc);

        // Initialize HQ Channel
        irc_writer = new IrcWriter(rc_loc, rc, HQ_ID);

        friendlyTeam = rc.getTeam();
        enemyTeam = this.friendlyTeam == Team.A ? Team.B : Team.A;

        nearbyWells = rc.senseNearbyWells();
        int numAdamantiumWells = 0, numManaWells = 0;

        allowedCarriersInRange = 8;
        // Compute allowedCarriersInRange as well as the number of
        // adamantium and mana wells in range
        for (WellInfo well : nearbyWells) {
            int dis = well.getMapLocation().distanceSquaredTo(rc.getLocation());
            if (dis >= 16) {
                allowedCarriersInRange += 4;
            } else if (dis >= 9) {
                allowedCarriersInRange += 2;
            }
            if (well.getResourceType() == ResourceType.ADAMANTIUM) {
                numAdamantiumWells++;
            } else if (well.getResourceType() == ResourceType.MANA) {
                numManaWells++;
            }
        }
        nearbyAdamantiumWells = new WellInfo[numAdamantiumWells];
        nearbyManaWells = new WellInfo[numManaWells];
        numAdamantiumWells = numManaWells = 0;
        for (WellInfo well : nearbyWells) {
            if (well.getResourceType() == ResourceType.ADAMANTIUM) {
                nearbyAdamantiumWells[numAdamantiumWells] = well;
                numAdamantiumWells++;
            } else if (well.getResourceType() == ResourceType.MANA) {
                nearbyManaWells[numManaWells] = well;
                numManaWells++;
            }
        }
    }

    // TODO: Calculate enemy HQ locations, first with facts and logic, then by
    // sending carrier scouts to determine which orientations are impossible
    // how to get carrier scouts to report back?
    boolean[] isPossibleOrientation = { true, true, true };
    // 0 = hor, 1 = vert, 2 = rot (aka both 0 and 1)

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
            if (x.type == RobotType.CARRIER) {
                nearbyCarrierCount++;
            }
        }

        int curRound = rc.getRoundNum();
        int adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int mana = rc.getResourceAmount(ResourceType.MANA);
        if (curRound < 10) {
            // In the opening, try to spawn carriers first
            if (adamantium >= Constants.CARRIER_COST_AD) {
                tryToSpawnCarrier();
            }
            if (mana >= Constants.LAUNCHER_COST_MN) {
                int launcher_id = tryToSpawnLauncher();

                if (launcher_id != 0 && spawnedLaunchers <= 3) {
                    Direction hq_dir = MAP_CENTER.directionTo(rc_loc);
                    MapLocation spawn_location = MAP_CENTER.add(hq_dir).add(hq_dir);

                    if (spawnedLaunchers == 2) {
                        spawn_location = spawn_location.add(hq_dir.rotateLeft());
                    } else if (spawnedLaunchers == 3) {
                        spawn_location = spawn_location.add(hq_dir.rotateRight());
                    }

                    // int data[] = IrcEvent.serializeHoldLocation(spawn_location, launcher_id);

                    // irc_writer.writeBufferEvent(IrcEvent.HOLD_LOCATION, data[0], data[1]);
                }
            }
        } else if (curRound < 150) {
            // Try to spawn launchers, then carriers
            if (rc.getResourceAmount(ResourceType.MANA) >= Constants.LAUNCHER_COST_MN) {
                tryToSpawnLauncher();
            }
            if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= Constants.CARRIER_COST_AD
                    && nearbyCarrierCount < allowedCarriersInRange) {
                tryToSpawnCarrier();
            }
        } else {
            // Be more conservative with launcher/carrier spawning, and
            // try to get anchors out
            if (rc.getNumAnchors(Anchor.STANDARD) == 0) {
                tryToSpawnAnchor();
            }
            // If you hate magic numbers, now is the time to shield your eyes
            if (rc.getResourceAmount(ResourceType.MANA) >= 150) {
                tryToSpawnLauncher();
            }
            // Harsher restriction because adamantium should never be the
            // limiting factor to anchor spawning
            if (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 200
                    && nearbyCarrierCount < allowedCarriersInRange) {
                tryToSpawnCarrier();
            }
        }
    }

    private static final int[][] farPlaces = { { 3, 2 }, { 3, 1 }, { 3, 0 }, { 2, 2 }, { 2, 1 }, { 2, 0 }, { 1, 1 },
            { 1, 0 } };

    private void tryToSpawnCarrier() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }

        // Find wells to spawn carriers near

        // TODO: Choose between wells and well resource types
        if (nearbyWells.length > 0) {
            if (rc.getRoundNum() < MORE_ADAMANTIUM_BEFORE_ROUND) {
                // Try an adamantium spawn, then a mana spawn
                int randIndex;
                if (nearbyAdamantiumWells.length != 0) {
                    randIndex = rng.nextInt(nearbyAdamantiumWells.length);
                    if (standardCarrierSpawn(nearbyAdamantiumWells[randIndex].getMapLocation())) {
                        return;
                    }
                } else if (nearbyManaWells.length != 0) {
                    randIndex = rng.nextInt(nearbyManaWells.length);
                    if (standardCarrierSpawn(nearbyManaWells[randIndex].getMapLocation())) {
                        return;
                    }
                }
            } else {
                // Try two mana spawns, then an adamantium spawn
                int randIndex;
                if (nearbyManaWells.length != 0) {
                    randIndex = rng.nextInt(nearbyManaWells.length);
                    if (standardCarrierSpawn(nearbyManaWells[randIndex].getMapLocation())) {
                        return;
                    }
                    randIndex = rng.nextInt(nearbyManaWells.length);
                    if (standardCarrierSpawn(nearbyManaWells[randIndex].getMapLocation())) {
                        return;
                    }
                } else if (nearbyAdamantiumWells.length != 0) {
                    randIndex = rng.nextInt(nearbyAdamantiumWells.length);
                    if (standardCarrierSpawn(nearbyAdamantiumWells[randIndex].getMapLocation())) {
                        return;
                    }
                }
            }
        }

        // If we haven't returned, we haven't spawned
        // Try spawning randomly
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc_loc, Constants.HQ_ACTION_RADIUS);
        // try to spawn in further squares
        // TODO: try to make it spawn closer to centre of board or wherever it's needed
        // (whatever location broadcasted to launchers)
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

    private int tryToSpawnLauncher() throws GameActionException {
        if (!rc.isActionReady()) {
            return 0;
        }
        // Spawn as close to center as possible
        MapLocation spawnLocation = null;
        int distanceSquaredFromCenter = 100000;
        int curRound = rc.getRoundNum();

        for (MapLocation location : rc.getAllLocationsWithinRadiusSquared(rc_loc, Constants.HQ_VISION_RADIUS)) {
            if (rc.canBuildRobot(RobotType.LAUNCHER, location)) {
                // At the start, avoid spawning launchers on currents because it disperses them
                int weightedDistanceSquared = MAP_CENTER.distanceSquaredTo(location);
                if ((curRound < 20) && (rc.senseMapInfo(location).getCurrentDirection() != Direction.CENTER)) {
                    weightedDistanceSquared += (int) (CURRENT_DISTANCE_PENALTY_FACTOR
                            * Math.sqrt(weightedDistanceSquared));
                }
                if (weightedDistanceSquared < distanceSquaredFromCenter) {
                    distanceSquaredFromCenter = weightedDistanceSquared;
                    spawnLocation = location;
                }
            }
        }
        if (spawnLocation == null) {
            return 0;
        }
        if (rc.canBuildRobot(RobotType.LAUNCHER, spawnLocation)) {
            rc.buildRobot(RobotType.LAUNCHER, spawnLocation);
            this.spawnedLaunchers++;
            return rc.senseRobotAtLocation(spawnLocation).getID();
        }
        return 0;
    }

    private void tryToSpawnAnchor() throws GameActionException {
        // Not sure if anchor spawning requires cooldown, but
        // canBuildAnchor should check that in any case
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            rc.buildAnchor(Anchor.STANDARD);
        }
    }

    // Tries to do a carrier spawn near the provided location m,
    // using the "standard" algorithm
    // Returns whether the robot was successfully spawned or not
    private boolean standardCarrierSpawn(MapLocation m) throws GameActionException {
        MapLocation loc = RobotMath.closestActionablePlacement(rc, rc_loc, m, Constants.HQ_ACTION_RADIUS);
        if (loc != null && rc.canBuildRobot(RobotType.CARRIER, loc)) {
            rc.buildRobot(RobotType.CARRIER, loc);
            spawnedCarriers++;
            return true;
        }
        return false;
    }
}
