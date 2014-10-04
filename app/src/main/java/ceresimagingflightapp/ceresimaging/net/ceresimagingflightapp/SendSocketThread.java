package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.util.Log;

import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by huaruiwu on 10/2/14.
 */
public class SendSocketThread extends Thread{

    private static final String TAG = "Ceres SBC Connection";
    private Socket mSocket;
    private boolean stopped = false;

    public Socket getSocket() {
        return mSocket;
    }

    public void stopThread() {
        this.stopped = true;
    }

    @Override
    public void run() {
        MainActivity.getEventBus().register(this);
        try {
            while (!this.stopped && (mSocket == null || mSocket.getInputStream().read() == -1)) {
                if (mSocket != null) {
                    mSocket.close();
                }
                try {
                    mSocket = new Socket(SingleBoardConnectionService.SBC_URL, SingleBoardConnectionService.SEND_PORT);
                } catch (IOException e) {
                    Log.w(TAG, "Warning write thread error connecting to SBC");
                    e.printStackTrace();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        Log.e(TAG, "Error socket connect sleep error");
                        e1.printStackTrace();
                    }
                } finally {
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "sending socket read error");
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onTabletGPSDataEvent(TabletGPSDataEvent event) {
        String time = Long.toString(event.location.getTime());
        String lat = Double.toString(event.location.getLatitude());
        String lng = Double.toString(event.location.getLongitude());
        if (mSocket != null) {
            try {
                PrintWriter out = new PrintWriter(mSocket.getOutputStream());
                out.print(lat + " " + lng + " " + time + "\r\n");
                out.flush();
            } catch (IOException e) {
                Log.e(TAG, "Socket write error");
                e.printStackTrace();
            }
        }
    }
}
