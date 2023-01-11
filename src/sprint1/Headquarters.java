package sprint1;

import battlecode.common.*;
import sprint1.data.HQMap;
import sprint1.irc.HQChannel;
import java.util.Random;

public class Headquarters extends Robot {
    static final Random rng = new Random(6147);

    // Specification constants
    private final int actionRadius = 9;
    private final Direction[] directions = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    /*
     * CENTER
     * Direction that represents pointing nowhere.
     * EAST
     * Direction that represents pointing east (right on screen).
     * NORTH
     * Direction that represents pointing north (up on screen).
     * NORTHEAST
     * Direction that represents pointing northeast (up and to the right on screen).
     * NORTHWEST
     * Direction that represents pointing northwest (up and to the left on screen).
     * SOUTH
     * Direction that represents pointing south (down on screen).
     * SOUTHEAST
     * Direction that represents pointing southeast (down and to the right on
     * screen).
     * SOUTHWEST
     * Direction that represents pointing southwest (down and to the left on
     * screen).
     * WEST
     */

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
        tryToSpawn();
    }

    private void tryToSpawn() throws GameActionException {
        if (spawnedCarriers < startingCarriers) {
            tryToSpawnCarrier();
            this.spawnedCarriers++;
        } else {
            if (rng.nextBoolean()) {
                tryToSpawnCarrier();
            } else {
                tryToSpawnLauncher();
            }
        }
    }

    private void tryToSpawnCarrier() throws GameActionException {
        // Find wells within action radius of headquarters
        WellInfo[] wells = rc.senseNearbyWells(actionRadius);

        // TODO: Alternate between wells
        if (wells.length > 1) {
            MapLocation wellLoc = wells[0].getMapLocation();
            if (rc.canBuildRobot(RobotType.CARRIER, wellLoc)) {
                rc.buildRobot(RobotType.CARRIER, wellLoc);
                return;
            }
        }

        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);
        // Let's try to build a carrier.
        rc.setIndicatorString("Trying to build a carrier");
        if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
            rc.buildRobot(RobotType.CARRIER, newLoc);
        }
    }

    private void tryToSpawnLauncher() throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);
        // Let's try to build a launcher.
        rc.setIndicatorString("Trying to build a launcher");
        if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
            rc.buildRobot(RobotType.LAUNCHER, newLoc);
        }
    }
}
