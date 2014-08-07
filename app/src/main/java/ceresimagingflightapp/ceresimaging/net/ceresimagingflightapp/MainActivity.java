package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.DialogInterface.OnClickListener;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "FlightApp";
    private static final String SERVICE_URL = "http://huaruiwu.github.io/ceresGeoApp/flights/";
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final int UPDATE_INTERVAL = 100;
    private static final int FASTEST_INTERVAL = 100;
    private static final boolean IS_DEV = false;
    private static final int mREAD_TIMEOUT = 10000;
    private GoogleMap mMap;
    LocationRequest mLocationRequest;
    LocationClient mLocationClient;
    private int mScreenWidth;
    private int mScreenHeight;

    private LatLng mCurrentLatLng;
    private Location mLocationCurrent;
    private Location mLocationPrev;
    private double mCurrentSpeed;
    private Marker mCurrentMarker;
    private Location mLocationA;
    private Location mLocationB;
    private LatLng mInterpA;
    private LatLng mInterpB;
    private Marker mMarkerA;
    private Marker mMarkerB;
    private Polyline mPathLine;
    private int mPathDir = 1;
    private int mShiftDist = (int) Math.round(MainActivity.toMeters(700));
    private int mPassNumber;
    private List<Marker> mFlightMarkers = new ArrayList<Marker>();
    private List<Polygon> mFlightPolygons = new ArrayList<Polygon>();
    private Polyline mFlightLine;
    private Marker mDestinationMarker;

    private boolean mIsFollowing = false;
    private boolean mIsRotating = false;
    private boolean mIsFlightLineVis = true;
    private boolean mIsExpanded = false;
    private boolean mIsLocked = false;
    private double mGamma = 0.98;
    private long mPrevBtnClickTime = SystemClock.elapsedRealtime();
    private long mNextBtnClickTime = SystemClock.elapsedRealtime();

    private AlertDialog mGetLocationAlert;
    private AlertDialog mRetrievingJsonAlert;
    private Dialog mDialogFlightSelect;
    private Dialog mDialogFlightConfirm;
    private boolean mFlightSelectConfirmed = false;
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
    private LinearLayout mLayoutArrow;
    private LinearLayout mInformationBox;
    private TextView mTextDistToField;
    private TextView mTextBrngToField;
    private TextView mTextTimeToField;
    private TextView mTextFieldAltitude;
    private Switch mSwitchLock;
    private View mDistLineIndicatorLeft;
    private View mDistLineIndicatorRight;

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
        LatLng center = new LatLng(latitude/totalPoints, longitude/totalPoints);
        return center;
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
        mTextDistToField = (TextView) findViewById(R.id.text_dist_to_field);
        mTextBrngToField = (TextView) findViewById(R.id.text_brng_to_field);
        mTextTimeToField = (TextView) findViewById(R.id.text_time_to_field);
        mTextFieldAltitude = (TextView) findViewById(R.id.text_field_altitude);
        mImageTrackDistDir = (ImageView) findViewById(R.id.image_trackDist_direction);
        mDrawableLeft = getResources().getDrawable(R.drawable.ic_action_back);
        mDrawableRight = getResources().getDrawable(R.drawable.ic_action_forward);
        ColorFilter filter = new LightingColorFilter(Color.RED, Color.RED);
        mLayoutArrow = (LinearLayout) findViewById(R.id.layout_arrow);
        mInformationBox = (LinearLayout) findViewById(R.id.information_box);
        mDrawableLeft.setColorFilter(filter);
        mDrawableRight.setColorFilter(filter);
        mImageTrackDistDir.setImageDrawable(mDrawableLeft);
        mButtonToggleSeekBar = (Button) findViewById(R.id.button_toggle_slider);
        mDistLineIndicatorLeft = findViewById(R.id.dist_indicator_line_left);
        mDistLineIndicatorRight = findViewById(R.id.dist_indicator_line_right);
        mSeekBarSlider = (SeekBar) findViewById(R.id.seekBar_slider);
        mSeekBarSlider.setMax(20);
        mSeekBarSlider.setProgress(14);
        mSeekBarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    int meters = (int) Math.round(MainActivity.toMeters(i * 50.0));
                    mButtonToggleSeekBar.setText(Integer.toString(i*50));
                    mShiftDist = meters;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mSwitchLock = (Switch) findViewById(R.id.switch_lock);
        mSwitchLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mIsLocked = b;
            }
        });

        getScreenSize();

        if (checkPlayServices()) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setTiltGesturesEnabled(false);
