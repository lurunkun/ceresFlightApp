package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.utils.MainThreadBus;

public class GpsService extends Service implements
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final int UPDATE_INTERVAL = 50;
    private static final int FASTEST_INTERVAL = 50;
    private static MainThreadBus mMainThreadBus = new MainThreadBus(new Bus());
    private static Bus mSBCThreadBus = new Bus(ThreadEnforcer.ANY);
    public static LatLng mCurrentLatLng;
    LocationRequest mLocationRequest;
    LocationClient mLocationClient;

    public GpsService() {
    }

    public static MainThreadBus getMainThreadBus() {
        return mMainThreadBus;
    }

    public static Bus getSBCThreadBus() {
        return mSBCThreadBus;
    }


    private void initGeolocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "GPS service started", Toast.LENGTH_SHORT).show();
        initGeolocation();
        mLocationClient.connect();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onDestroy();
        Toast.makeText(this, "GPS service stopped", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "GPS locationClient Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        // post location to main thread
        mMainThreadBus.post(new TabletGPSDataEvent(location));
        // post location to SBC send socket
        mSBCThreadBus.post(new TabletGPSDataEvent(location));
        mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

}
