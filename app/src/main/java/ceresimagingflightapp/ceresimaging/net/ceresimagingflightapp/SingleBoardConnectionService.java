package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class SingleBoardConnectionService extends Service {
    private static final int UPDATE_INTERVAL = 100;
    private static final String SBC_URL = "192.168.0.5:";
    private static final int SBC_PORT = 9000;
    private static final String TAG = "Ceres SBC Connection Service";
    private Timer timer = new Timer();
    private static MainThreadBus mBus = new MainThreadBus(new Bus());

    static final String STATUS = "Status";
    static final String ERROR = "Error";
    static boolean inError = false;

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
        super.onDestroy();
        timer.cancel();
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

    private void retrieveDataFromSBC() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                InputStream content;
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpResponse response = httpClient.execute(new HttpGet("http://" + SBC_URL + SBC_PORT));
                    content = response.getEntity().getContent();
                    BufferedReader read = new BufferedReader(new InputStreamReader(content));
                    StringBuilder contentString = new StringBuilder();
                    String line;
                    while ((line = read.readLine()) != null) {
                        contentString.append(line);
                    }
                    // parse the string retrieved to JSON Array
                    try {
                        JSONArray statusArray = new JSONArray(contentString.toString());
                        for (int i=0; i < statusArray.length(); i++) {
                            JSONObject statusObject = statusArray.getJSONObject(i);
                            // send statusObject event through eventBus
                            mBus.post(new SingleBoardDataEvent(statusObject));
                            if (statusObject.getString("type").equals(SingleBoardConnectionService.ERROR)) {
                                SingleBoardConnectionService.inError = true;
                            }
                        }
                        mBus.post(new SingleBoardConnectionEvent(true));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Status Parse Failed.", e);
                    }
                } catch (MalformedURLException e) {
                    Log.e(TAG, "LOG MalformedURLException", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "LOG Error timed out", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                } catch (HttpHostConnectException e) {
                    Log.e(TAG, "LOG connection refused");
                    mBus.post(new SingleBoardConnectionEvent(false));
                } catch (IOException e) {
                    Log.e(TAG, "LOG Error connecting to service", e);
                    mBus.post(new SingleBoardConnectionEvent(false));
                } finally {
                }
            }
        }, 0, UPDATE_INTERVAL);
    }
}