//            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(this);
            mMap.setOnMapClickListener(this);
            mGetLocationAlert = new AlertDialog.Builder(this)
                    .setTitle("retrieving location")
                    .setMessage("Please Wait...")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.no, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();

            mGetLocationAlert.show();

            mRetrievingJsonAlert = new AlertDialog.Builder(this)
                    .setTitle("Retrieving flight plan")
                    .setMessage("Please Wait...")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .create();

            mGetLocationAlert.show();
        } else {
            mLayoutArrow.setVisibility(View.INVISIBLE);
            mToggleFlightLine.setVisibility(View.INVISIBLE);
            mToggleCurrentLocation.setVisibility(View.INVISIBLE);
            ToggleButton toggleRotation = (ToggleButton) findViewById(R.id.toggle_rotation);
            toggleRotation.setVisibility(View.INVISIBLE);
        }
        initGeolocation();
        try {
            loadFlightPlan();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    @Override
    public void onBackPressed() {
        final Activity activity = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.on_back_button_title);
        builder.setMessage(R.string.on_back_button_message);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.finish();
            }
        });
        builder.setNegativeButton(R.string.no, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
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

    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
    }

    public void displayTrackDist(double trackDist) {
        if (trackDist > 0) {
            mImageTrackDistDir.setImageDrawable(mDrawableLeft);
            mTextTrackDist.setText(Integer.toString((int) Math.abs(Math.round(MainActivity.toFeet(trackDist))))+'L');
        } else {
            mImageTrackDistDir.setImageDrawable(mDrawableRight);
            mTextTrackDist.setText(Integer.toString((int) Math.abs(Math.round(MainActivity.toFeet(trackDist))))+'R');
        }
    }

    public void adjustLineIndicator(double trackDist) {
        getScreenSize();
        int MAX_WIDTH = mScreenWidth/2 - 10;

        double dist = MainActivity.toFeet(trackDist);
        String green = "#298200";
        String red = "#f20000";
        ViewGroup.LayoutParams layoutLeft =  mDistLineIndicatorLeft.getLayoutParams();
        ViewGroup.LayoutParams layoutRight =  mDistLineIndicatorRight.getLayoutParams();
        if (dist > 50 || dist < -50) {
            mDistLineIndicatorLeft.setBackgroundColor(Color.parseColor(red));
            mDistLineIndicatorRight.setBackgroundColor(Color.parseColor(red));
        } else {
            mDistLineIndicatorLeft.setBackgroundColor(Color.parseColor(green));
            mDistLineIndicatorRight.setBackgroundColor(Color.parseColor(green));
        }
        if (dist > 0) {
            layoutLeft.width = (int) Math.round((dist/100) * MAX_WIDTH);
            mDistLineIndicatorLeft.setVisibility(View.VISIBLE);
            mDistLineIndicatorRight.setVisibility(View.INVISIBLE);
        } else if (dist < 0) {
            layoutRight.width = (int) Math.round((Math.abs(dist)/100) * MAX_WIDTH);
            mDistLineIndicatorLeft.setVisibility(View.INVISIBLE);
            mDistLineIndicatorRight.setVisibility(View.VISIBLE);
        } else {
            mDistLineIndicatorRight.setVisibility(View.INVISIBLE);
            mDistLineIndicatorLeft.setVisibility(View.INVISIBLE);
        }
        mDistLineIndicatorLeft.setLayoutParams(layoutLeft);
        mDistLineIndicatorRight.setLayoutParams(layoutRight);

    }

    private Location filterPosition(Location current, Location prev, final double GAMMA) {
        double lat = current.getLatitude();
        double lng = current.getLongitude();
        double prevLat = prev.getLatitude();
        double prevLng = prev.getLongitude();
        double newLat = lat*GAMMA + prevLat*(1 - GAMMA);
        double newLng = lng*GAMMA + prevLng*(1 - GAMMA);
        current.setLatitude(newLat);
        current.setLongitude(newLng);
        return current;
    }

    protected void retrieveAndAddFlightPlan(String flight) throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service
            URL url = new URL(SERVICE_URL + flight + ".json");
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            conn.setReadTimeout(mREAD_TIMEOUT);
            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[10000];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Error timed out", e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Could not connect", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // create markers and polygons
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
        if (mFlightMarkers != null) {
            for (Marker marker : mFlightMarkers) {
                marker.remove();
            }
            mFlightMarkers.clear();
        }
        if (mFlightPolygons != null) {
            for (Polygon polygon : mFlightPolygons) {
                polygon.remove();
            }
            mFlightPolygons.clear();
        }
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject geometry = feature.getJSONObject("geometry");
            String name = feature.getJSONObject("properties").getString("Name");
            String altitude = feature.getJSONObject("properties").getString("Description");

            JSONArray coords = geometry.getJSONArray("coordinates");
            // if feature is a point
            if (geometry.getString("type").equals("Point")) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(coords.getDouble(1), coords.getDouble(0))));
                marker.setTitle(name);
                marker.setSnippet("alt: " + altitude);
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
                mFlightPolygons.add(polygon);
            }
        }
        saveFlightPlan(json);
    }

    private void saveFlightPlan(String flightPlan) {
        SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("flightPlan", flightPlan);
        prefsEditor.commit();
    }

    private void loadFlightPlan() throws JSONException {
        SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
        String flightPlan = mPrefs.getString("flightPlan", null);
        if (flightPlan != null) {
            createMarkersFromJson(flightPlan);
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
                mMap.moveCamera(CameraUpdateFactory
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
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            } else {
                float zoom = mMap.getCameraPosition().zoom;
                cameraPosition = new CameraPosition.Builder()
                        .target(mCurrentLatLng)
                        .zoom(zoom).bearing(0).build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

    public void onToggleMarkers(View view) {
        boolean isChecked = ((ToggleButton) view).isChecked();
        if (mMap != null && mFlightMarkers != null) {
            for (Marker marker : mFlightMarkers) {
                marker.setVisible(isChecked);
            }
        }
    }

    public void onClickButtonSelectFlight(View view) {
        mFlightSelectConfirmed = false;

        if (mDialogFlightConfirm == null) {
            // confirm
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("select this flight plan?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mFlightSelectConfirmed = true;
                        }
                    })
                    .setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mDialogFlightSelect.dismiss();
                        }
                    });
            mDialogFlightConfirm = builder.create();
        }

        if (mDialogFlightSelect == null) {
            // flight select
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Flight Plan");
            final Toast toast = Toast.makeText(this, "flight plan loaded", Toast.LENGTH_SHORT);
            final Toast failed_toast = Toast.makeText(this, "connection failed", Toast.LENGTH_SHORT);
            final ListView flightList = new ListView(this);
            final String[] flightArray = new String[] { "flight1", "flight2" };
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, flightArray);
            flightList.setAdapter(modeAdapter);

            flightList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    mDialogFlightConfirm.show();
                    mDialogFlightConfirm.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (mFlightSelectConfirmed) {
                                mRetrievingJsonAlert.show();
                                final String flight = flightArray[i];
                                new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            retrieveAndAddFlightPlan(flight);
                                            toast.show();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Cannot retrieve data", e);
                                            failed_toast.show();
                                        } finally {
                                            mRetrievingJsonAlert.dismiss();
                                            mDialogFlightSelect.dismiss();
                                        }
                                    }
                                }).start();
                            }
                        }
                    });
                }
            });
            builder.setView(flightList);
            mDialogFlightSelect = builder.create();
        }

        mDialogFlightSelect.show();
    }

    public void onClickButtonA(View view) {
        this.onClickButtonA(view, null);
    }
    public void onClickButtonA(View view, LatLng point) {
        if (mMap != null && !mIsLocked) {
            if (point != null) {
                mLocationA = new Location("");
                mLocationA.setLongitude(point.longitude);
                mLocationA.setLatitude(point.latitude);
            } else {
                mLocationA = mLocationClient.getLastLocation();
            }
            mPassNumber = 0;
            view.setBackgroundColor(Color.RED);
            Button button_B = (Button) findViewById(R.id.button_B);
            button_B.setBackgroundColor(Color.LTGRAY);
            if (mPathLine != null) {
                mPathLine.setVisible(false);
            }
        }
    }

    public void onClickButtonB(View view) {
        this.onClickButtonB(view, null);
    }
    public void onClickButtonB(View view, LatLng point) {
        if (mMap != null && !mIsLocked) {
            if (mLocationA != null){
                mPassNumber = 0;
                mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
                if (point != null) {
                    mLocationB = new Location("");
                    mLocationB.setLongitude(point.longitude);
                    mLocationB.setLatitude(point.latitude);
                } else {
                    mLocationB = mLocationClient.getLastLocation();
                }
                view.setBackgroundColor(Color.RED);
                LatLng pointA = new LatLng(mLocationA.getLatitude(), mLocationA.getLongitude());
                LatLng pointB = new LatLng(mLocationB.getLatitude(), mLocationB.getLongitude());
                mInterpA = SphericalUtil.computeOffset(pointB, 4000, SphericalUtil.computeHeading(pointA, pointB));
                mInterpB = SphericalUtil.computeOffset(pointA, 4000, SphericalUtil.computeHeading(pointB, pointA));

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
                    mMarkerA.setVisible(false);
                    mMarkerB.setVisible(false);
                }

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
            }
        }
    }

    public void onClickButtonPrev(View view) {
        if (SystemClock.elapsedRealtime() - mPrevBtnClickTime < 2000) {
            return;
        }
        mPrevBtnClickTime = SystemClock.elapsedRealtime();
        if (mMap != null && mInterpA != null && mInterpB != null) {
            double heading = SphericalUtil.computeHeading(mInterpA, mInterpB);
            if (heading > -90 && heading < 90) {
                heading -= 90*mPathDir;
            } else {
                heading += 90*mPathDir;
            }
            mInterpA = SphericalUtil.computeOffset(mInterpA, mShiftDist, heading);
            mInterpB = SphericalUtil.computeOffset(mInterpB, mShiftDist, heading);
            List<LatLng> newPoints = mPathLine.getPoints();
            newPoints.clear();
            newPoints.add(mInterpA);
            newPoints.add(mInterpB);
            mPathLine.setPoints(newPoints);
            mMarkerA.setPosition(mInterpA);
            mMarkerB.setPosition(mInterpB);
            double trackDist = this.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
            displayTrackDist(trackDist);
            mPassNumber--;
            mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
        }
    }

    public void onClickButtonNext(View view) {
        if (SystemClock.elapsedRealtime() - mNextBtnClickTime < 2000) {
            return;
        }
        mNextBtnClickTime = SystemClock.elapsedRealtime();
        if (mMap != null && mInterpA != null && mInterpB != null) {
            double heading = SphericalUtil.computeHeading(mInterpA, mInterpB);
            if (heading > -90 && heading < 90) {
                heading += 90*mPathDir;
            } else {
                heading -= 90*mPathDir;
            }
            mInterpA = SphericalUtil.computeOffset(mInterpA, mShiftDist, heading);
            mInterpB = SphericalUtil.computeOffset(mInterpB, mShiftDist, heading);
            List<LatLng> newPoints = mPathLine.getPoints();
            newPoints.clear();
            newPoints.add(mInterpA);
            newPoints.add(mInterpB);
            mPathLine.setPoints(newPoints);
            mMarkerA.setPosition(mInterpA);
            mMarkerB.setPosition(mInterpB);
            double trackDist = this.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
            displayTrackDist(trackDist);
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

    public void onClickButtonGamma(View view) {
        final EditText input = new EditText(this);
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setMessage("enter gamma")
                .setView(input)
                .setPositiveButton("Ok", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mGamma = Double.parseDouble(input.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
    }

    public void onClickLayoutArrow(View view) {
        int imageHeight = mImageTrackDistDir.getHeight();
        ViewGroup.LayoutParams imageParams = mImageTrackDistDir.getLayoutParams();
        if (!mIsExpanded) {
            imageParams.width = imageHeight * 2;
            imageParams.height = imageHeight * 2;
            mImageTrackDistDir.setLayoutParams(imageParams);
            mTextTrackDist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
            mTextDistToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            mTextTimeToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            mTextBrngToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            mTextFieldAltitude.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            mIsExpanded = true;
        } else {
            imageParams.width = imageHeight / 2;
            imageParams.height = imageHeight / 2;
            mImageTrackDistDir.setLayoutParams(imageParams);
            mTextTrackDist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
            mTextDistToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            mTextTimeToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            mTextBrngToField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            mTextFieldAltitude.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            mIsExpanded = false;
        }
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
        return false;
    }

    @Override
    public void onMapClick(LatLng clickLatLng) {
        Location clickLocation = new Location(mLocationCurrent);
        clickLocation.setLongitude(clickLatLng.longitude);
        clickLocation.setLatitude(clickLatLng.latitude);
        for (Polygon polygon : mFlightPolygons) {
            List<LatLng> points = polygon.getPoints();
            if (PolyUtil.isLocationOnEdge(clickLatLng, polygon.getPoints(), false, 1000)) {
                LatLng pointA = null;
                LatLng pointB = null;
                double minDist = 1000;
                for (int i=0; i < points.size() - 1; i++){
                    double dist = Math.abs(MainActivity.getTrackDist(points.get(i), points.get(i + 1), clickLocation));
                    List<LatLng> line = new ArrayList<LatLng>();
                    line.add(points.get(i));
                    line.add(points.get(i+1));
                    if (dist < minDist && PolyUtil.isLocationOnPath(clickLatLng, line, false, 500 )) {
                        minDist = dist;
                        pointA = points.get(i);
                        pointB = points.get(i+1);
                    }
                }
                double dist = Math.abs(MainActivity.getTrackDist(points.get(0), points.get(points.size() - 1), clickLocation));
                List<LatLng> line = new ArrayList<LatLng>();
                line.add(points.get(0));
                line.add(points.get(points.size()-1));
                if (dist < minDist && PolyUtil.isLocationOnPath(clickLatLng, line, false, 500 )) {
                    minDist = dist;
                    pointA = points.get(0);
                    pointB = points.get(points.size()-1);
                }
                if (pointA != null && pointB != null) {
                    onClickButtonA(findViewById(R.id.button_A), pointA);
                    onClickButtonB(findViewById(R.id.button_B), pointB);
                    // check if closer to polygon
                    if (mInterpA != null && mInterpB != null) {
                        double heading = SphericalUtil.computeHeading(mInterpA, mInterpB);
                        if (heading > -90 && heading < 90) {
                            heading += 90*mPathDir;
                        } else {
                            heading -= 90*mPathDir;
                        }
                        LatLng newPointA = SphericalUtil.computeOffset(mInterpA, mShiftDist, heading);
                        LatLng newPointB = SphericalUtil.computeOffset(mInterpB, mShiftDist, heading);
                        // check dist to center of polygon
                        LatLng polygonCenter = MainActivity.getPolyCenter(points);
                        Location center = new Location("");
                        center.setLongitude(polygonCenter.longitude);
                        center.setLatitude(polygonCenter.latitude);
                        double centerToPoint = MainActivity.getTrackDist(pointA, pointB, center);
                        double centerToNewPoint = MainActivity.getTrackDist(newPointA, newPointB, center);
                        if (Math.abs(centerToNewPoint) > Math.abs(centerToPoint)) {
                            mPathDir = mPathDir * -1;
                        }
                    }
                }
            } else {
            }
        }
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
        if (location.getSpeed() < 89.408) {
            TextView textBearing = (TextView) findViewById(R.id.text_bearing);
            textBearing.setText(Float.toString(location.getBearing()));
            mLocationCurrent = location;
            mCurrentSpeed = location.getSpeed();
            if (mLocationCurrent != null) {
                mGetLocationAlert.dismiss();
                if (mLocationPrev == null) {
                    mLocationPrev = location;
                }
                // filter
                mLocationCurrent = filterPosition(location, mLocationPrev, mGamma);

                double lat = mLocationCurrent.getLatitude();
                double lng = mLocationCurrent.getLongitude();
                LatLng current = new LatLng(lat, lng);
                LatLng prev = new LatLng(mLocationPrev.getLatitude(), mLocationPrev.getLongitude());
                location.setBearing((float) SphericalUtil.computeHeading(prev, current));
                mLocationPrev = mLocationCurrent;
                if (mCurrentMarker == null) {
                    Drawable arrow = getResources().getDrawable(R.drawable.location_arrow);
                    Bitmap arrowBm = ((BitmapDrawable) arrow).getBitmap();
                    arrowBm = arrowBm.createScaledBitmap(arrowBm, arrowBm.getWidth()/3, arrowBm.getHeight()/3, true);
                    mCurrentMarker = mMap.addMarker(new MarkerOptions()
                            .position(current)
                            .flat(true)
                            .anchor((float) 0.5, (float) 0.8)
                            .rotation(mLocationCurrent.getBearing())
                            .icon(BitmapDescriptorFactory.fromBitmap(arrowBm)));
                } else {
                    mCurrentMarker.setPosition(current);
                    mCurrentMarker.setRotation(mLocationCurrent.getBearing());
                }
            }
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            mCurrentLatLng = new LatLng(lat, lng);
            if (IS_DEV) {
                mTextCurrentLocation.setText((double)Math.round(lat*1000)/1000 + ", " + (double)Math.round(lng*1000)/1000);
            }
            if (mInterpA != null && mInterpB != null && mCurrentLatLng != null) {
                double trackDist = this.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
                displayTrackDist(trackDist);
                adjustLineIndicator(trackDist);
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
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            if (mFlightLine != null) {
                List<LatLng> points = new ArrayList<LatLng>();
                points.add(mCurrentLatLng);
                points.add(mDestinationMarker.getPosition());
                mFlightLine.setPoints(points);

                // display travel info
                double dist = SphericalUtil.computeDistanceBetween(mCurrentLatLng, mDestinationMarker.getPosition());
                double brng = SphericalUtil.computeHeading(mCurrentLatLng, mDestinationMarker.getPosition());
                double time = dist / mCurrentSpeed;
                long day = (int) TimeUnit.SECONDS.toDays((long) time);
                long hours = TimeUnit.SECONDS.toHours((long) time) - (day * 24);
                long minutes = TimeUnit.SECONDS.toMinutes((long) time) - (TimeUnit.SECONDS.toHours((long) time)* 60);
                long second = TimeUnit.SECONDS.toSeconds((long) time) - (TimeUnit.SECONDS.toMinutes((long) time) *60);
                if (hours > 100) { hours = 0; minutes = 0; }
                String altitude = mDestinationMarker.getSnippet();
                dist = MainActivity.toMiles(dist);
                mTextDistToField.setText(Integer.toString((int)Math.round(dist)) + "miles");
                mTextBrngToField.setText(Integer.toString((int)Math.round(brng)) + "\u00B0");
                mTextTimeToField.setText(Long.toString(hours) + "h " + Long.toString(minutes) + "m" );
                mTextFieldAltitude.setText(altitude.substring(5) + "ft ASL");
            }
        }
    }
}