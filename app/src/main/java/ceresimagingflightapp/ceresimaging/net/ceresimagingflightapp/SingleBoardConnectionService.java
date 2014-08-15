package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class SingleBoardConnectionService extends Service {
    private static final int UPDATE_INTERVAL = 2000;
    private static final String SBC_URL = "192.168.44.78:";
    private static final int SBC_PORT = 9000;
    private static final String TAG = "Ceres SBC Connection Service";
    private Timer timer = new Timer();
    private static MainThreadBus mBus = new MainThreadBus(new Bus());

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
                    Log.e(TAG, contentString.toString());
                    // send event through eventBus
                    mBus.post(new SingleBoardDataEvent(contentString.toString()));
                } catch (MalformedURLException e) {
                    Log.e(TAG, "MalformedURLException", e);
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Error timed out", e);
                } catch (IOException e) {
                    Log.e(TAG, "Error connecting to service", e);
                } finally {
                }
            }
        }, 0, UPDATE_INTERVAL);
    }
}
