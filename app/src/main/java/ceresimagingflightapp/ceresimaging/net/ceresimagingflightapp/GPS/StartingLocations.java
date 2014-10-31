package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.HashMap;

/**
 * Created by huarui on 10/30/14.
 */
public class StartingLocations {
    private static final HashMap<LatLng, Integer> STARTING_LOCATIONS = new HashMap<LatLng, Integer>();
    static {
        // CA
        STARTING_LOCATIONS.put(new LatLng(36.398487, -119.621887), 7);
        // AUS
        STARTING_LOCATIONS.put(new LatLng(-34.183324, 142.080051), 8);
    }

    public static LatLng getClosest(LatLng currentLocation) {
        double lat = currentLocation.latitude;
        double lng = currentLocation.longitude;
        double shortest = Integer.MAX_VALUE;
        LatLng closest = new LatLng(36.398487, -119.621887);
        for (LatLng l : STARTING_LOCATIONS.keySet()) {
            double dist = SphericalUtil.computeDistanceBetween(new LatLng(lat, lng), l);
            if (dist < shortest) {
                shortest = dist;
                closest = l;
            }
        }
        return closest;
    }

    public static int getZoom(LatLng closest) {
        return STARTING_LOCATIONS.get(closest);
    }
}
