package nctu.cs.cgv.itour.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

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
import nctu.cs.cgv.itour.fragment.PlanFragment;
import nctu.cs.cgv.itour.fragment.SettingsFragment;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.mapTag;

public class MainActivity extends AppCompatActivity {

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
    // Firebase
    private DatabaseReference databaseReference;
    private ValueEventListener checkinListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        fragmentList.add(PlanFragment.newInstance());
        fragmentList.add(SettingsFragment.newInstance());

        viewPager = (MyViewPager) findViewById(R.id.viewpager);
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
        viewPager.setOffscreenPageLimit(3);

        BottomBar bottomBar = (BottomBar) findViewById(R.id.bottomBar);
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
                    case R.id.tab_plan:
                        viewPager.setCurrentItem(2);
                        break;
                    case R.id.tab_settings:
                        viewPager.setCurrentItem(3);
                        break;
                }
            }
        });

        bottomBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
            }
        });
    }

    private void setBroadcastReceiver() {
        FirebaseMessaging.getInstance().subscribeToTopic(mapTag);

        checkinListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Checkin checkin = issue.getValue(Checkin.class);
                        mapFragment.handleCheckinMsg(issue.getKey(), checkin);
//                        listFragment.addToList(checkin);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
//        databaseReference = FirebaseDatabase.getInstance().getReference().child("checkin").child(mapTag);
//        databaseReference.addValueEventListener(checkinListener);

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "checkinIcon":
                        mapFragment.handleCheckinMsg(
                                intent.getStringExtra("postId"),
                                intent.getDoubleExtra("lat", 0),
                                intent.getDoubleExtra("lng", 0));
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
    protected void onStop() {
        super.onStop();

        if (databaseReference != null) {
            databaseReference.removeEventListener(checkinListener);
        }
    }
}
