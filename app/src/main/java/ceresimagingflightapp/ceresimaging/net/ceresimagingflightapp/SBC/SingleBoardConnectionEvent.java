package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC;

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
}
