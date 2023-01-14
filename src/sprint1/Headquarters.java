package sprint1;

import battlecode.common.*;
import sprint1.data.HQMap;
import sprint1.irc.HQChannel;
import sprint1.utils.RobotMath;

public class Headquarters extends Robot {

    // Specification constants
    private final int actionRadius = 9;
    private final int visionRadius = 34;
    private final Direction[] directions = { Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST,
            Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

    private final HQMap map;
    private final HQChannel channel;

    // Final HQ fields
    private final int HQ_ID;
    private final MapLocation HQ_LOC;

    // Initialize constants
    private final int startingCarriers = 4;
    private int spawnedCarriers;
    private int spawnedLaunchers;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        // Take a unique HQ number from the 63rd array element,
        // and increment that element so the next HQ will have
        // a different HQ number
        // TODO: Move to channel
        this.HQ_ID = rc.readSharedArray(63);
        rc.writeSharedArray(63, this.HQ_ID + 1);

        // Set location
        this.HQ_LOC = rc.getLocation();
        System.out.println("HQ #" + this.HQ_ID + " IS AT " + this.HQ_LOC.x + ", " + this.HQ_LOC.y); // DEBUG

        // Initialize HQ Channel
        this.channel = new HQChannel(this.HQ_LOC, rc, HQ_ID);

        // Initialize the map
        this.map = new HQMap(HQ_LOC, rc);

        spawnedCarriers = 0;
        spawnedLaunchers = 0;
    }

    @Override
    public void run() throws GameActionException {
        this.channel.sync(this.map);

//        if (this.rc.getRoundNum() > 4) {
//            return;
//        }

        if(rc.isActionReady()) tryToSpawn();
    }

    private void tryToSpawn() throws GameActionException {
        if (spawnedCarriers < startingCarriers) {
            tryToSpawnCarrier();
        } else {
            if (rng.nextBoolean()) {
                tryToSpawnCarrier();
            } else {
                tryToSpawnLauncher();
            }
        }
    }

    private void tryToSpawnCarrier() throws GameActionException {
        rc.setIndicatorString("Trying to build a carrier");
        // Find wells within action radius of headquarters
        // (only needs to be run the first time) - possible to store all wells in this HQ, distribute units between them
        WellInfo[] wells = rc.senseNearbyWells(visionRadius);

        // TODO: Choose between wells
        if(wells.length > 0){
            for (WellInfo well: wells){
                MapLocation loc = RobotMath.closestActionablePlacement(rc, HQ_LOC, well.getMapLocation(), actionRadius);
                if (loc != null && rc.canBuildRobot(RobotType.CARRIER, loc)) {
                    rc.buildRobot(RobotType.CARRIER, loc);
                    return;
                }
            }
        }else{
//            Direction dir = directions[rng.nextInt(directions.length)];
//            MapLocation newLoc = rc.getLocation().add(dir);
//            // Let's try to build a carrier.
//            rc.setIndicatorString("Trying to build a carrier");
//            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
//                rc.buildRobot(RobotType.CARRIER, newLoc);
//                this.spawnedCarriers++;
//                return;
//            }

            MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(HQ_LOC, actionRadius);
            for(MapLocation location: locations){
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
        rc.setIndicatorString("Trying to build a launcher");

//        Direction dir = directions[rng.nextInt(directions.length)];
//        MapLocation newLoc = rc.getLocation().add(dir);
//        // Let's try to build a launcher.
//        if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
//            rc.buildRobot(RobotType.LAUNCHER, newLoc);
//            this.spawnedLaunchers++;
//            return;
//        }

        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(HQ_LOC, actionRadius);
        for(MapLocation location: locations){
            if (rc.canBuildRobot(RobotType.LAUNCHER, location)) {
                rc.buildRobot(RobotType.LAUNCHER, location);
                this.spawnedLaunchers++;
                return;
            }
        }
    }
}
