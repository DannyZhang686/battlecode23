package sprint1;

import battlecode.common.*;
import sprint1.data.HQMap;
import sprint1.irc.HQChannel;

public class Headquarters extends Robot {

    private final HQMap map;
    private final HQChannel channel;

    // Final HQ fields
    private final int HQ_ID;
    private final MapLocation HQ_LOC;

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
    }

    @Override
    public void run() throws GameActionException {
        this.channel.sync(this.map);

        this.map.print();
    }
}
