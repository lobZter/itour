package nctu.cs.cgv.itour.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Mesh;

import static nctu.cs.cgv.itour.MyApplication.dirPath;

public class LocationChooseActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationChooseActivity";
    // constants
    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    private final int gpsMarkerWidth = 48;
    private final int gpsMarkerHeight = 48;
    private final int gpsDirectionWidth = 32;
    private final int gpsDirectionHeight = 32;
    private final int nodeIconWidth = 16;
    private final int nodeIconHeight = 16;
    private final int checkinIconWidth = 64;
    private final int checkinIconHeight = 64;
    private String mapTag;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float lat = 0, lng = 0;
    private float gpsDistortedX = 0;
    private float gpsDistortedY = 0;
    private float lastFogClearPosX = 0;
    private float lastFogClearPosY = 0;
    private int initialOffsetX = 0;
    private int initialOffsetY = 0;
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
    // Objects
    private Mesh realMesh;
    private Mesh warpMesh;
    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
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
    // flags
    private boolean isGpsCurrent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_choose);

        Intent intent = getIntent();
        mapTag = intent.getStringExtra("MAP");
        mapTag = "nctu";

        // Set Location API.
        buildGoogleApiClient();
        createLocationRequest();

        // Set Sensors.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

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
        touristMap = new ImageView(this);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setScaleType(ImageView.ScaleType.FIT_START);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        rootLayout.addView(touristMap);

        // set gpsMarker
        gpsMarker = (LinearLayout) findViewById(R.id.gps_marker);
        gpsMarker.setPivotX(gpsMarkerWidth / 2);
        gpsMarker.setPivotY(gpsMarkerHeight / 2 + gpsDirectionHeight);

        // map center marker for checkin

        mapCenter = new ImageView(this);
        mapCenter.setImageResource(R.drawable.ic_location_on_red_600_24dp);
        mapCenter.setElevation(2);
        rootLayout.addView(mapCenter);

        // set buttons
        gpsBtn = (FloatingActionButton) findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGpsCurrent) rotateToNorth();
                else translateToCurrent();
            }
        });

        realMesh = new Mesh(new File(dirPath + mapTag + "_mesh.txt"));
        realMesh.readBoundingBox(new File(dirPath + mapTag + "_bound_box.txt"));
        warpMesh = new Mesh(new File(dirPath + mapTag + "_warpMesh.txt"));
        transformMat = new Matrix();

        setTouchListener();
        setSensors();

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                rootLayoutWidth = rootLayout.getWidth();
                rootLayoutHeight = rootLayout.getHeight();
                touristMap.setScaleType(ImageView.ScaleType.MATRIX);

                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(rootLayoutWidth / 2, rootLayoutHeight / 3, 0, 0);
                mapCenter.setLayoutParams(layoutParams);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_submit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.btn_submit:
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
    }

    private void translateToCurrent() {
        final float transX = screenWidth / 2 - gpsMarker.getTranslationX();
        final float transY = screenHeight / 2 - gpsMarker.getTranslationY();
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(screenWidth / 2 - gpsMarker.getTranslationX()) < Math.abs(deltaTransX)) {
                    transformMat.postTranslate(
                            screenWidth / 2 - gpsMarker.getTranslationX(),
                            screenHeight / 2 - gpsMarker.getTranslationY());
                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);
                    reRender();
                    if (Math.abs(screenWidth / 2 - gpsMarker.getTranslationX()) < screenHeight / 4) {
                        translationHandler.postDelayed(this, 5);
                    } else {
                        translationHandler.postDelayed(this, 2);
                    }
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 2);
        isGpsCurrent = true;
        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
    }

    private void rotateToNorth() {
        final float deltaAngle = rotation / 10;

        final Handler rotationHandler = new Handler();
        Runnable rotationInterpolation = new Runnable() {
            @Override
            public void run() {
                transformMat.postTranslate(-screenWidth / 2, -screenHeight / 2);
                transformMat.postRotate(-deltaAngle);
                transformMat.postTranslate(screenWidth / 2, screenHeight / 2);
                rotation -= deltaAngle;
                reRender();
                if (Math.abs(rotation) <= Math.abs(deltaAngle)) {
                    transformMat.postTranslate(-screenWidth / 2, -screenHeight / 2);
                    transformMat.postRotate(-rotation);
                    transformMat.postTranslate(screenWidth / 2, screenHeight / 2);
                    rotation = 0;
                    reRender();
                    rotationHandler.removeCallbacks(this);
                } else {
                    rotationHandler.postDelayed(this, 1);
                }
            }
        };
        rotationHandler.postDelayed(rotationInterpolation, 1);
    }

    private void handleLocationChange(Location currentLocation) {
        lat = (float) currentLocation.getLatitude();
        lng = (float) currentLocation.getLongitude();
        double imgX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        double imgY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);

        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            double[] newPos = warpMesh.interpolatePosition(idxWeights);
            gpsDistortedX = (float) newPos[0];
            gpsDistortedY = (float) newPos[1];
        }

        // transform gps marker
        float[] point = new float[]{gpsDistortedX, gpsDistortedY};
        Matrix gpsMarkTransform = new Matrix();
        gpsMarkTransform.postTranslate(-(gpsMarkerWidth / 2), -(gpsMarkerHeight / 2 + gpsDirectionHeight));
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);
    }

    @Override
    protected void onResume() {
        super.onResume();

        googleApiClient.connect();

        if (currentLocation != null) {
            handleLocationChange(currentLocation);
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

    /**
     * Builds a GoogleApiClient.
     */
    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Implementation for Google Services Location API.
     */
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

    /**
     * Implementation for Google Services Location API.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(), errorCode: " + String.valueOf(cause));

    }

    /**
     * Implementation for Google Services Location API.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(), errorCode: " + connectionResult.getErrorCode());
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        handleLocationChange(location);
    }

//
//    float[] point = new float[]{0, 0}; // tourist map position
//    float latCenter = lat;
//    float lngCenter = lng;
//    Matrix temp = new Matrix();
//            temp.set(transformMat);
//
//    // calculate lat lng
//            temp.postTranslate(-screenWidth / 2, -screenHeight / 2);
//            temp.postRotate(-rotation);
//            temp.postTranslate(screenWidth / 2, screenHeight / 2);
//            temp.mapPoints(point);
//    IdxWeights idxWeights = warpMesh.getPointInTriangleIdx((screenWidth / 2 - point[0]) / scale, (screenHeight / 2 - point[1]) / scale);
//            if (idxWeights.idx >= 0) {
//        double[] newPos = realMesh.interpolatePosition(idxWeights);
//        lngCenter = (float) (newPos[0] / realMesh.mapWidth * (realMesh.maxLon - realMesh.minLon) + realMesh.minLon);
//        latCenter = (float) (realMesh.maxLat - newPos[1] / realMesh.mapHeight * (realMesh.maxLat - realMesh.minLat));
//    }
//
//    // close menu_search
//            floatingActionsMenu.collapse();
//
//    // upload audio
//    AsyncHttpClient client = new AsyncHttpClient();
//    RequestParams params = new RequestParams();
//            params.setForceMultipartEntityContentType(true);
//            try {
//        File audioFile = new File(filename);
//        if (audioFile.exists())
//            params.put("file", audioFile);
//        params.put("mapTag", mapTag);
//        params.put("location", "null");
//        params.put("description", "null");
//        params.put("lat", latCenter);
//        params.put("lng", lngCenter);
//        params.put("type", "audio");
//
//        client.post("https://itour-lobst3rd.c9users.io/upload", params, new AsyncHttpResponseHandler() {
//            @Override
//            public void onStart() {
//            }
//
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
//                Toast.makeText(context, "上傳失敗, 網路錯誤QQ", Toast.LENGTH_LONG).show();
//            }
//        });
//
//    } catch (FileNotFoundException e) {
//        e.printStackTrace();
}
