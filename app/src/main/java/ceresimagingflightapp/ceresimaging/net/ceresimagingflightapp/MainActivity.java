package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "FlightApp";
    private static final String SERVICE_URL = "http://huaruiwu.github.io/ceresGeoApp/flights/flight1.json";
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 1000;
    private static final boolean IS_DEV = true;
    private GoogleMap mMap;
    LocationRequest mLocationRequest;
    LocationClient mLocationClient;

    private LatLng mCurrentLatLng;
    private Location mLocationCurrent;
    private Location mLocationA;
    private Location mLocationB;
    private LatLng mInterpA;
    private LatLng mInterpB;
    private Marker mMarkerA;
    private Marker mMarkerB;
    private Polyline mPathLine;
    private int mShiftDist = (int) Math.round(MainActivity.toMeters(850));
    private int mPassNumber;
    private List<Marker> mFlightMarkers = new ArrayList<Marker>();
    private Polyline mFlightLine;
    private Marker mDestinationMarker;

    private boolean mIsFollowing = false;
    private boolean mIsRotating = false;
    private boolean mIsFlightLineVis = true;

    private TextView mTextCurrentLocation;
    private TextView mTextTrackDist;
    private TextView mTextPassNumber;
    private ToggleButton mToggleCurrentLocation;
    private ToggleButton mToggleFlightLine;
    private SeekBar mSeekBarSlider;
    private Button mButtonToggleSeekBar;
    private ImageView mImageTrackDistDir;
    private Drawable mDrawableLeft;
    private Drawable mDrawableRight;

    public static double getTrackDist(LatLng a, LatLng b, Location currentLocation) {
        final double R = 6371009;
        LatLng current = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        double currentHeading = currentLocation.getBearing();
        double distAC = SphericalUtil.computeDistanceBetween(a, current);
        double bearingAC = SphericalUtil.computeHeading(a, current);
        double bearingAB = SphericalUtil.computeHeading(a, b);
        bearingAC = Math.toRadians(bearingAC);
        bearingAB = Math.toRadians(bearingAB);
        double trackDist = Math.asin(Math.sin(distAC/R) * Math.sin(bearingAC - bearingAB)) * R;
        double angleDiff = Math.abs(currentHeading - bearingAB) % 360;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToggleCurrentLocation = (ToggleButton) findViewById(R.id.toggle_current_location);
        mToggleFlightLine = (ToggleButton) findViewById(R.id.toggle_flight_line);
        mTextTrackDist = (TextView) findViewById(R.id.text_track_dist);
        mTextCurrentLocation = (TextView) findViewById(R.id.current_location);
        mTextPassNumber = (TextView) findViewById(R.id.text_pass_number);
        mImageTrackDistDir = (ImageView) findViewById(R.id.image_trackDist_direction);
        mDrawableLeft = getResources().getDrawable(R.drawable.ic_action_back);
        mDrawableRight = getResources().getDrawable(R.drawable.ic_action_forward);
        ColorFilter filter = new LightingColorFilter(Color.RED, Color.RED);
        mDrawableLeft.setColorFilter(filter);
        mDrawableRight.setColorFilter(filter);
        mImageTrackDistDir.setImageDrawable(mDrawableLeft);
        mButtonToggleSeekBar = (Button) findViewById(R.id.button_toggle_slider);
        mSeekBarSlider = (SeekBar) findViewById(R.id.seekBar_slider);
        mSeekBarSlider.setMax(1200);
        mSeekBarSlider.setProgress(850);
        mSeekBarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    int feet = (int) Math.round(MainActivity.toFeet(i));
                    mButtonToggleSeekBar.setText(Integer.toString(feet));
                    mShiftDist = i;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (checkPlayServices()) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setOnMarkerClickListener(this);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        retrieveAndAddCities();
                    } catch (IOException e) {
                        Log.e(TAG, "Cannot retrieve data", e);
                        return;
                    }
                }
            }).start();
        } else {
            LinearLayout layoutArrow = (LinearLayout) findViewById(R.id.layout_arrow);
            layoutArrow.setVisibility(View.INVISIBLE);
            mToggleFlightLine.setVisibility(View.INVISIBLE);
            mToggleCurrentLocation.setVisibility(View.INVISIBLE);
            ToggleButton toggleRotation = (ToggleButton) findViewById(R.id.toggle_rotation);
            toggleRotation.setVisibility(View.INVISIBLE);
        }
        initGeolocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsFollowing = mToggleCurrentLocation.isChecked();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, "This device is not supported.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    private void initGeolocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationClient = new LocationClient(this, this, this);
    }

    protected void retrieveAndAddCities() throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service
            URL url = new URL(SERVICE_URL);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[10000];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Create markers for the city data.
        // Must run this on the UI thread since it's a UI operation.
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    createMarkersFromJson(json.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing JSON", e);
                }
            }
        });
    }

    void createMarkersFromJson(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        JSONArray features = jsonObj.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject geometry = feature.getJSONObject("geometry");
            String name = feature.getJSONObject("properties").getString("name");

            JSONArray coords = geometry.getJSONArray("coordinates");
            // if feature is a point
            if (geometry.getString("type").equals("Point")) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(coords.getDouble(1), coords.getDouble(0))));
                marker.setTitle(name);
                mFlightMarkers.add(marker);
            // if feature is polygon
            } else if (geometry.getString("type").equals("Polygon")) {
                coords = coords.getJSONArray(0);
                PolygonOptions options = new PolygonOptions();
                for (int j = 0; j < coords.length(); j++){
                    JSONArray coord = coords.getJSONArray(j);
                    options.add(new LatLng(coord.getDouble(1), coord.getDouble(0)));
                }
                options.strokeColor(Color.BLUE).fillColor(Color.TRANSPARENT);
                Polygon polygon = mMap.addPolygon(options);
            }
        }
    }

    public void onToggleCurrentLocation(View view) {
        if (mMap != null) {
            mIsFollowing = ((ToggleButton) view).isChecked();
            if (mIsFollowing) {
                float zoom = mMap.getCameraPosition().zoom;
                CameraPosition cameraPosition;
                if (mIsRotating) {
                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
                            .zoom(zoom).bearing(mLocationCurrent.getBearing()).build();
                } else {
                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
                            .zoom(zoom).build();
                }
                mMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition));
            } else {

            }
        }
    }

    public void onToggleRotation(View view) {
        if (mMap != null) {
            mIsRotating = ((ToggleButton) view).isChecked();
            CameraPosition cameraPosition = null;
            if (mIsRotating) {
                if (mIsFollowing) {
                    float zoom = mMap.getCameraPosition().zoom;
                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
                            .zoom(zoom).bearing(mLocationCurrent.getBearing()).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            } else {
                float zoom = mMap.getCameraPosition().zoom;
                cameraPosition = new CameraPosition.Builder()
                        .target(mCurrentLatLng)
                        .zoom(zoom).bearing(0).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

        }
    }

    public void onToggleFlightLine(View view) {
        if (mMap != null) {
            mIsFlightLineVis = ((ToggleButton) view).isChecked();
            if (mFlightLine != null) {
                mFlightLine.setVisible(mIsFlightLineVis);
            }
        }
    }

    public void onClickButtonA(View view) {
        if (mMap != null) {
            mLocationA = mLocationClient.getLastLocation();
            mPassNumber = 0;
            view.setBackgroundColor(Color.RED);
            Button button_B = (Button) findViewById(R.id.button_B);
            button_B.setBackgroundColor(Color.LTGRAY);
            if (mPathLine != null) {
                mPathLine.setVisible(false);
            }
            if (IS_DEV) {
                TextView locationA = (TextView) findViewById(R.id.text_point_A);
                locationA.setText(mLocationA.getLatitude() + " " + mLocationA.getLongitude());
            }

        }
    }

    public void onClickButtonB(View view) {
        if (mMap != null) {
            if (mLocationA != null){
                mPassNumber = 0;
                mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
                mLocationB = mLocationClient.getLastLocation();
                view.setBackgroundColor(Color.RED);
                LatLng pointA = new LatLng(mLocationA.getLatitude(), mLocationA.getLongitude());
                LatLng pointB = new LatLng(mLocationB.getLatitude(), mLocationB.getLongitude());
                mInterpA = SphericalUtil.computeOffset(pointB, 500, SphericalUtil.computeHeading(pointB, pointA));
                mInterpB = SphericalUtil.computeOffset(pointA, 500, SphericalUtil.computeHeading(pointA, pointB));

                if (mMarkerA != null && mMarkerB != null) {
                    mMarkerA.setPosition(mInterpA);
                    mMarkerB.setPosition(mInterpB);
                } else {
                    mMarkerA = mMap.addMarker(new MarkerOptions()
                            .position(mInterpA));
                    mMarkerA.setTitle("interpA");
                    mMarkerB = mMap.addMarker(new MarkerOptions()
                            .position(mInterpB));
                    mMarkerB.setTitle("interpB");
                }
//            if (IS_DEV) {
//                mMap.addMarker(new MarkerOptions()
//                        .position(mInterpA)).setTitle("interp A");
//                mMap.addMarker(new MarkerOptions()
//                        .position(mInterpB)).setTitle("interp B");
//            }

                PolylineOptions pathOptions = new PolylineOptions()
                        .width(10)
                        .color(Color.MAGENTA)
                        .add(mInterpA)
                        .add(mInterpB);
                if (mPathLine != null) {
                    mPathLine.setVisible(true);
                    List<LatLng> newPoints = mPathLine.getPoints();
                    newPoints.clear();
                    newPoints.add(mInterpA);
                    newPoints.add(mInterpB);
                    mPathLine.setPoints(newPoints);
                    mMarkerA.setPosition(mInterpA);
                    mMarkerB.setPosition(mInterpB);
                } else {
                    mPathLine = mMap.addPolyline(pathOptions);
                }
                if (IS_DEV) {
                    TextView locationB = (TextView) findViewById(R.id.text_point_B);
                    locationB.setText(mLocationB.getLatitude() + " " + mLocationB.getLongitude());
                }
            }
        }
    }

    public void onClickButtonPrev(View view) {
        if (mMap != null && mInterpA != null && mInterpB != null) {
            double heading = SphericalUtil.computeHeading(mInterpA, mInterpB);
            mInterpA = SphericalUtil.computeOffset(mInterpA, mShiftDist, heading + 90);
            mInterpB = SphericalUtil.computeOffset(mInterpB, mShiftDist, heading + 90);
            List<LatLng> newPoints = mPathLine.getPoints();
            newPoints.clear();
            newPoints.add(mInterpA);
            newPoints.add(mInterpB);
            mPathLine.setPoints(newPoints);
            mMarkerA.setPosition(mInterpA);
            mMarkerB.setPosition(mInterpB);
            int trackDist = (int) Math.round(this.getTrackDist(mInterpA, mInterpB, mLocationCurrent));
            mTextTrackDist.setText(Integer.toString((int) Math.round(MainActivity.toFeet(trackDist))));
            if (trackDist > 0) {
                mImageTrackDistDir.setImageDrawable(mDrawableRight);
            } else {
                mImageTrackDistDir.setImageDrawable(mDrawableLeft);
            }
            mPassNumber--;
            mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
        }
    }

    public void onClickButtonNext(View view) {
        if (mMap != null && mInterpA != null && mInterpB != null) {
            double heading = SphericalUtil.computeHeading(mInterpA, mInterpB);
            mInterpA = SphericalUtil.computeOffset(mInterpA, mShiftDist, heading - 90);
            mInterpB = SphericalUtil.computeOffset(mInterpB, mShiftDist, heading - 90);
            List<LatLng> newPoints = mPathLine.getPoints();
            newPoints.clear();
            newPoints.add(mInterpA);
            newPoints.add(mInterpB);
            mPathLine.setPoints(newPoints);
            mMarkerA.setPosition(mInterpA);
            mMarkerB.setPosition(mInterpB);
            int trackDist = (int) Math.round(this.getTrackDist(mInterpA, mInterpB, mLocationCurrent));
            mTextTrackDist.setText(Integer.toString((int) Math.round(MainActivity.toFeet(trackDist))));
            if (trackDist > 0) {
                mImageTrackDistDir.setImageDrawable(mDrawableRight);
            } else {
                mImageTrackDistDir.setImageDrawable(mDrawableLeft);
            }
            mPassNumber++;
            mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
        }
    }

    public void onClickButtonToggleSlider(View view) {
        SeekBar distSlider = (SeekBar) findViewById(R.id.seekBar_slider);
        ViewGroup.LayoutParams params = distSlider.getLayoutParams();
        if (params.height == 0){
            params.height = 70;
        } else {
            params.height = 0;
        }
        distSlider.setLayoutParams(params);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (mFlightMarkers.contains(marker)){
            if (mFlightLine != null){
                mFlightLine.remove();
            }
            PolylineOptions pathOptions = new PolylineOptions()
                    .width(10)
                    .color(Color.CYAN)
                    .add(mCurrentLatLng)
                    .add(marker.getPosition());
            mFlightLine = mMap.addPolyline(pathOptions);
            mDestinationMarker = marker;
            mToggleFlightLine.setChecked(true);
            mIsFlightLineVis = true;
        }
        return true;
    }


    @Override
    public void onConnected(Bundle dataBundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, location.toString());
        mLocationCurrent = location;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        mCurrentLatLng = new LatLng(lat, lng);
        if (IS_DEV) {
            mTextCurrentLocation.setText(lat + ", " + lng);
        }
        if (mInterpA != null && mInterpB != null && mCurrentLatLng != null) {
            int trackDist = (int) Math.round(this.getTrackDist(mInterpA, mInterpB, mLocationCurrent));
            mTextTrackDist.setText(Integer.toString((int) Math.round(MainActivity.toFeet(trackDist))));
            if (trackDist > 0) {
                mImageTrackDistDir.setImageDrawable(mDrawableRight);
            } else {
                mImageTrackDistDir.setImageDrawable(mDrawableLeft);
            }
        }
        if (mIsFollowing) {
            CameraPosition cameraPosition;
            float zoom = mMap.getCameraPosition().zoom;
            if (mIsRotating) {
                cameraPosition = new CameraPosition.Builder()
                        .target(mCurrentLatLng)
                        .zoom(zoom)
                        .bearing(location.getBearing())
                        .build();
            } else {
                cameraPosition = new CameraPosition.Builder()
                        .target(mCurrentLatLng)
                        .zoom(zoom)
                        .build();
            }
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        if (mFlightLine != null) {
            List<LatLng> points = new ArrayList<LatLng>();
            points.add(mCurrentLatLng);
            points.add(mDestinationMarker.getPosition());
            mFlightLine.setPoints(points);
        }
    }


}

