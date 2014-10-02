package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SingleBoardConnectionService extends Service {
    private static final int UPDATE_INTERVAL = 50;
    private static final String SBC_URL = "192.168.1.11";
    private static final int SBC_PORT = 9000;
    private static final String TAG = "Ceres SBC Connection Service";
    private static Timer timer = new Timer();
    private static MainThreadBus mBus = new MainThreadBus(new Bus());

    static boolean inError = false;
    static boolean inWarning = false;
    // list of errors received
    static ArrayList<SingleBoardDataEvent> mErrors = new ArrayList<SingleBoardDataEvent>();

    private static TimerTask mRetrieveDataTask;

    public SingleBoardConnectionService() {
    }

    public static MainThreadBus getEventBus() {
        return mBus;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
        mRetrieveDataTask = createRetrieveDataTask();
        retrieveDataFromSBC();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SingleBoardConnectionService.timer.cancel();
        SingleBoardConnectionService.timer.purge();
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

    public static void requestRestartSBC() {
        pauseRetrieveDataTask();
        Thread thread = new Thread(new Runnable() {
            InputStream content;
            @Override
            public void run() {
                try {
//                    HttpClient httpClient = new DefaultHttpClient();
//                    HttpResponse response = httpClient.execute(new HttpGet("http://" + SBC_URL + SBC_PORT + "/reboot"));
//                    content = response.getEntity().getContent();
//                    BufferedReader read = new BufferedReader(new InputStreamReader(content));
                    Socket socket = new Socket(SBC_URL, SBC_PORT);
                    BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder contentString = new StringBuilder();
                    String line;
                    while ((line = read.readLine()) != null) {
                        contentString.append(line);
                    }
                    String responseString = contentString.toString();
                    if (responseString.equals("restarting")) {
                        // if restarting
                        mBus.post(new SingleBoardConnectionEvent(false, true));
                    } else {
                        // if not restarting
                        mBus.post(new SingleBoardConnectionEvent(false, false));
                    }
                    read.close();
                    resumeRetrieveDataTask();
                } catch (MalformedURLException e) {

                } catch (IOException e) {

                }

            }
        });
        thread.start();
    }

    public static void clearErrors() {
        mErrors.clear();
        inError = false;
    }

    private static void pauseRetrieveDataTask() {
        SingleBoardConnectionService.timer.cancel();
        SingleBoardConnectionService.timer.purge();
    }
    private static void resumeRetrieveDataTask() {
        SingleBoardConnectionService.timer = new Timer();
        mRetrieveDataTask = createRetrieveDataTask();
        SingleBoardConnectionService.timer.scheduleAtFixedRate(mRetrieveDataTask, 0, UPDATE_INTERVAL);
    }

    private static TimerTask createRetrieveDataTask() {
        return new TimerTask() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
//                InputStream content;
                try {
//                    HttpClient httpClient = new DefaultHttpClient();
//                    HttpResponse response = httpClient.execute(new HttpGet("http://" + SBC_URL + SBC_PORT));
//                    content = response.getEntity().getContent();
                    Socket socket = new Socket(SBC_URL, SBC_PORT);
                    BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder contentString = new StringBuilder();
                    String line;
                    while ((line = read.readLine()) != null) {
                        contentString.append(line);
                    }
                    Log.e(TAG, contentString.toString());
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
                } catch (MalformedURLException e) {
                    Log.e(TAG, "LOG MalformedURLException", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                    SingleBoardConnectionService.inError = false;
                    SingleBoardConnectionService.inWarning = false;
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "LOG Error timed out", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                    SingleBoardConnectionService.inError = false;
                    SingleBoardConnectionService.inWarning = false;
                } catch (HttpHostConnectException e) {
                    Log.e(TAG, "LOG connection refused");
                    mBus.post(new SingleBoardConnectionEvent(false));
                    SingleBoardConnectionService.inError = false;
                    SingleBoardConnectionService.inWarning = false;
                } catch (IOException e) {
                    Log.w(TAG, "LOG Error connecting to service", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                    SingleBoardConnectionService.inError = false;
                    SingleBoardConnectionService.inWarning = false;
                } finally {
                }
            }
        };
    }

    private void retrieveDataFromSBC() {
//        timer = new Timer();
        SingleBoardConnectionService.timer.scheduleAtFixedRate(mRetrieveDataTask, 0, UPDATE_INTERVAL);
    }
}
