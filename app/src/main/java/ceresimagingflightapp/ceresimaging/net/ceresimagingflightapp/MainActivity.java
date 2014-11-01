package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Camera;
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
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
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
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS.GpsService;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS.StartingLocations;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.GPS.TabletGPSDataEvent;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardConnectionEvent;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardConnectionService;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardDataEvent;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardStatus;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.UI.BreadCrumbs;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.utils.GeoUtils;

import static android.content.DialogInterface.OnClickListener;

public class MainActivity extends Activity implements
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "FlightApp";
    private static final String SERVICE_URL = "http://huaruiwu.github.io/ceresGeoApp/flights/";
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    // moved to GPS Service
//    private static final int UPDATE_INTERVAL = 50;
//    private static final int FASTEST_INTERVAL = 50;
    private static final boolean IS_DEV = false;
    private static final int mREAD_TIMEOUT = 10000;
    private static HashMap<LatLng, Integer> STARTING_LOCATIONS = new HashMap<LatLng, Integer>();
    private GoogleMap mMap;
    // moved to GPS Service
//    LocationRequest mLocationRequest;
//    LocationClient mLocationClient;
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
    private Polygon mCurrentFieldPolygon = null;
    private int mPathDir = 1;
    private int mShiftDist = (int) Math.round(GeoUtils.toMeters(700));
    private int mPassNumber;
    private List<Marker> mFlightMarkers = new ArrayList<Marker>();
    private List<Polygon> mFlightPolygons = new ArrayList<Polygon>();
    private Map<Marker, Boolean> mMarkerDoneMap = new HashMap<Marker, Boolean>();
    private Polyline mFlightLine;
    private Marker mDestinationMarker;
    private Marker mAirportMarker;
    private int mNumberOfFieldsRemaining;
    private long mExitFieldTime = SystemClock.elapsedRealtime();
    private long mEnterFieldTime = SystemClock.elapsedRealtime();

    private boolean mIsFollowing = false;
    private boolean mIsRotating = false;
    private boolean mIsFlightLineVis = true;
    private boolean mIsExpanded = false;
    private boolean mIsLocked = false;
    private boolean mIsInField = false;
    private double mGamma = 0.98;
    private long mPrevBtnClickTime = SystemClock.elapsedRealtime();
    private long mNextBtnClickTime = SystemClock.elapsedRealtime();

    private AlertDialog mGetLocationAlert;
    private AlertDialog mRetrievingJsonAlert;
    private Dialog mDialogFlightSelect;
    private Dialog mDialogFlightConfirm;
    private Dialog mDialogDoneField;
    private Dialog mDialogNextField;
    private boolean mFlightSelectConfirmed = false;
    private TextView mTextCurrentLocation;
    private TextView mTextTrackDist;
    private TextView mTextPassNumber;
    private ToggleButton mToggleCurrentLocation;
    private ToggleButton mToggleFlightLine;
    private SeekBar mSeekBarSlider;
    private Button mButtonToggleSeekBar;
    private Button mButtonSBC;
    private Button mButtonNext;
    private Button mButtonPrev;
    private ImageView mImageTrackDistDirLeft;
    private ImageView mImageTrackDistDirRight;
    private Drawable mDrawableLeft;
    private Drawable mDrawableRight;
    private Drawable mDrawableBreadCrumbDot;
    private LinearLayout mLayoutArrow;
    private TextView mTextDistToField;
    private TextView mTextBrngToField;
    private TextView mTextTimeToField;
    private TextView mTextFieldAltitude;
    private TextView mTextFieldsRemaining;
    private TextView mTextFieldsPercentage;
    private TextView mTextDistBetweenPass;
    private TextView mTextTimeOfTurn;
    private Switch mSwitchLock;
    private View mDistLineIndicatorLeft;
    private View mDistLineIndicatorRight;
    private View mDistLineIndicatorLeftStatic;
    private View mDistLineIndicatorRightStatic;
    private Bitmap mArrowBm;
    private Bitmap mArrowBmGreen;


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
        mTextFieldsRemaining = (TextView) findViewById(R.id.text_fields_remaining);
        mTextFieldsPercentage = (TextView) findViewById(R.id.text_fields_percentage);
        mTextDistBetweenPass = (TextView) findViewById(R.id.text_dist_between_pass);
        mTextTimeOfTurn = (TextView) findViewById(R.id.text_time_of_turn);
        mImageTrackDistDirLeft = (ImageView) findViewById(R.id.image_trackDist_left);
        mImageTrackDistDirRight = (ImageView) findViewById(R.id.image_trackDist_right);
        mDrawableLeft = getResources().getDrawable(R.drawable.ic_action_back);
        mDrawableRight = getResources().getDrawable(R.drawable.ic_action_forward);
        mDrawableBreadCrumbDot = getResources().getDrawable(R.drawable.reddot);
        ColorFilter filter = new LightingColorFilter(Color.RED, Color.RED);
        mLayoutArrow = (LinearLayout) findViewById(R.id.layout_arrow);
        mDrawableLeft.setColorFilter(filter);
        mDrawableRight.setColorFilter(filter);
        mImageTrackDistDirLeft.setImageDrawable(mDrawableLeft);
        mImageTrackDistDirRight.setImageDrawable(mDrawableRight);
        mButtonToggleSeekBar = (Button) findViewById(R.id.button_toggle_slider);
        mButtonSBC = (Button) findViewById(R.id.button_SBC_status);
        mButtonNext = (Button) findViewById(R.id.button_next);
        mButtonPrev = (Button) findViewById(R.id.button_prev);

        // starting location zoom fields
        STARTING_LOCATIONS.put(new LatLng(36.398487, -119.621887), 7);
        STARTING_LOCATIONS.put(new LatLng(-34.183324, 142.080051), 8);

        // adjust buttons width
        ViewGroup.LayoutParams layoutButtonNext = mButtonNext.getLayoutParams();
        ViewGroup.LayoutParams layoutButtonPrev = mButtonPrev.getLayoutParams();
        getScreenSize();
        double MAX_WIDTH = mScreenWidth/2 - 10;
        layoutButtonNext.width = (int) Math.round(MAX_WIDTH/2);
        layoutButtonPrev.width = (int) Math.round(MAX_WIDTH/2);

        mDistLineIndicatorLeft = findViewById(R.id.dist_indicator_line_left);
        mDistLineIndicatorRight = findViewById(R.id.dist_indicator_line_right);
        mDistLineIndicatorLeftStatic = findViewById(R.id.dist_left_indicator_static);
        mDistLineIndicatorRightStatic = findViewById(R.id.dist_right_indicator_static);
        mSeekBarSlider = (SeekBar) findViewById(R.id.seekBar_slider);
        mSeekBarSlider.setMax(20);
        mSeekBarSlider.setProgress(14);
        mSeekBarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    int meters = (int) Math.round(GeoUtils.toMeters(i * 50.0));
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
            mMap.getUiSettings().setZoomControlsEnabled(false);
