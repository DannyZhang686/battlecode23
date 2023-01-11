package sprint1.irc;

import battlecode.common.RobotController;

public class HQChannel {
    private final RobotController rc;
    private final int HQ_ID;

    public HQChannel(RobotController rc, int HQ_ID) {
        this.rc = rc;
        this.HQ_ID = HQ_ID;
    }
    
    // Return value indicates if state is fully synced
    public boolean broadcastInitState() {
        int current_round = rc.getRoundNum();

        if (current_round == 1) {
            // First round
        }


        return false;
    }
}
