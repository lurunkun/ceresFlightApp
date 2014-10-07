package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.maps.android.SphericalUtil;

import java.util.List;

/**
 * Created by huaruiwu on 10/7/14.
 */
public class GeoUtils {

    public static double getTrackDist(LatLng a, LatLng b, Location currentLocation) {
        final double R = 6371009;
        LatLng current = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        double currentHeading = currentLocation.getBearing();
        double distAC = SphericalUtil.computeDistanceBetween(a, current);
        double bearingAC = SphericalUtil.computeHeading(a, current);
        double bearingAB = SphericalUtil.computeHeading(a, b);
        double bearingRadAC = Math.toRadians(bearingAC);
        double bearingRadAB = Math.toRadians(bearingAB);
        double trackDist = Math.asin(Math.sin(distAC/R) * Math.sin(bearingRadAC - bearingRadAB)) * R;
        if (bearingAB < 0) {
            bearingAB += 360;
        }
        double angleDiff = Math.abs(currentHeading - bearingAB) % 360;
        angleDiff = angleDiff > 180 ? 360 - angleDiff : angleDiff;
        if (angleDiff > 90) {
            trackDist = trackDist * -1;
        }
        return trackDist;
    }

    public static double toFeet(double distance) {
        return distance * 3.28084;
    }
    public static double toMeters(double distance) {
        return distance * 0.3048;
    }
    public static double toMiles(double distance) { return distance / 1609.34; }
    public static LatLng getPolyCenter(List<LatLng> polygon) {
        double latitude = 0;
        double longitude = 0;
        int totalPoints = polygon.size();
        for (int i = 0; i < polygon.size(); i++ ) {
            latitude  += polygon.get(i).latitude;
            longitude += polygon.get(i).longitude;
        }
        return new LatLng(latitude/totalPoints, longitude/totalPoints);
    }
    public static Marker getPolygonMarker(Polygon polygon, List<Marker> markers) {
        List<LatLng> points = polygon.getPoints();
        LatLng center = getPolyCenter(points);
        double shortestDist = 100000;
        Marker closestMarker = null;
        for (Marker marker : markers) {
            if (closestMarker == null) {
                shortestDist = Math.abs(SphericalUtil.computeDistanceBetween(marker.getPosition(), center));
                closestMarker = marker;
            } else {
                double dist = Math.abs(SphericalUtil.computeDistanceBetween(marker.getPosition(), center));
                if (dist < shortestDist) {
                    shortestDist = dist;
                    closestMarker = marker;
                }
            }
        }
        return closestMarker;
    }
}