//            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(this);
            mMap.setOnMapClickListener(this);

            CameraPosition cameraPosition;
                cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(36.398487, -119.621887))
                        .zoom(7)
                        .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            mGetLocationAlert = new AlertDialog.Builder(this)
                    .setTitle("retrieving location")
                    .setMessage(R.string.get_location_alert)
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
        // start bread crumbs
        BreadCrumbs.startBreadCrumb(mMap);

        // moved to gps service
//        initGeolocation();

        // start SBC service and register event bus
        startSBCService();
        startGPSService();
        SingleBoardConnectionService.getEventBus().register(this);
        GpsService.getMainThreadBus().register(this);
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
        // moved to GPS service
//        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        // moved to GPS service
//        if (mLocationClient.isConnected()) {
//            mLocationClient.removeLocationUpdates(this);
//        }
//        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsFollowing = mToggleCurrentLocation.isChecked();
    }

    @Override
    protected void onDestroy() {
        stopSBCService();
        stopGPSService();
        super.onDestroy();
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

    // moved to gps service
//    private void initGeolocation() {
//        mLocationRequest = LocationRequest.create();
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        mLocationRequest.setInterval(UPDATE_INTERVAL);
//        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
//        mLocationClient = new LocationClient(this, this, this);
//    }

    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
    }

    public void displayTrackDist(double trackDist) {
        if (trackDist > 0) {
//            mImageTrackDistDirLeft.setImageDrawable(mDrawableLeft);
            mTextTrackDist.setText(Integer.toString((int) Math.abs(Math.round(GeoUtils.toFeet(trackDist)))) + 'L');
            mImageTrackDistDirLeft.setVisibility(View.VISIBLE);
            mImageTrackDistDirRight.setVisibility(View.INVISIBLE);
        } else {
//            mImageTrackDistDirLeft.setImageDrawable(mDrawableRight);
            mTextTrackDist.setText(Integer.toString((int) Math.abs(Math.round(GeoUtils.toFeet(trackDist)))) + 'R');
            mImageTrackDistDirRight.setVisibility(View.VISIBLE);
            mImageTrackDistDirLeft.setVisibility(View.INVISIBLE);
        }
    }

    public void adjustLineIndicator(double trackDist) {
        getScreenSize();
        double MAX_WIDTH = mScreenWidth/2 - 10;

        double dist = GeoUtils.toFeet(trackDist);
        String green = "#298200";
        String red = "#f20000";
        ViewGroup.LayoutParams layoutLeft =  mDistLineIndicatorLeft.getLayoutParams();
        ViewGroup.LayoutParams layoutRight =  mDistLineIndicatorRight.getLayoutParams();
        ViewGroup.MarginLayoutParams layoutLeftStatic =  (ViewGroup.MarginLayoutParams) mDistLineIndicatorLeftStatic.getLayoutParams();
        ViewGroup.MarginLayoutParams layoutRightStatic =  (ViewGroup.MarginLayoutParams) mDistLineIndicatorRightStatic.getLayoutParams();
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
        layoutLeftStatic.rightMargin = (int) Math.round(MAX_WIDTH/2);
        layoutRightStatic.leftMargin = (int) Math.round(MAX_WIDTH/2);
        mDistLineIndicatorLeft.setLayoutParams(layoutLeft);
        mDistLineIndicatorRight.setLayoutParams(layoutRight);
        mDistLineIndicatorLeftStatic.setLayoutParams(layoutLeftStatic);
        mDistLineIndicatorRightStatic.setLayoutParams(layoutRightStatic);
    }

    // draw flightline to center of edge
    private void drawFlightLineToEdge() {
        if (mCurrentFieldPolygon != null && mFlightMarkers != null) {
            Marker marker = GeoUtils.getPolygonMarker(mCurrentFieldPolygon, mFlightMarkers);
            if (mFlightMarkers.contains(marker)) {
                double distanceBetween = SphericalUtil.computeDistanceBetween(mInterpA, mInterpB);
                double headingBetween = SphericalUtil.computeHeading(mInterpA, mInterpB);
                LatLng pointFlightLineMid = SphericalUtil.computeOffsetOrigin(mInterpB, distanceBetween / 2, headingBetween);
                if (distanceBetween > 0) {
                    Marker newDestMarker = mMap.addMarker(new MarkerOptions()
                            .position(pointFlightLineMid)
                            .snippet(marker.getSnippet()).visible(false));
                    if (mFlightLine != null) {
                        mFlightLine.remove();
                    }
                    PolylineOptions pathOptions = new PolylineOptions()
                            .width(10)
                            .color(Color.CYAN)
                            .add(mCurrentLatLng)
                            .add(pointFlightLineMid);
                    mDestinationMarker = newDestMarker;
                    mFlightLine = mMap.addPolyline(pathOptions);
                    mToggleFlightLine.setChecked(true);
                    mToggleFlightLine.setBackgroundColor(Color.BLUE);
                    mIsFlightLineVis = true;
                }
            }
        }
    }

    // low pass filter
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
            JSONArray coords = geometry.getJSONArray("coordinates");
            // if feature is a point
            if (geometry.getString("type").equals("Point")) {
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(coords.getDouble(1), coords.getDouble(0))));
                if (!name.equals("Airport")) {
                    String[] description = feature.getJSONObject("properties").getString("Description").split(", ");
                    String altitude = description[0];
                    String distanceBetweenPass = description[1];
                    marker.setSnippet("alt: " + altitude + ", distance between pass: " + distanceBetweenPass);
                    marker.setTitle(name);
                    mFlightMarkers.add(marker);
                    mMarkerDoneMap.put(marker, false);
                } else {
                    marker.setTitle(name);
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    mAirportMarker = marker;
                }
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
        mNumberOfFieldsRemaining = mFlightMarkers.size();
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

    public void startSBCService() {
        Intent i = new Intent(getBaseContext(), SingleBoardConnectionService.class);
        this.startService(i);
    }

    public void stopSBCService() {
        Intent i = new Intent(getBaseContext(), SingleBoardConnectionService.class);
        this.stopService(i);
    }

    public void startGPSService() {
        Intent i = new Intent(getBaseContext(), GpsService.class);
        this.startService(i);
    }

    public void stopGPSService() {
        Intent i = new Intent(getBaseContext(), GpsService.class);
        this.stopService(i);
    }

    public void onToggleCurrentLocation(View view) {
        if (mMap != null) {
            mIsFollowing = ((ToggleButton) view).isChecked();
            if (mIsFollowing) {
                view.setBackgroundColor(Color.BLUE);
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
                view.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    public void onToggleRotation(View view) {
        if (mMap != null) {
            mIsRotating = ((ToggleButton) view).isChecked();
            CameraPosition cameraPosition;
            if (mIsRotating) {
                view.setBackgroundColor(Color.BLUE);
                if (mIsFollowing) {
                    float zoom = mMap.getCameraPosition().zoom;
                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
                            .zoom(zoom).bearing(mLocationCurrent.getBearing()).build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
                mToggleCurrentLocation.setChecked(true);
                mToggleCurrentLocation.callOnClick();
            } else {
                view.setBackgroundColor(Color.LTGRAY);
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
            if (mIsFlightLineVis) {
                view.setBackgroundColor(Color.BLUE);
            } else {
                view.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    public void onToggleMarkers(View view) {
        boolean isChecked = ((ToggleButton) view).isChecked();
        if (isChecked) {
            view.setBackgroundColor(Color.BLUE);
        } else {
            view.setBackgroundColor(Color.LTGRAY);
        }
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
            final String[] flightArray = new String[10];
            for (int i=0; i < 10; i++) {
                flightArray[i] = "flight" + (i+1);
            }
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
//                mLocationA = mLocationClient.getLastLocation();
                mLocationA = mLocationCurrent;
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
//                    mLocationB = mLocationClient.getLastLocation();
                    mLocationB = mLocationCurrent;
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
            double trackDist = GeoUtils.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
            displayTrackDist(trackDist);
            mPassNumber--;
            mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
            // update flightline
            drawFlightLineToEdge();
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
            double trackDist = GeoUtils.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
            displayTrackDist(trackDist);
            mPassNumber++;
            mTextPassNumber.setText("Pass #" + Integer.toString(mPassNumber));
            // update flightline
            drawFlightLineToEdge();
        }
    }

    public void onClickDoneFieldButton(View view) {
        if (mDestinationMarker.getSnippet() == null) {
            return;
        }
        if (mDialogNextField == null) {
            // confirm select next field
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("Go to next closest field?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // get closest field marker to current position and trigger click
                            Marker closest = null;
                            double closestDist = 0;
                            for (Marker marker : mFlightMarkers) {
                                if (!mMarkerDoneMap.get(marker)) {
                                    if (closest == null) {
                                        closest = marker;
                                        closestDist = SphericalUtil.computeDistanceBetween(marker.getPosition(), mCurrentMarker.getPosition());
                                    } else {
                                        double dist = SphericalUtil.computeDistanceBetween(marker.getPosition(), mCurrentMarker.getPosition());
                                        if (dist < closestDist) {
                                            closestDist = dist;
                                            closest = marker;
                                        }
                                    }
                                }
                            }
                            onMarkerClick(closest);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mDialogNextField.dismiss();
                        }
                    });
            mDialogNextField = builder.create();
        }
        if (mDialogDoneField == null) {
            // confirm
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("Are you done?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (Marker marker : mFlightMarkers) {
                                if (mCurrentFieldPolygon != null) {
                                    List<LatLng> polygon = mCurrentFieldPolygon.getPoints();
                                    if (PolyUtil.containsLocation(marker.getPosition(), polygon, false)) {
                                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                        mMarkerDoneMap.put(marker, true);
                                        mNumberOfFieldsRemaining--;
                                        break;
                                    }
                                } else {
                                    mDestinationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                    mMarkerDoneMap.put(mDestinationMarker, true);
                                    mNumberOfFieldsRemaining--;
                                    break;
                                }
                            }
                            if (mNumberOfFieldsRemaining > 0) {
                                mDialogNextField.show();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mDialogDoneField.dismiss();
                        }
                    });
            mDialogDoneField = builder.create();
        }
        if (mCurrentFieldPolygon != null) {
            for (Marker marker : mFlightMarkers) {
                List<LatLng> polygon = mCurrentFieldPolygon.getPoints();
                if (PolyUtil.containsLocation(marker.getPosition(), polygon, false)) {
                    if (!mMarkerDoneMap.get(marker)) {
                        mDialogDoneField.show();
                    }
                }
            }
        } else if (mDestinationMarker != null) {
            if (!mMarkerDoneMap.get(mDestinationMarker)) {
                mDialogDoneField.show();
            }
        }
    }

    public void onClickSBCButton(View view) {
        if (SingleBoardConnectionService.inWarning) {
            SingleBoardConnectionService.inWarning = false;
        }
        Intent intent = new Intent(this, SingleBoardStatusActivity.class);
        startActivity(intent);
    }

    public void onClickButtonToggleSlider(View view) {
        SeekBar distSlider = (SeekBar) findViewById(R.id.seekBar_slider);
        ViewGroup.LayoutParams params = distSlider.getLayoutParams();
        if (params.height == 0){
            params.height = 140;
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

    public void onClickZoomIn(View view) {
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
    }

    public void onClickZoomOut(View view) {
        mMap.animateCamera(CameraUpdateFactory.zoomOut());
    }

    public void onClickLayoutArrow(View view) {
        int imageHeight = mImageTrackDistDirLeft.getHeight();
        ViewGroup.LayoutParams imageParams = mImageTrackDistDirLeft.getLayoutParams();
        if (!mIsExpanded) {
            imageParams.width = imageHeight * 2;
            imageParams.height = imageHeight * 2;
            mImageTrackDistDirLeft.setLayoutParams(imageParams);
            mImageTrackDistDirRight.setLayoutParams(imageParams);
            mTextTrackDist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
            mIsExpanded = true;
        } else {
            imageParams.width = imageHeight / 2;
            imageParams.height = imageHeight / 2;
            mImageTrackDistDirLeft.setLayoutParams(imageParams);
            mImageTrackDistDirRight.setLayoutParams(imageParams);
            mTextTrackDist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
            mIsExpanded = false;
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        if (marker.equals(mCurrentMarker)) {
            return false;
        }
        if (mFlightMarkers.contains(marker) || marker.getTitle().equals("Airport")){
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
            mCurrentFieldPolygon = null;
            mToggleFlightLine.setChecked(true);
            mToggleFlightLine.setBackgroundColor(Color.BLUE);
            mIsFlightLineVis = true;

            // adjust seek bar
            if (!marker.getTitle().equals("Airport")) {
                String[] description = marker.getSnippet().split(", ");
                double distanceBetweenPass = Double.parseDouble(description[1].substring(23));
                mShiftDist = (int) Math.round(GeoUtils.toMeters(distanceBetweenPass));
                int dist = (int) Math.round((distanceBetweenPass/1000)*20);
                mSeekBarSlider.setProgress(dist);
                mButtonToggleSeekBar.setText(description[1].substring(23) + "ft");
            }
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
                    double dist = Math.abs(GeoUtils.getTrackDist(points.get(i), points.get(i + 1), clickLocation));
                    List<LatLng> line = new ArrayList<LatLng>();
                    line.add(points.get(i));
                    line.add(points.get(i+1));
                    if (dist < minDist && PolyUtil.isLocationOnPath(clickLatLng, line, false, 500 )) {
                        minDist = dist;
                        pointA = points.get(i);
                        pointB = points.get(i+1);
                    }
                }
                double dist = Math.abs(GeoUtils.getTrackDist(points.get(0), points.get(points.size() - 1), clickLocation));
                List<LatLng> line = new ArrayList<LatLng>();
                line.add(points.get(0));
                line.add(points.get(points.size()-1));
                if (dist < minDist && PolyUtil.isLocationOnPath(clickLatLng, line, false, 500 )) {
                    minDist = dist;
                    pointA = points.get(0);
                    pointB = points.get(points.size()-1);
                }
                if (pointA != null && pointB != null && SphericalUtil.computeDistanceBetween(pointA, pointB) > 0) {
                    mCurrentFieldPolygon = polygon;
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
                        LatLng polygonCenter = GeoUtils.getPolyCenter(points);

                        Location center = new Location("");
                        center.setLongitude(polygonCenter.longitude);
                        center.setLatitude(polygonCenter.latitude);
                        double centerToPoint = GeoUtils.getTrackDist(pointA, pointB, center);
                        double centerToNewPoint = GeoUtils.getTrackDist(newPointA, newPointB, center);
                        if (Math.abs(centerToNewPoint) > Math.abs(centerToPoint)) {
                            mPathDir = mPathDir * -1;
                        }

                        // adjust seek bar
                        Marker marker = GeoUtils.getPolygonMarker(mCurrentFieldPolygon, mFlightMarkers);
                        String[] description = marker.getSnippet().split(", ");
                        double distanceBetweenPass = Double.parseDouble(description[1].substring(23));
                        mShiftDist = (int) Math.round(GeoUtils.toMeters(distanceBetweenPass));
                        int shiftDist = (int) Math.round((distanceBetweenPass/1000)*20);
                        mSeekBarSlider.setProgress(shiftDist);
                        mButtonToggleSeekBar.setText(description[1].substring(23) + "ft");

                        // draw flightline to center of edge
                        drawFlightLineToEdge();
                    }
                }
            } else {
            }
        }
    }

    // moved to GPS service
//    @Override
//    public void onConnected(Bundle dataBundle) {
//        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
//        mLocationClient.requestLocationUpdates(mLocationRequest, this);
//    }

    // moved to GPS service
//    @Override
//    public void onDisconnected() {
//        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
//    }

    // moved GPS service
//    @Override
//    public void onConnectionFailed(ConnectionResult result) {
//        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
//    }

    @Subscribe
    public void onGPSDataEvent(TabletGPSDataEvent event) {
        Location location = event.location;
        // set the starting location
        if (mLocationCurrent == null && location != null) {
            LatLng closest  = StartingLocations.getClosest(
                    new LatLng( location.getLatitude(), location.getLongitude())
            );
            int zoom = StartingLocations.getZoom(closest);
            CameraPosition cameraPosition;
            cameraPosition = new CameraPosition.Builder()
                    .target(closest)
                    .zoom(zoom)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

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
                if (mCurrentMarker == null) {
                    // initialize regular current marker arrow
//                    Drawable arrow = getResources().getDrawable(R.drawable.location_arrow);
                    Drawable arrow = getResources().getDrawable(R.drawable.plane_icon);
                    mArrowBm = ((BitmapDrawable) arrow).getBitmap();
                    mArrowBm = mArrowBm.createScaledBitmap(mArrowBm, mArrowBm.getWidth()/5, mArrowBm.getHeight()/3, true);
                    // initialize green current marker arrow
//                    Drawable arrowGreen = getResources().getDrawable(R.drawable.location_arrow_green);
                    Drawable arrowGreen = getResources().getDrawable(R.drawable.plane_icon_green);
                    mArrowBmGreen = ((BitmapDrawable) arrowGreen).getBitmap();
                    mArrowBmGreen = mArrowBmGreen.createScaledBitmap(mArrowBmGreen,
                            mArrowBmGreen.getWidth()/3, mArrowBmGreen.getHeight()/3, true);
                    mCurrentMarker = mMap.addMarker(new MarkerOptions()
                            .position(current)
                            .flat(true)
                            .anchor((float) 0.5, (float) 0.5)
                            .rotation(mLocationCurrent.getBearing())
                            .icon(BitmapDescriptorFactory.fromBitmap(mArrowBm)));
                } else {
                    mCurrentMarker.setPosition(current);
                    mCurrentMarker.setRotation(mLocationCurrent.getBearing());
                    // set current marker color and exit enter times
                    Polygon polygon = GeoUtils.getPointPolygon(mFlightPolygons, current);
                    if (polygon != null) {
                        if (mIsInField == false) {
                            polygon.setFillColor(0x220000FF);
                            polygon.setStrokeColor(Color.MAGENTA);
                            mCurrentMarker.setIcon(BitmapDescriptorFactory.fromBitmap(mArrowBmGreen));
                            mTextTimeOfTurn.setText("0");
                        }
                        mIsInField = true;
                    } else {
                        if (mIsInField == true) {
                            for (Polygon p : mFlightPolygons) {
                                p.setFillColor(0x00000000);
                                p.setStrokeColor(Color.BLUE);
                            }
                            mCurrentMarker.setIcon(BitmapDescriptorFactory.fromBitmap(mArrowBm));
                            mExitFieldTime = SystemClock.elapsedRealtime();
                        }
                        mIsInField = false;
                        mEnterFieldTime = SystemClock.elapsedRealtime();
                        double timeOfTurn = mEnterFieldTime - mExitFieldTime;
                        String sTimeOfTurn = String.format("%d min, %d sec",
                                TimeUnit.MILLISECONDS.toMinutes((long)timeOfTurn),
                                TimeUnit.MILLISECONDS.toSeconds((long)timeOfTurn) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)timeOfTurn))
                        );
                        mTextTimeOfTurn.setText(sTimeOfTurn);
                    }
                }
            }
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            mCurrentLatLng = new LatLng(lat, lng);
            if (IS_DEV) {
                mTextCurrentLocation.setText((double)Math.round(lat*1000)/1000 + ", " + (double)Math.round(lng*1000)/1000);
            }
            if (mInterpA != null && mInterpB != null && mCurrentLatLng != null) {
                double trackDist = GeoUtils.getTrackDist(mInterpA, mInterpB, mLocationCurrent);
                displayTrackDist(trackDist);
                adjustLineIndicator(trackDist);
            }
            if (mIsFollowing) {
                CameraPosition cameraPosition;
                float zoom = mMap.getCameraPosition().zoom;
                if (mIsRotating) {
                    // get offset amount
//                    Projection proj = mMap.getProjection();
//                    LatLng northeast = proj.getVisibleRegion().latLngBounds.northeast;
//                    LatLng southwest = proj.getVisibleRegion().latLngBounds.southwest;
//                    double span = SphericalUtil.computeDistanceBetween(
//                            northeast, southwest);
//                    Log.e(TAG, Double.toString(span));
//                    LatLng interpCenter = SphericalUtil.computeOffset(mCurrentLatLng, span/8, location.getBearing());

                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
//                            .target(interpCenter)
                            .zoom(zoom)
                            .bearing(location.getBearing())
                            .build();
                } else {
                    mMap.setPadding(0,0,0,0);
                    cameraPosition = new CameraPosition.Builder()
                            .target(mCurrentLatLng)
                            .zoom(zoom)
                            .build();
                }
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            if (mFlightLine != null && mDestinationMarker != null) {
                List<LatLng> points = new ArrayList<LatLng>();
                points.add(mCurrentLatLng);
                points.add(mDestinationMarker.getPosition());
                mFlightLine.setPoints(points);

                // display travel info
                double dist = SphericalUtil.computeDistanceBetween(mCurrentLatLng, mDestinationMarker.getPosition());
                double brng = SphericalUtil.computeHeading(mCurrentLatLng, mDestinationMarker.getPosition());
                if (brng < 0) {
                    brng += 360;
                }
                double time = dist / mCurrentSpeed;
                long day = (int) TimeUnit.SECONDS.toDays((long) time);
                long hours = TimeUnit.SECONDS.toHours((long) time) - (day * 24);
                long minutes = TimeUnit.SECONDS.toMinutes((long) time) - (TimeUnit.SECONDS.toHours((long) time)* 60);
//                long second = TimeUnit.SECONDS.toSeconds((long) time) - (TimeUnit.SECONDS.toMinutes((long) time) *60);
                if (hours > 100) { hours = 0; minutes = 0; }
                if (mDestinationMarker.getSnippet() != null) {
                    String[] description = mDestinationMarker.getSnippet().split(", ");
                    String altitude = description[0].substring(5);
                    String distanceBetweenPass = description[1].substring(23);
                    mTextFieldAltitude.setText(altitude + "ft ASL");
                    mTextDistBetweenPass.setText(distanceBetweenPass + "ft");
                }
                dist = GeoUtils.toMiles(dist);
                mTextDistToField.setText(Integer.toString((int)Math.round(dist)) + "miles");
                mTextBrngToField.setText(Integer.toString((int)Math.round(brng)) + "\u00B0");
                mTextTimeToField.setText(Long.toString(hours) + "h " + Long.toString(minutes) + "m" );
            }
            mTextFieldsRemaining.setText(Integer.toString(mNumberOfFieldsRemaining) + " remaining");
            mTextFieldsPercentage.setText(Integer.toString((int)(100 - ((double)mNumberOfFieldsRemaining/mFlightPolygons.size())*100)) +
                "% completed");
            mLocationPrev = mLocationCurrent;
        }
    }

    @Subscribe
    public void onSingleBoardConnectionEvent(SingleBoardConnectionEvent event) {
        if (event.restarting) {
            mButtonSBC.setBackgroundColor(Color.LTGRAY);
            Toast.makeText(getApplicationContext(), "restarting SBC", Toast.LENGTH_LONG).show();
            SingleBoardConnectionService.clearErrors();
        } else if (SingleBoardConnectionService.inError) {
            mButtonSBC.setBackgroundColor(Color.RED);
        } else if (SingleBoardConnectionService.inWarning) {
            mButtonSBC.setBackgroundColor(Color.YELLOW);
        } else {
            if (event.connected) {
                mButtonSBC.setBackgroundColor(Color.GREEN);
            } else {
                mButtonSBC.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    @Subscribe
    public void onSingleBoardDataEvent(SingleBoardDataEvent event) {
        if (SingleBoardConnectionService.inError) {
            mButtonSBC.setBackgroundColor(Color.RED);
        } else if (SingleBoardConnectionService.inWarning) {
            mButtonSBC.setBackgroundColor(Color.YELLOW);
        } else {
            if (event.type.equals(SingleBoardStatus.STATUS)) {
            }
        }
    }

}