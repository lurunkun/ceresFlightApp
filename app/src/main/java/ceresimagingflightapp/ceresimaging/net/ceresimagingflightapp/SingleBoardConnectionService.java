package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.util.ArrayList;

public class SingleBoardConnectionService extends Service {
    public static final String SBC_URL = "192.168.1.231";
    public static final int RECEIVE_PORT = 9000;
    public static final int SEND_PORT = 3000;
    private static final String TAG = "Ceres SBC Connection";
    private static MainThreadBus mBus = new MainThreadBus(new Bus());

    static boolean inError = false;
    static boolean inWarning = false;
    // list of errors received
    static ArrayList<SingleBoardDataEvent> mErrors = new ArrayList<SingleBoardDataEvent>();

    private static ReceiveSocketThread mReceiveSocketThread;
    private static SendSocketThread mSendSocketThread;

    public SingleBoardConnectionService() {
    }

    public static MainThreadBus getEventBus() {
        return mBus;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
        retrieveDataFromSBC();
        sendDataToSBC();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mReceiveSocketThread.stopThread();
        mSendSocketThread.stopThread();
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

    private void sendDataToSBC() {
        mSendSocketThread =  new SendSocketThread();
        mSendSocketThread.start();
    }

    private void retrieveDataFromSBC() {
        mReceiveSocketThread = new ReceiveSocketThread();
        mReceiveSocketThread.start();
    }
}
