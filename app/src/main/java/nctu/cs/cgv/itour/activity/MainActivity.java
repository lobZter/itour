package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.MyViewPager;
import nctu.cs.cgv.itour.fragment.ListFragment;
import nctu.cs.cgv.itour.fragment.MapFragment;
import nctu.cs.cgv.itour.fragment.PersonalFragment;
import nctu.cs.cgv.itour.fragment.PlanFragment;
import nctu.cs.cgv.itour.fragment.SettingsFragment;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotList;
import nctu.cs.cgv.itour.service.GpsLocationService;
import nctu.cs.cgv.itour.service.ScreenShotService;

import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.edgeNode;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.MyApplication.warpMesh;
import static nctu.cs.cgv.itour.Utility.actionLog;

public class MainActivity extends AppCompatActivity implements
        SettingsFragment.OnFogListener,
        SettingsFragment.OnDistanceIndicatorListener,
        SettingsFragment.OnCheckinIconListener,
        SettingsFragment.OnSpotIonListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    // Checkins
    public static Map<String, Checkin> checkinMap;
    public static Map<String, Boolean> savedPostId;
    // view objects
    private MyViewPager viewPager;
    private BottomBar bottomBar;
    private List<Fragment> fragmentList;
    // MapFragment: communicate by calling fragment method
    private MapFragment mapFragment;
    private ListFragment listFragment;
    private PersonalFragment personalFragment;
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
        realMesh = new Mesh(new File(dirPath + "/" + mapTag + "_mesh.txt"));
        realMesh.readBoundingBox(new File(dirPath + "/" + mapTag + "_bound_box.txt"));
        warpMesh = new Mesh(new File(dirPath + "/" + mapTag + "_warpMesh.txt"));
        spotList = new SpotList(new File(dirPath + "/" + mapTag + "_spot_list.txt"));
        edgeNode = new EdgeNode(new File(dirPath + "/" + mapTag + "_edge_length.txt"));

        checkinMap = new LinkedHashMap<>();
        savedPostId = new LinkedHashMap<>();

        startService(new Intent(this, GpsLocationService.class));
        setSensors();
        setBroadcastReceiver();
        setCheckinPreference();
        setView();
//        if (logFlag) requestScreenCapture();
    }

    private void setCheckinPreference() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("checkin", true);
        editor.apply();
    }

    public void queryCheckin() {
        checkinMap.clear();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("checkin").child(mapTag);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Checkin checkin = dataSnapshot.getValue(Checkin.class);
                checkin.key = dataSnapshot.getKey();
                checkinMap.put(dataSnapshot.getKey(), checkin);
                mapFragment.addCheckin(checkin);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // like change
                Checkin checkin = dataSnapshot.getValue(Checkin.class);
                checkin.key = dataSnapshot.getKey();
                checkinMap.put(dataSnapshot.getKey(), checkin);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Checkin checkin = dataSnapshot.getValue(Checkin.class);
                checkin.key = dataSnapshot.getKey();
                checkinMap.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void querySavedPostId() {
        savedPostId.clear();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final Query saveQuery = databaseReference.child("user").child(uid).child("saved").child(mapTag);

        saveQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                savedPostId.put(dataSnapshot.getKey(), (Boolean) dataSnapshot.getValue());

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                savedPostId.put(dataSnapshot.getKey(), (Boolean) dataSnapshot.getValue());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                savedPostId.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setView() {
        mapFragment = MapFragment.newInstance();
        listFragment = ListFragment.newInstance();
        personalFragment = PersonalFragment.newInstance();
        fragmentList = new ArrayList<>();
        fragmentList.add(mapFragment);
        fragmentList.add(listFragment);
        fragmentList.add(personalFragment);
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

        bottomBar = (BottomBar) findViewById(R.id.bottom_bar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_map:
                        viewPager.setCurrentItem(0);
                        actionLog("Current Page: map");
                        break;
                    case R.id.tab_list:
                        viewPager.setCurrentItem(1);
                        actionLog("Current Page: list");
                        break;
                    case R.id.tab_person:
                        viewPager.setCurrentItem(2);
                        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                        }
                        actionLog("Current Page: personal");
                        break;
                    case R.id.tab_plan:
                        viewPager.setCurrentItem(3);
                        actionLog("Current Page: plan");
                        break;
                    case R.id.tab_settings:
                        viewPager.setCurrentItem(4);
                        actionLog("Current Page: setting");
                        break;
                }
            }
        });
    }

    private void setBroadcastReceiver() {
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                switch (intent.getAction()) {
                    case "gpsUpdate":
                        mapFragment.handleGpsUpdate(
                                intent.getFloatExtra("lat", 0),
                                intent.getFloatExtra("lng", 0));
                        break;
                    case "fogUpdate":
                        mapFragment.handleFogUpdate(
                                intent.getFloatExtra("lat", 0),
                                intent.getFloatExtra("lng", 0));
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

    private void requestScreenCapture() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Intent service = new Intent(this, ScreenShotService.class);
                service.putExtra("resultCode", resultCode);
                service.putExtra("resultData", data);
                startService(service);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("gpsUpdate");
        intentFilter.addAction("fogUpdate");
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
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, GpsLocationService.class));
    }

    @Override
    public void onDistanceIndicatorSwitched(boolean flag) {
        mapFragment.switchDistanceIndicator(flag);
    }

    @Override
    public void onFogSwitched(boolean flag) {
        mapFragment.switchFog(flag);
    }

    @Override
    public void onCheckinIconSwitched(boolean flag) {
        mapFragment.switchCheckinIcon(flag);
    }

    @Override
    public void onSpotIconSwitched(boolean flag) {
        mapFragment.switchSpotIcon(flag);
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
        builder.setTitle(R.string.permission_title)
                .setMessage(R.string.permission_message)
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

                    if (storagePermission && gpsPermission) {
                        init();
                    } else {
                        showExplanation();
                    }
                }
                break;
        }
    }

    public void onLocateClick(float imgPxX, float imgPxY) {
        mapFragment.translateToImgPx(imgPxX, imgPxY, false);
        bottomBar.selectTabAtPosition(0);
    }
}
