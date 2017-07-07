package nctu.cs.cgv.itour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.ncapdevi.fragnav.FragNavController;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        FragNavController.RootFragmentListener {

    private static final String TAG = "MainActivity";
    private String mapTag;
    // view objects
    private BottomBar bottomBar;
    private FragNavController fragNavController;
    // fragment index
    private final int INDEX_MAP = FragNavController.TAB1;
    private final int INDEX_PLAN = FragNavController.TAB2;
    private final int INDEX_SETTINGS = FragNavController.TAB3;
    // Google Services Location API
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location currentLocation;
    // device sensor manager
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    // use broadcast to send received checkin data(fbc topic message) to activity
    private BroadcastReceiver messageReceiver;
    // MapFragment: communicate by calling fragment method
    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get mapTag passed from MapList
        Intent intent = getIntent();
        mapTag = intent.getStringExtra("mapTag");
        mapTag = "nctu";

        // set Location API
        buildGoogleApiClient();
        createLocationRequest();
        setSensors();
        setBroadcastReceiver();

        setView(savedInstanceState);
    }

    private void setView(Bundle savedInstanceState) {
        mapFragment = MapFragment.newInstance(mapTag);

        fragNavController = FragNavController.newBuilder(savedInstanceState, getSupportFragmentManager(), R.id.fragment_content)
                .rootFragmentListener(this, 3)
                .build();

        bottomBar = (BottomBar) findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_map:
                        fragNavController.switchTab(INDEX_MAP);
                        break;
                    case R.id.tab_plan:
                        fragNavController.switchTab(INDEX_PLAN);
                        break;
                    case R.id.tab_settings:
                        fragNavController.switchTab(INDEX_SETTINGS);
                        break;
                }
            }
        });

        bottomBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                fragNavController.clearStack();
            }
        });
    }

    private void setBroadcastReceiver() {
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String lat = intent.getStringExtra("lat");
                String lng = intent.getStringExtra("lng");
                mapFragment.handleCheckinMsg(Float.valueOf(lat), Float.valueOf(lng));
                Log.d(TAG, "Got message: " + lat + ", " + lng);
            }
        };
    }

    private void setSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    gravity = event.values;

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    geomagnetic = event.values;

                if (gravity != null && geomagnetic != null) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
                    if (success) {
                        float orientation[] = new float[3];
                        SensorManager.getOrientation(R, orientation);
//                    glRenderer.handleSensorChange(orientation[0]);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    @Override
    public void onBackPressed() {
        if (!fragNavController.isRootFragment()) {
            fragNavController.popFragment();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (fragNavController != null) {
            fragNavController.onSaveInstanceState(outState);
        }
    }

    @Override
    public Fragment getRootFragment(int index) {
        switch (index) {
            case INDEX_MAP:
                return mapFragment;
            case INDEX_PLAN:
                return PlanFragment.newInstance();
            case INDEX_SETTINGS:
                return SettingsFragment.newInstance();
        }
        throw new IllegalStateException("Unexpected tab index");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("my-event"));

        googleApiClient.connect();

        if (currentLocation != null) {
            mapFragment.handleLocationChange(currentLocation);
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                    sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        if (magnetometer != null) {
            sensorManager.registerListener(
                    sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }

        if (magnetometer != null || accelerometer != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(), errorCode: " + String.valueOf(cause));

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(), errorCode: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        mapFragment.handleLocationChange(location);
    }
}
