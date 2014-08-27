package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

/**
 * Created by huaruiwu on 8/24/14.
 */
public class SingleBoardConnectionEvent {

    public boolean connected;
    public boolean restarting;

    SingleBoardConnectionEvent(boolean status) {
        this.connected = status;
        this.restarting = false;
    }
    SingleBoardConnectionEvent(boolean status, boolean restarting) {
        this.connected = status;
        this.restarting = restarting;
    }

}
