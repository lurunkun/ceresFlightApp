package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.UI;

import android.os.Handler;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS.GpsService;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.R;

/**
 * Created by huaruiwu on 10/24/14.
 */
public class BreadCrumbs {

    private static Handler mHandler;
    private static boolean mRunning = false;
    private static GoogleMap mMap;
    private static List<Marker> mBreadCrumbs = new ArrayList<Marker>();

    private static long INTERVAL = 10000;
    private static int CRUMB_SIZE = 200;

    public static void startBreadCrumb(GoogleMap map) {
        if (!mRunning) {
            mMap = map;
            mRunning = true;
            mHandler = new Handler();
            mHandler.postDelayed(mRunnable, INTERVAL);
        }
    }

    private static Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (GpsService.mCurrentLatLng != null) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot))
                                .position(GpsService.mCurrentLatLng)
                );
                mBreadCrumbs.add(marker);
                if (mBreadCrumbs.size() > CRUMB_SIZE) {
                    mBreadCrumbs.get(0).remove();
                    mBreadCrumbs.remove(0);
                }
            }
            mHandler.postDelayed(mRunnable, INTERVAL);
        }
    };
}
