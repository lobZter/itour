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
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class LocationChooseActivity extends AppCompatActivity {

    private static final String TAG = "LocationChooseActivity";
    // constants
    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    private final int gpsMarkerWidth = 48;
    private final int gpsMarkerHeight = 48;
    private final int gpsDirectionWidth = 32;
    private final int gpsDirectionHeight = 32;
    private final int checkinIconWidth = 64;
    private final int checkinIconHeight = 64;
    // intent info
    private String location;
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
    private int screenWidth = 0;
    private int screenHeight = 0;
    private int rootLayoutWidth = 0;
    private int rootLayoutHeight = 0;
    // UI references
    private FloatingActionButton gpsBtn;
    private LinearLayout gpsMarker;
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private ImageView mapCenter;
    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    // receive gps location
    private BroadcastReceiver messageReceiver;
    // device sensor manager
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
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

        // set actionBar title, top-left icon
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        // get variables from checkinActivity
        Intent intent = getIntent();
        location = intent.getStringExtra("location");
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

        // get screen size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // set views
        rootLayout = (RelativeLayout) findViewById(R.id.parent_layout);

        // load image from disk and set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight);
        touristMap = new ImageView(this);
        touristMap.setLayoutParams(layoutParams);
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        ((FrameLayout)findViewById(R.id.touristmap)).addView(touristMap);

        // set gpsMarker
        gpsMarker = (LinearLayout) findViewById(R.id.gps_marker);
        gpsMarker.setElevation(1);
        gpsMarker.setPivotX(gpsMarkerWidth / 2);
        gpsMarker.setPivotY(gpsMarkerHeight / 2 + gpsDirectionHeight);

        // map center marker for checkinIcon
        mapCenter = new ImageView(this);
        mapCenter.setImageResource(R.drawable.ic_location_on_red_600_24dp);
        mapCenter.setElevation(2);  // higher than gpsMarker
        rootLayout.addView(mapCenter);

        // draw spots
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.nodes.entrySet()) {
            addSpot(spotNodeEntry.getValue());
        }

        // set buttons
        gpsBtn = (FloatingActionButton) findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGpsCurrent && !isOrientationCurrent) rotateToNorth();
                else translateToCurrent();
            }
        });

        setTouchListener();
        setSensors();

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                rootLayoutWidth = rootLayout.getWidth();
                rootLayoutHeight = rootLayout.getHeight();
                touristMap.setScaleType(ImageView.ScaleType.MATRIX);

                // transform mapcenter icon to center
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(rootLayoutWidth / 2 - checkinIconWidth / 2, rootLayoutHeight / 3 - checkinIconHeight, 0, 0);
                mapCenter.setLayoutParams(layoutParams);

                translateToCurrent();
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
        gpsMarkTransform.postTranslate(-(gpsMarkerWidth / 2), -(gpsMarkerHeight / 2 + gpsDirectionHeight));
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
            Matrix spotIconTransform = new Matrix();
            spotIconTransform.postTranslate(-dpToPx(12 / 2), -dpToPx(12 / 2));
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }
    }

    private void addSpot(SpotNode spotNode) {
        // create icon
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View icon = inflater.inflate(R.layout.item_spot, null);
        TextView spotNameView = (TextView) icon.findViewById(R.id.spot_name);
        spotNameView.setText(spotNode.name);
        spotNode.icon = icon;
        // transform icon
        Matrix iconTransform = new Matrix();
        float[] gpsDistorted = {spotNode.x, spotNode.y};
        iconTransform.postTranslate(-dpToPx(12 / 2), -dpToPx(12 / 2));
        transformMat.mapPoints(gpsDistorted);
        iconTransform.mapPoints(gpsDistorted);
        icon.setTranslationX(gpsDistorted[0]);
        icon.setTranslationY(gpsDistorted[1]);
        // add to rootlayout
        rootLayout.addView(icon);
    }

    private void translateToCurrent() {
        final float transX = rootLayoutWidth / 2 - (gpsMarker.getTranslationX() + gpsMarkerWidth / 2);
        final float transY = rootLayoutHeight / 3 - (gpsMarker.getTranslationY() + gpsDirectionHeight + gpsMarkerHeight / 2);
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rootLayoutWidth / 2 - (gpsMarker.getTranslationX() + gpsMarkerWidth / 2)) <= Math.abs(deltaTransX) ||
                        Math.abs(rootLayoutHeight / 3 - (gpsMarker.getTranslationY() + gpsDirectionHeight + gpsMarkerHeight / 2)) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(
                            rootLayoutWidth / 2 - (gpsMarker.getTranslationX() + gpsMarkerWidth / 2),
                            rootLayoutHeight / 3 - (gpsMarker.getTranslationY() + gpsDirectionHeight + gpsMarkerHeight / 2));
                    reRender();
                    translationHandler.removeCallbacks(this);
                    gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
                    isGpsCurrent = true;
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);
                    reRender();
                    if (Math.abs(rootLayoutWidth / 2 - (gpsMarker.getTranslationX() + gpsMarkerWidth / 2)) < rootLayoutHeight / 4) {
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
                transformMat.postTranslate(-rootLayoutWidth / 2, -rootLayoutHeight / 3);
                transformMat.postRotate(-deltaAngle);
                transformMat.postTranslate(rootLayoutWidth / 2, rootLayoutHeight / 3);
                rotation -= deltaAngle;
                reRender();
                if (Math.abs(rotation) <= Math.abs(deltaAngle)) {
                    transformMat.postTranslate(-rootLayoutWidth / 2, -rootLayoutHeight / 3);
                    transformMat.postRotate(-rotation);
                    transformMat.postTranslate(rootLayoutWidth / 2, rootLayoutHeight / 3);
                    rotation = 0;
                    reRender();
                    rotationHandler.removeCallbacks(this);
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

        // transform gps marker
        Matrix gpsMarkTransform = new Matrix();
        gpsMarkTransform.postTranslate(-(gpsMarkerWidth / 2), -(gpsMarkerHeight / 2 + gpsDirectionHeight));
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);

        if(!isTranslated) {
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

        float[] point = new float[]{0, 0}; // tourist map position
        float latCenter = currentLat;
        float lngCenter = currentLng;
        Matrix temp = new Matrix();
        temp.set(transformMat);

        // calculate lat lng
        temp.postTranslate(-rootLayoutWidth / 2, -rootLayoutHeight / 3);
        temp.postRotate(-rotation);
        temp.postTranslate(rootLayoutWidth / 2, rootLayoutHeight / 3);
        temp.mapPoints(point);
        IdxWeights idxWeights = warpMesh.getPointInTriangleIdx((rootLayoutWidth / 2 - point[0]) / scale, (rootLayoutHeight / 3 - point[1]) / scale);
        if (idxWeights.idx >= 0) {
            double[] newPos = realMesh.interpolatePosition(idxWeights);
            lngCenter = (float) (newPos[0] / realMesh.mapWidth * (realMesh.maxLon - realMesh.minLon) + realMesh.minLon);
            latCenter = (float) (realMesh.maxLat - newPos[1] / realMesh.mapHeight * (realMesh.maxLat - realMesh.minLat));
        }

        // push firebase database
        final String key = databaseReference.child("checkin").child(mapTag).push().getKey();
        // rename file with postId
        File from = new File(getCacheDir().toString()+ "/" + filename);
        File to = new File(getCacheDir().toString()+ "/" + key + ".jpg");
        from.renameTo(to);
        if(type.equals("photo"))
            filename = key + ".jpg";
        if(type.equals("audio"))
            filename = key + ".3gp";
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
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
                    File file = new File(filename);
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

