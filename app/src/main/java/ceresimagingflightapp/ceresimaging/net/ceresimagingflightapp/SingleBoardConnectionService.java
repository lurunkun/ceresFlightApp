package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class SingleBoardConnectionService extends Service {
    private static final int UPDATE_INTERVAL = 50;
    private static final String SBC_URL = "192.168.1.11";
    private static final int SBC_PORT = 9000;
    private static final String TAG = "Ceres SBC Connection Service";
    private static MainThreadBus mBus = new MainThreadBus(new Bus());

    static boolean inError = false;
    static boolean inWarning = false;
    // list of errors received
    static ArrayList<SingleBoardDataEvent> mErrors = new ArrayList<SingleBoardDataEvent>();

    private static Thread mSocketThread;

    public SingleBoardConnectionService() {
    }

    public static MainThreadBus getEventBus() {
        return mBus;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
        retrieveDataFromSBC();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSocketThread.interrupt();
        super.onDestroy();
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void clearErrors() {
        mErrors.clear();
        inError = false;
    }

    private void retrieveDataFromSBC() {
        mSocketThread = new Thread() {
            public void run() {
                while(!this.isInterrupted()) {
                    try {
                        Socket socket = new Socket(SBC_URL, SBC_PORT);
                        mBus.post(new SingleBoardConnectionEvent(true));
                        BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        StringBuilder contentString = new StringBuilder();
                        String line;
                        while ((line = read.readLine()) != null) {
                            contentString.append(line);
                            Log.e(TAG, line);
                        }
                        // parse the string retrieved to JSON Array
                        try {
                            JSONArray statusArray = new JSONArray(contentString.toString());
                            for (int i=0; i < statusArray.length(); i++) {
                                JSONObject statusObject = statusArray.getJSONObject(i);
                                // send statusObject event through eventBus
                                mBus.post(new SingleBoardDataEvent(statusObject));
                                String status = statusObject.getString("type");
                                // if received error
                                if (status.equals(SingleBoardStatus.ERROR)) {
                                    SingleBoardConnectionService.inError = true;
                                    mErrors.add(new SingleBoardDataEvent(statusObject));
                                    // if received warning
                                } else if (status.equals(SingleBoardStatus.WARNING)) {
                                    SingleBoardConnectionService.inWarning = true;
                                }
                            }
                            mBus.post(new SingleBoardConnectionEvent(true));
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Status Parse Failed.", e);
                        }
                    } catch (IOException e) {
                        mBus.post(new SingleBoardConnectionEvent(false));
                        Log.w(TAG, "LOG Warning connecting to SBC");
                        e.printStackTrace();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            Log.e(TAG, "LOG Error socket connect sleep error");
                            e1.printStackTrace();
                        }
                    }
                }
            }
        };
        mSocketThread.start();
    }
}
