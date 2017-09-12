package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.MyViewPager;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.fragment.ListFragment;
import nctu.cs.cgv.itour.fragment.MapFragment;
import nctu.cs.cgv.itour.fragment.PersonalFragment;
import nctu.cs.cgv.itour.fragment.PlanFragment;
import nctu.cs.cgv.itour.fragment.SettingsFragment;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.mapTag;

public class MainActivity extends AppCompatActivity implements SettingsFragment.OnFogListener, SettingsFragment.OnDistanceIndicatorListener{

    private static final String TAG = "MainActivity";
    // view objects
    private MyViewPager viewPager;
    private List<Fragment> fragmentList;
    // MapFragment: communicate by calling fragment method
    private MapFragment mapFragment;
    private ListFragment listFragment;
    // use broadcast to send received checkinIcon data(fbc topic message) to activity
    private BroadcastReceiver messageReceiver;
    // device sensor manager
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            init();
        }
    }

    private void init() {
        setSensors();
        setBroadcastReceiver();
        setView();
    }

    private void setView() {
        mapFragment = MapFragment.newInstance();
        listFragment = ListFragment.newInstance();
        fragmentList = new ArrayList<>();
        fragmentList.add(mapFragment);
        fragmentList.add(listFragment);
        fragmentList.add(PersonalFragment.newInstance());
        fragmentList.add(PlanFragment.newInstance());
        fragmentList.add(SettingsFragment.newInstance());

        viewPager = (MyViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }
        });
        // disable swipe
        viewPager.setPagingEnabled(false);
        // set keep all three pages alive
        viewPager.setOffscreenPageLimit(5);

        BottomBar bottomBar = (BottomBar) findViewById(R.id.bottom_bar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_map:
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.tab_list:
                        viewPager.setCurrentItem(1);
                        break;
                    case R.id.tab_person:
                        viewPager.setCurrentItem(2);
                        break;
                    case R.id.tab_plan:
                        viewPager.setCurrentItem(3);
                        break;
                    case R.id.tab_settings:
                        viewPager.setCurrentItem(4);
                        break;
                }
            }
        });
    }

    private void setBroadcastReceiver() {
//        FirebaseMessaging.getInstance().subscribeToTopic(mapTag);

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "checkinIcon":
                        mapFragment.handleCheckinMsg(
                                intent.getStringExtra("postId"),
                                intent.getDoubleExtra("lat", 0),
                                intent.getDoubleExtra("lng", 0));

                        listFragment.addCheckin(intent.getStringExtra("postId"));
                        break;

                    case "gpsLocation":
                        mapFragment.handleLocationChange(
                                intent.getDoubleExtra("lat", 0),
                                intent.getDoubleExtra("lng", 0));
                        break;
                }
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
                        mapFragment.handleSensorChange(orientation[0]);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("checkinIcon");
        intentFilter.addAction("gpsLocation");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);

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
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

        if (magnetometer != null || accelerometer != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    public void onDistanceIndicatorSwitched(boolean flag) {
        mapFragment.switchDistanceIndicator(flag);
    }

    @Override
    public void onFogSwitched(boolean flag) {
        mapFragment.switchFog(flag);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int gpsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (gpsPermission + storagePermission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation();
            } else {
                requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            init();
        }
    }

    private void showExplanation() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Needed")
                .setMessage("We need to store map package on the device and track your GPS location to run this app!")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_MULTIPLE_REQUEST);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean gpsPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storagePermission && gpsPermission)
                    {
                        init();
                    } else {
                        showExplanation();
                    }
                }
                break;
        }
    }
}
