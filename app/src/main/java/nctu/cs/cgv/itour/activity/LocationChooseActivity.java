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
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.SpotNode;

import static com.arlib.floatingsearchview.util.Util.dpToPx;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.MyApplication.warpMesh;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.hideSoftKeyboard;

public class LocationChooseActivity extends AppCompatActivity {

    private static final String TAG = "LocationChooseActivity";
    // constants
    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    // intent info
    private String filename;
    private String description;
    private String type;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float currentLat = 0;
    private float currentLng = 0;
    private float gpsDistortedX = 0;
    private float gpsDistortedY = 0;
    private int touristMapWidth = 0;
    private int touristMapHeight = 0;
    private int mapCenterX = 0;
    private int mapCenterY = 0;
    private int gpsMarkerPivotX = 0;
    private int gpsMarkerPivotY = 0;
    private int checkinIconPivotX = 0;
    private int checkinIconPivotY = 0;
    private int spotIconPivotX = 0;
    private int spotIconPivotY = 0;
    // UI references
    private FloatingActionButton gpsBtn;
    private AutoCompleteTextView locationEdit;
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private View gpsMarker;
    private View checkinIcon;
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
    // firebase
    private DatabaseReference databaseReference;
    // flags
    private boolean isGpsCurrent = false;
    private boolean isOrientationCurrent = true;
    private boolean isTranslated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_choose);

        // set variables
        Intent intent = getIntent();
        filename = intent.getStringExtra("filename");
        description = intent.getStringExtra("description");
        type = intent.getStringExtra("type");

        // initialize objects
        transformMat = new Matrix();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "gpsLocation":
                        handleLocationChange(
                                intent.getDoubleExtra("lat", 0),
                                intent.getDoubleExtra("lng", 0));
                        break;
                }
            }
        };

        // set actionBar title, top-left icon
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("選擇地點");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);

        // get view references
        rootLayout = (RelativeLayout) findViewById(R.id.parent_layout);
        gpsBtn = (FloatingActionButton) findViewById(R.id.btn_gps);
        locationEdit = (AutoCompleteTextView) findViewById(R.id.et_location);
        checkinIcon = findViewById(R.id.checkin_icon);
        gpsMarker = findViewById(R.id.gps_marker);

        // load image from disk and set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
        touristMap = new ImageView(this);
        touristMap.setLayoutParams(new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight));
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
                if (!isGpsCurrent)
                    translateToCurrent();
                else if (!isOrientationCurrent)
                    rotateToNorth();
            }
        });

        // draw spots
        spotIconPivotX = (int) getResources().getDimension(R.dimen.spot_icon_width) / 2;
        spotIconPivotY = (int) getResources().getDimension(R.dimen.spot_icon_height) / 2;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.nodes.entrySet()) {
            SpotNode spotNode = spotNodeEntry.getValue();
            View icon = inflater.inflate(R.layout.item_spot, null);
            ((TextView) icon.findViewById(R.id.spot_name)).setText(spotNode.name);
            spotNode.icon = icon;
            rootLayout.addView(icon, 1);
        }

        // set location autocomplete
        ArrayList<String> array = new ArrayList<>();
        array.addAll(spotList.getSpots());
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_search, array);
        locationEdit.setThreshold(1);
        locationEdit.setAdapter(adapter);
        locationEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideSoftKeyboard(LocationChooseActivity.this);
                translateToSpot(spotList.nodes.get(adapter.getItem(position)));
            }
        });

        setTouchListener();
        setSensors();

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                mapCenterX = rootLayout.getWidth() / 2;
                mapCenterY = rootLayout.getHeight() / 5 * 2;

                checkinIconPivotX = checkinIcon.getWidth() / 2;
                checkinIconPivotY = checkinIcon.getHeight();

                // translate to center
                checkinIcon.setTranslationX(mapCenterX - checkinIconPivotX);
                checkinIcon.setTranslationY(mapCenterY - checkinIconPivotY);

                reRender();
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
                        gpsMarker.setRotation(orientation[0] * RADIAN);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    private void reRender() {
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
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.nodes.entrySet()) {
            SpotNode spotNode = spotNodeEntry.getValue();
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }
    }

    private void translateToSpot(final SpotNode spotNode) {
        final float transX = mapCenterX - (spotNode.icon.getTranslationX() + spotIconPivotX);
        final float transY = mapCenterY - (spotNode.icon.getTranslationY() + spotIconPivotY);
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;
        final float deltaScale = (2.2f - scale) / 10f;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                float distanceToTransX = mapCenterX - (spotNode.icon.getTranslationX() + spotIconPivotX);
                float distanceToTransY = mapCenterY - (spotNode.icon.getTranslationY() + spotIconPivotY);

                if (Math.abs(distanceToTransX) <= Math.abs(deltaTransX) || Math.abs(distanceToTransY) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(distanceToTransX, distanceToTransY);
                    if (scale < 2.2) {
                        transformMat.postTranslate(-spotNode.icon.getTranslationX() - spotIconPivotX, -spotNode.icon.getTranslationY() - spotIconPivotY);
                        transformMat.postScale(2.2f / scale, 2.2f / scale);
                        transformMat.postTranslate(spotNode.icon.getTranslationX() + spotIconPivotY, spotNode.icon.getTranslationY() + spotIconPivotY);
                        scale = 2.2f;
                    }

                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);

                    if (scale < 2.2) {
                        transformMat.postTranslate(-spotNode.icon.getTranslationX() - spotIconPivotX, -spotNode.icon.getTranslationY() - spotIconPivotY);
                        transformMat.postScale((scale + deltaScale) / scale, (scale + deltaScale) / scale);
                        transformMat.postTranslate(spotNode.icon.getTranslationX() + spotIconPivotX, spotNode.icon.getTranslationY() + spotIconPivotY);
                        scale += deltaScale;
                    }

                    reRender();
                    if (Math.abs(distanceToTransX) < 300) {
                        // slow down
                        translationHandler.postDelayed(this, 5);
                    } else {
                        translationHandler.postDelayed(this, 2);
                    }
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 2);
        isGpsCurrent = false;
        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
    }

    private void translateToCurrent() {
        final float transX = mapCenterX - (gpsMarker.getTranslationX() + gpsMarkerPivotX);
        final float transY = mapCenterY - (gpsMarker.getTranslationY() + gpsMarkerPivotY);
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                float distanceToTransX = mapCenterX - (gpsMarker.getTranslationX() + gpsMarkerPivotX);
                float distanceToTransY = mapCenterY - (gpsMarker.getTranslationY() + gpsMarkerPivotY);

                if (Math.abs(distanceToTransX) <= Math.abs(deltaTransX) || Math.abs(distanceToTransY) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(distanceToTransX, distanceToTransY);
                    translationHandler.removeCallbacks(this);
                    reRender();
                    gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
                    isGpsCurrent = true;
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);
                    reRender();
                    if (Math.abs(distanceToTransX) < 300) {
                        // slow down
                        translationHandler.postDelayed(this, 5);
                    } else {
                        translationHandler.postDelayed(this, 2);
                    }
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 2);
    }

    private void rotateToNorth() {
        final float deltaAngle = rotation / 10;

        final Handler rotationHandler = new Handler();
        Runnable rotationInterpolation = new Runnable() {
            @Override
            public void run() {
                transformMat.postTranslate(-mapCenterX, -mapCenterY);
                transformMat.postRotate(-deltaAngle);
                transformMat.postTranslate(mapCenterX, mapCenterY);
                rotation -= deltaAngle;
                reRender();
                if (Math.abs(rotation) <= Math.abs(deltaAngle)) {
                    transformMat.postTranslate(-mapCenterX, -mapCenterY);
                    transformMat.postRotate(-rotation);
                    transformMat.postTranslate(mapCenterX, mapCenterY);
                    rotationHandler.removeCallbacks(this);
                    rotation = 0;
                    reRender();
                    isOrientationCurrent = true;
                } else {
                    rotationHandler.postDelayed(this, 1);
                }
            }
        };
        rotationHandler.postDelayed(rotationInterpolation, 1);
    }

    private void handleLocationChange(double lat, double lng) {

        currentLat = (float) lat;
        currentLng = (float) lng;

        float[] point = gpsToImgPx(realMesh, warpMesh, currentLat, currentLng);

        gpsDistortedX = point[0];
        gpsDistortedY = point[1];

        reRender();

        // translate to center when handleLocationChange first time
        if (!isTranslated) {
            isTranslated = true;
            translateToCurrent();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
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

    private void checkin() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        // calculate lat lng
        float[] point = new float[]{0, 0}; // tourist map position
        float latCenter = currentLat;
        float lngCenter = currentLng;
        Matrix temp = new Matrix();
        temp.set(transformMat);
        temp.postTranslate(-mapCenterX, -mapCenterY);
        temp.postRotate(-rotation);
        temp.postTranslate(mapCenterX, mapCenterY);
        temp.mapPoints(point);
        IdxWeights idxWeights = warpMesh.getPointInTriangleIdx((mapCenterX - point[0]) / scale, (mapCenterY - point[1]) / scale);
        if (idxWeights.idx >= 0) {
            double[] newPos = realMesh.interpolatePosition(idxWeights);
            lngCenter = (float) (newPos[0] / realMesh.mapWidth * (realMesh.maxLon - realMesh.minLon) + realMesh.minLon);
            latCenter = (float) (realMesh.maxLat - newPos[1] / realMesh.mapHeight * (realMesh.maxLat - realMesh.minLat));
        }

        // push firebase database
        final String key = databaseReference.child("checkin").child(mapTag).push().getKey();
        // rename file with postId
        if (type.equals("photo")) {
            File from = new File(getCacheDir().toString() + "/" + filename);
            File to = new File(getCacheDir().toString() + "/" + key + ".jpg");
            filename = key + ".jpg";
            from.renameTo(to);
        }
        if (type.equals("audio")) {
            File from = new File(getCacheDir().toString() + "/" + filename);
            File to = new File(getCacheDir().toString() + "/" + key + ".mp4");
            from.renameTo(to);
            filename = key + ".mp4";
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        String location = locationEdit.getText().toString().trim();
        Checkin checkin = new Checkin(String.valueOf(latCenter), String.valueOf(lngCenter), location, description, filename, type, uid, username);
        Map<String, Object> checkinValues = checkin.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/checkin/" + mapTag + "/" + key, checkinValues);
        // push to database than upload file to server and server will send notification
        final float finalLngCenter = lngCenter;
        final float finalLatCenter = latCenter;
        databaseReference.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                // upload file
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.setForceMultipartEntityContentType(true);
                try {
                    File file = new File(getCacheDir().toString() + "/" + filename);
                    if (file.exists())
                        params.put("file", file);
                    params.put("mapTag", mapTag);
                    params.put("postId", key);
                    params.put("lat", finalLatCenter);
                    params.put("lng", finalLngCenter);

                    client.post("https://itour-lobst3rd.c9users.io/upload", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            progressDialog.dismiss();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Upload failed." + statusCode, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });


        // upload audio
//        AsyncHttpClient client = new AsyncHttpClient();
//        RequestParams params = new RequestParams();
//        params.setForceMultipartEntityContentType(true);
//        try {
//            File audioFile = new File(filename);
//            if (audioFile.exists())
//                params.put("file", audioFile);
//            params.put("mapTag", mapTag);
//            params.put("lat", latCenter);
//            params.put("lng", lngCenter);
//            params.put("location", location);
//            params.put("description", description);
//            params.put("type", type);
//
//            client.post("https://itour-lobst3rd.c9users.io/upload", params, new AsyncHttpResponseHandler() {
//                @Override
//                public void onStart() {
//                }
//
//                @Override
//                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
//                    progressDialog.dismiss();
//                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                }
//
//                @Override
//                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
//                    progressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(), "Upload failed." + statusCode, Toast.LENGTH_LONG).show();
//                }
//            });
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }
}

