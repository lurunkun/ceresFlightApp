package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.utils.MainThreadBus;

/**
 * Created by huaruiwu on 10/2/14.
 */
public class ReceiveSocketThread extends Thread {

    static MainThreadBus mBus = SingleBoardConnectionService.getEventBus();
    private static final String TAG = "Ceres SBC Connection";

    private boolean stopped = false;

    public void stopThread() {
        this.stopped = true;
    }

    @Override
    public void run() {
        while(!this.stopped) {
            try {
                Socket socket = new Socket(SingleBoardConnectionService.SBC_URL, SingleBoardConnectionService.RECEIVE_PORT);
                mBus.post(new SingleBoardConnectionEvent(true));
                BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // all lines read in contentString
                StringBuilder contentString = new StringBuilder();
                String line;
                while ((line = read.readLine()) != null) {
                    contentString.append(line);
                    Log.e(TAG, line);
                    // parse the string retrieved to JSON Array
                    try {
                        JSONArray statusArray = new JSONArray(line);
                        for (int i=0; i < statusArray.length(); i++) {
                            JSONObject statusObject = statusArray.getJSONObject(i);
                            // send statusObject event through eventBus
                            mBus.post(new SingleBoardDataEvent(statusObject));
                            String status = statusObject.getString("type");
                            // if received error
                            if (status.equals(SingleBoardStatus.ERROR)) {
                                SingleBoardConnectionService.inError = true;
                                SingleBoardConnectionService.mErrors.add(new SingleBoardDataEvent(statusObject));
                                // if received warning
                            } else if (status.equals(SingleBoardStatus.WARNING)) {
                                SingleBoardConnectionService.inWarning = true;
                            }
                        }
                        mBus.post(new SingleBoardConnectionEvent(true));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Status Parse Failed.", e);
                    }
                }
            } catch (IOException e) {
                mBus.post(new SingleBoardConnectionEvent(false));
                Log.w(TAG, "Read thread error connecting to SBC");
                e.printStackTrace();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    Log.e(TAG, "Error socket connect sleep error");
                    e1.printStackTrace();
                }
            }
        }
    }
}
