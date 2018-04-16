package nctu.cs.cgv.itour.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.Node;
import nctu.cs.cgv.itour.object.Notification;
import nctu.cs.cgv.itour.object.SpotList;
import nctu.cs.cgv.itour.object.SpotNode;

import static nctu.cs.cgv.itour.MyApplication.MAX_ZOOM;
import static nctu.cs.cgv.itour.MyApplication.MIN_ZOOM;
import static nctu.cs.cgv.itour.MyApplication.ZOOM_THRESHOLD;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.fileUploadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.appLog;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.hideSoftKeyboard;
import static nctu.cs.cgv.itour.Utility.imgPxToGps;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.Utility.spToPx;
import static nctu.cs.cgv.itour.activity.CheckinActivity.RESULT_CODE_CHECKIN_FINISH;

public class LocationChooseActivity extends AppCompatActivity {

    private static final String TAG = "LocationChooseActivity";
    // intent info
    private String description;
    private String photo;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float gpsDistortedX = -1;
    private float gpsDistortedY = -1;
    private int mapCenterX = 0;
    private int mapCenterY = 0;
    private int gpsMarkerPivotX = 0;
    private int gpsMarkerPivotY = 0;
    private int checkinIconPivotX = 0;
    private int checkinIconPivotY = 0;
    private int spotIconPivotX = 0;
    private int spotIconPivotY = 0;
    // UI references
    private RelativeLayout rootLayout;
    private AutoCompleteTextView locationEdit;
    private FloatingActionButton gpsBtn;
    private ImageView touristMap;
    private View checkinIcon;
    private View gpsMarker;
    // containers
    private SpotList spotList;
    private List<SpotNode> spotNodeList;
    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    // device sensor manager
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    // receive gps location
    private BroadcastReceiver messageReceiver;
    // flags
    private boolean isGpsCurrent = false;
    private boolean isOrientationCurrent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_choose);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
        }

        // set variables
        Intent intent = getIntent();
        description = intent.getStringExtra("description");
        photo = intent.getStringExtra("photo");

        // set actionBar title, top-left icon
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.activity_choose_location_title);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);

        // initialize objects
        transformMat = new Matrix();

        // get view references
        rootLayout = findViewById(R.id.parent_layout);
        locationEdit = findViewById(R.id.et_location);
        gpsBtn = findViewById(R.id.btn_gps);
        checkinIcon = findViewById(R.id.checkin_icon);
        gpsMarker = findViewById(R.id.gps_marker);

        // set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + "/" + mapTag + "_distorted_map.png");
        touristMap = new ImageView(this);
        touristMap.setLayoutParams(new RelativeLayout.LayoutParams(touristMapBitmap.getWidth(), touristMapBitmap.getHeight()));
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        ((FrameLayout) findViewById(R.id.touristmap)).addView(touristMap);

        // set gpsMarker
        int gpsMarkerWidth = (int) getResources().getDimension(R.dimen.gps_marker_width);
        int gpsMarkerHeight = (int) getResources().getDimension(R.dimen.gps_marker_height);
        int gpsDirectionHeight = (int) getResources().getDimension(R.dimen.gps_direction_height);
        int gpsMarkerPadding = (int) getResources().getDimension(R.dimen.gps_marker_padding);
        gpsMarkerPivotX = gpsMarkerWidth / 2 + gpsMarkerPadding;
        gpsMarkerPivotY = gpsDirectionHeight + gpsMarkerHeight / 2 + gpsMarkerPadding;
        gpsMarker.setPivotX(gpsMarkerPivotX);
        gpsMarker.setPivotY(gpsMarkerPivotY);

        // set buttons
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(LocationChooseActivity.this);
                if (gpsMarker.getVisibility() == View.GONE) {
                    Toast.makeText(LocationChooseActivity.this, getString(R.string.toast_gps_outside), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isGpsCurrent)
                    translateToImgPx(gpsDistortedX, gpsDistortedY, true);
                else if (!isOrientationCurrent)
                    rotateToNorth();
            }
        });

        // draw spots
        spotIconPivotX = (int) getResources().getDimension(R.dimen.spot_icon_width) / 2;
        spotIconPivotY = spToPx(this, 14) / 2;
        spotNodeList = new ArrayList<>();
        spotList = new SpotList(new File(dirPath + "/" + mapTag + "_spot_list.txt"));
        for (SpotNode spotNode : spotList.nodeMap.values()) {
            // allocate new spotNode instead using spotNode in nodeMap
            spotNodeList.add(new SpotNode(spotNode.x, spotNode.y, spotNode.name, spotNode.order));
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        for (SpotNode spotNode : spotNodeList) {
            addSpotNodeIcon(spotNode, inflater);
        }


        // set location autocomplete
        ArrayList<String> array = new ArrayList<>();
        array.addAll(spotList.getSpotsName());
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_search, array);
        locationEdit.setThreshold(0);
        locationEdit.setAdapter(adapter);
        locationEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideSoftKeyboard(LocationChooseActivity.this);
                String autocompleteStr = adapter.getItem(position);
                Node node = spotList.nodeMap.get(autocompleteStr);
                translateToImgPx(node.x, node.y, false);
            }
        });

        setTouchListener();
        setSensors();
        setBroadcastReceiver();

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                mapCenterX = rootLayout.getWidth() / 2;
                mapCenterY = rootLayout.getHeight() / 5 * 2;

                checkinIconPivotX = checkinIcon.getWidth() / 2;
                checkinIconPivotY = checkinIcon.getHeight();

                // translate to center
                checkinIcon.setTranslationX(mapCenterX - checkinIconPivotX);
                checkinIcon.setTranslationY(mapCenterY - 96);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_submit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_submit:
                checkin();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setTouchListener() {
        gestureDetector = new GestureDetector(
                this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                transformMat.postTranslate(-distanceX, -distanceY);
                isGpsCurrent = false;
                gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
                reRender();

                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(
                this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                float focusX = scaleGestureDetector.getFocusX();
                float focusY = scaleGestureDetector.getFocusY();
                float scaleFactor = scaleGestureDetector.getScaleFactor();
                // clamp scaleFactor
                scaleFactor = MAX_ZOOM >= scale * scaleFactor ? scaleFactor : MAX_ZOOM / scale;
                scaleFactor = MIN_ZOOM <= scale * scaleFactor ? scaleFactor : MIN_ZOOM / scale;

                transformMat.postTranslate(-focusX, -focusY);
                transformMat.postScale(scaleFactor, scaleFactor);
                transformMat.postTranslate(focusX, focusY);
                scale *= scaleFactor;

                reRender();

                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            }
        });

        rotationGestureDetector = new RotationGestureDetector(
                new RotationGestureDetector.OnRotationGestureListener() {
                    @Override
                    public void onRotation(RotationGestureDetector rotationDetector) {
                        float focusX = rotationDetector.getFocusX();
                        float focusY = rotationDetector.getFocusY();
                        float deltaAngel = -rotationDetector.getDeltaAngle();

                        transformMat.postTranslate(-focusX, -focusY);
                        transformMat.postRotate(deltaAngel);
                        transformMat.postTranslate(focusX, focusY);

                        rotation += deltaAngel;
                        if (rotation > 180)
                            rotation -= 360;
                        if (rotation <= -180)
                            rotation += 360;

                        isOrientationCurrent = false;

                        reRender();
                    }

                    @Override
                    public void onRotationEnd(RotationGestureDetector rotationDetector) {
                    }
                });

        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard(LocationChooseActivity.this);
                boolean res = false;
                res |= scaleGestureDetector.onTouchEvent(event);
                res |= rotationGestureDetector.onTouchEvent(event);
                res |= gestureDetector.onTouchEvent(event);
                return res;
            }
        });
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
                        final float RADIAN = 57.296f;
                        gpsMarker.setRotation(orientation[0] * RADIAN + rotation);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    private void setBroadcastReceiver() {
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "gpsUpdate":
                        handleGpsUpdate(
                                intent.getFloatExtra("lat", 0),
                                intent.getFloatExtra("lng", 0));
                        break;
                }
            }
        };
    }

    private void handleGpsUpdate(float lat, float lng) {

        // GPS is within tourist map.
        if (lat >= realMesh.minLat && lat <= realMesh.maxLat && lng >= realMesh.minLon && lng <= realMesh.maxLon) {

            if (gpsMarker.getVisibility() != View.VISIBLE) {
                gpsMarker.setVisibility(View.VISIBLE);
            }

            float[] imgPx = gpsToImgPx(lat, lng);

            if (imgPx[0] != -1 && imgPx[1] != -1) {

                // translate to center when handleGpsUpdate first time
                if (gpsDistortedX == -1 && gpsDistortedY == -1) {
                    translateToImgPx(imgPx[0], imgPx[1], true);
                }

                gpsDistortedX = imgPx[0];
                gpsDistortedY = imgPx[1];

                reRender();
            }

        } else { // GPS outside.

            if (gpsMarker.getVisibility() != View.GONE) {
                gpsMarker.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.toast_gps_outside), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void reRender() {

        boolean isMerged = scale < ZOOM_THRESHOLD;

        Matrix gpsMarkTransform = new Matrix();
        Matrix spotIconTransform = new Matrix();
        gpsMarkTransform.postTranslate(-gpsMarkerPivotX, -gpsMarkerPivotY);
        spotIconTransform.postTranslate(-spotIconPivotX, -spotIconPivotY);
        float[] point = new float[]{0, 0};

        // transform tourist map (ImageView)
        transformMat.mapPoints(point);
        touristMap.setScaleX(scale);
        touristMap.setScaleY(scale);
        touristMap.setRotation(rotation);
        touristMap.setTranslationX(point[0]);
        touristMap.setTranslationY(point[1]);

        // transform gpsMarker
        point[0] = gpsDistortedX;
        point[1] = gpsDistortedY;
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);

        // transform spot
        if (isMerged) {
            for (SpotNode spotNode : spotNodeList) {
                if (spotNode.order == 2) {
                    spotNode.icon.setVisibility(View.GONE);
                }
            }
        } else {
            for (SpotNode spotNode : spotNodeList) {
                if (spotNode.order == 2) {
                    spotNode.icon.setVisibility(View.VISIBLE);
                }
            }
        }

        for (SpotNode spotNode : spotNodeList) {
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }
    }

    private void translateToImgPx(final float x, final float y, final boolean toCurrent) {

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                float[] point = new float[]{x, y};
                transformMat.mapPoints(point);
                float distanceToCenterX = mapCenterX - point[0];
                float distanceToCenterY = mapCenterY - point[1];
                float scaleTo22 = ZOOM_THRESHOLD - scale;

                if (Math.abs(distanceToCenterX) <= 30.0f || Math.abs(distanceToCenterY) <= 30.0f) {
                    transformMat.postTranslate(distanceToCenterX, distanceToCenterY);
                    if (scale < ZOOM_THRESHOLD) {
                        transformMat.postTranslate(-point[0], -point[1]);
                        transformMat.postScale(ZOOM_THRESHOLD / scale, ZOOM_THRESHOLD / scale);
                        transformMat.postTranslate(point[0], point[1]);
                        scale = ZOOM_THRESHOLD;
                    }
                    reRender();
                    translationHandler.removeCallbacks(this);
                    if (toCurrent) {
                        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
                        isGpsCurrent = true;
                    } else {
                        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
                        isGpsCurrent = false;
                    }
                } else {
                    transformMat.postTranslate(distanceToCenterX / 5, distanceToCenterY / 5);
                    if (scale < ZOOM_THRESHOLD) {
                        transformMat.postTranslate(-point[0], -point[1]);
                        transformMat.postScale((scale + scaleTo22 / 5) / scale, (scale + scaleTo22 / 5) / scale);
                        transformMat.postTranslate(point[0], point[1]);
                        scale += scaleTo22 / 5;
                    }
                    reRender();
                    translationHandler.postDelayed(this, 5);
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 5);
    }

    private void rotateToNorth() {
        final Handler rotationHandler = new Handler();
        Runnable rotationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rotation) <= 6.0f) {
                    transformMat.postTranslate(-mapCenterX, -mapCenterY);
                    transformMat.postRotate(-rotation);
                    transformMat.postTranslate(mapCenterX, mapCenterY);
                    rotation = 0;
                    reRender();
                    rotationHandler.removeCallbacks(this);
                    isOrientationCurrent = true;
                } else {
                    transformMat.postTranslate(-mapCenterX, -mapCenterY);
                    transformMat.postRotate(-rotation / 5);
                    transformMat.postTranslate(mapCenterX, mapCenterY);
                    rotation -= rotation / 5;
                    reRender();
                    rotationHandler.postDelayed(this, 5);
                }
            }
        };
        rotationHandler.postDelayed(rotationInterpolation, 5);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("gpsUpdate");
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

    private void addSpotNodeIcon(final SpotNode spotNode, LayoutInflater inflater) {
        View icon = inflater.inflate(R.layout.item_spot, null);
        spotNode.icon = icon;
        ((TextView) spotNode.icon.findViewById(R.id.spot_name)).setText(spotNode.name);
        rootLayout.addView(icon, 1); // after tourist map, and before every other views.
    }

    private void checkin() {
        appLog("post checkin");

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.dialog_uploading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // calculate lat lng
        float[] point = new float[]{0, 0}; // tourist map position
        Matrix temp = new Matrix(transformMat);
        temp.postTranslate(-mapCenterX, -mapCenterY);
        temp.postRotate(-rotation);
        temp.postTranslate(mapCenterX, mapCenterY);
        temp.mapPoints(point);
        float[] gps = imgPxToGps((mapCenterX - point[0]) / scale, (mapCenterY - point[1]) / scale);
        final String lat = String.valueOf(gps[1]);
        final String lng = String.valueOf(gps[0]);

        // push firebase database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String key = databaseReference.child("checkin").child(mapTag).push().getKey();
        // rename file with postId
        if (!photo.equals("")) {
            File from = new File(getCacheDir().toString() + "/" + photo);
            File to = new File(getCacheDir().toString() + "/" + key + ".jpg");
            photo = key + ".jpg";
            from.renameTo(to);
        }

        // save checkin data to firebase database
        final String location = locationEdit.getText().toString().trim();
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final String username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        Checkin checkin = new Checkin(lat, lng, location, description, photo, uid, username, timestamp);
        Map<String, Object> checkinValues = checkin.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/checkin/" + mapTag + "/" + key, checkinValues);
        databaseReference.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {

                if (photo.equals("")) {
                    actionLog("post checkin", location, key);
                    pushNotification(key, username, location, lat, lng);

                    progressDialog.dismiss();
                    setResult(RESULT_CODE_CHECKIN_FINISH);
                    finish();
                } else {
                    // upload files to app server
                    try {
                        AsyncHttpClient client = new AsyncHttpClient();
                        RequestParams params = new RequestParams();
                        params.setForceMultipartEntityContentType(true);
                        params.put("photo", new File(getCacheDir().toString() + "/" + key + ".jpg"));
                        client.post(fileUploadURL, params, new AsyncHttpResponseHandler() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                // move files
                                if (getExternalCacheDir() != null) {
                                    if (!photo.equals(""))
                                        moveFile(getCacheDir().toString(), photo, getExternalCacheDir().toString());
                                }
                                actionLog("post checkin", location, key);
                                pushNotification(key, username, location, lat, lng);
                                progressDialog.dismiss();
                                setResult(RESULT_CODE_CHECKIN_FINISH);
                                finish();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                                databaseReference.child("checkin").child(mapTag).child(key).removeValue();
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), getString(R.string.toast_upload_file_failed) + statusCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        databaseReference.child("checkin").child(mapTag).child(key).removeValue();
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "FileNotFoundException", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void pushNotification(final String key, String username, final String location, String lat, String lng) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String notificationKey = databaseReference.child("notification").child(mapTag).push().getKey();
        Notification notification = new Notification(key,
                "all",
                username,
                location + " | " + description,
                photo,
                location,
                lat,
                lng,
                System.currentTimeMillis() / 1000);
        Map<String, Object> notificationValues = notification.toMap();
        Map<String, Object> notificationUpdates = new HashMap<>();
        notificationUpdates.put("/notification/" + mapTag + "/" + notificationKey, notificationValues);
        databaseReference.updateChildren(notificationUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                actionLog("push notification", location, key);
            }
        });
    }
}

