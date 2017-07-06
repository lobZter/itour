package nctu.cs.cgv.itour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.map.IdxWeights;
import nctu.cs.cgv.itour.map.Mesh;
import nctu.cs.cgv.itour.map.RotationGestureDetector;

import static nctu.cs.cgv.itour.MyApplication.dirPath;

public class MapActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "MapActivity";
    private String mapTag;
    public LinkedList<Float> nodeList;
    public LinkedList<Float> edgeList;
    public LinkedList<ImageView> nodeImageList;
    private float MIN_ZOOM = 1.0f;
    private float MAX_ZOOM = 6.0f;
    private float parentLayoutWidth = 0f, parentLayoutHeight = 0f;
    private float touristMapWidth = 0f, touristMapHeight = 0f;
    private Matrix transformMat;
    private double latitude = 0, longitude = 0;
    private float gpsDistortedX = 0, gpsDistortedY = 0;
    private float prevFocusX = 0, prevFocusY = 0;
    private float scale = 1f;
    private float rotation = 0f;
    int vertexNumber = 0;
    float edgeRatioMin = 0;
    float headStdX = 0;
    float headStdY = 0;
    float tailStdX = 0;
    float tailStdY = 0;
    float distanceRatioRealLength = 0;
    float distanceRatioStandard = 0;
    float edgePixelLengthStd = 0;
    float edgeRealLengthStd = 0;
    //    private Edge edgeNode;
    Boolean meshReady = false;
    Boolean realMapReady = false;
    Boolean warpMeshReady = false;
    private FloatingActionButton floatingActionButton;
    private FloatingActionButton compassButton;
    private FloatingActionButton checkinButton;
    private RelativeLayout parentLayout;
    private FrameLayout touristMap;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    // Google Services Location API
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location currentLocation;
    // device sensor manager
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private SensorEventListener sensorEventListener = new SensorEventListener() {

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
    private ImageView gpsMarker;
    private Mesh realMesh;
    private Mesh warpMesh;
    private Bitmap fogBitmap;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        Intent intent = getIntent();
//        mapTag = intent.getStringExtra("MAP");
        mapTag = "Tamsui";

        // Set Location API.
        buildGoogleApiClient();
        createLocationRequest();

        // Set Sensors.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        transformMat = new Matrix();

        String touristMapPath = dirPath + mapTag + "_distorted_map.png";
        Log.d(TAG, touristMapPath);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        final Bitmap touristMapBitmap = BitmapFactory.decodeFile(touristMapPath, options);
        final Drawable touristMapDrawable = new BitmapDrawable(getResources(), touristMapBitmap);
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        fogBitmap = Bitmap.createBitmap(touristMapBitmap.getWidth(), touristMapBitmap.getHeight(), config);
        Canvas fogCanvas = new Canvas(fogBitmap);
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();

        touristMap = new FrameLayout(this);
        touristMap.setPivotX(0f);
        touristMap.setPivotY(0f);
        parentLayout = (RelativeLayout) findViewById(R.id.parent_layout);
        parentLayout.addView(touristMap);


        nodeList = new LinkedList<Float>();
        edgeList = new LinkedList<Float>();
        nodeImageList = new LinkedList<ImageView>();
//        drawEdgeNode(parentLayout);
        setTouchListener();

        String realMeshDir = dirPath + mapTag + "_mesh.txt";
        String warpMeshDir = dirPath + mapTag + "_warpMesh.txt";
        String boundBoxDir = dirPath + mapTag + "_bound_box.txt";

        File realMeshFile = new File(realMeshDir);
        File boundBoxFile = new File(boundBoxDir);
        if (realMeshFile.exists()) {
            realMesh = new Mesh(realMeshFile);
            meshReady = true;
            if (boundBoxFile.exists()) {
                realMesh.readBoundingBox(boundBoxFile);
                realMapReady = true;
            }
        }

        File warpMeshFile = new File(warpMeshDir);
        if (warpMeshFile.exists()) {
            warpMesh = new Mesh(warpMeshFile);
            warpMeshReady = true;
        }

        gpsMarker = new ImageView(this);
        gpsMarker.setImageDrawable(getResources().getDrawable(R.drawable.circle));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(48, 48);
        gpsMarker.setLayoutParams(lp);
        parentLayout.addView(gpsMarker);

        findViewById(R.id.scrim_status_bar).bringToFront();
        findViewById(R.id.scrim_navigation_bar).bringToFront();

        floatingActionButton = (FloatingActionButton) findViewById(R.id.btn_nav);
        floatingActionButton.bringToFront();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateToCurrent();
            }
        });
        floatingActionButton.setVisibility(View.GONE);

        compassButton = (FloatingActionButton) findViewById(R.id.btn_compass);
        compassButton.bringToFront();
        compassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateToNorth();
            }
        });
        compassButton.setVisibility(View.GONE);

        checkinButton = (FloatingActionButton) findViewById(R.id.btn_checkin);
        checkinButton.bringToFront();
        checkinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, CheckinActivity.class);
                intent.putExtra("lat", latitude);
                intent.putExtra("lng", longitude);
                startActivity(intent);
            }
        });
        checkinButton.setVisibility(View.GONE);



//        updateCheckin();

        parentLayout.post(new Runnable() {
            @Override
            public void run() {
                parentLayoutWidth = parentLayout.getMeasuredWidth();
                parentLayoutHeight = parentLayout.getMeasuredHeight();

//                touristMap.setScaleX(touristMapWidth / parentLayoutWidth);
//                touristMap.setScaleY(touristMapHeight / parentLayoutHeight);
                touristMap.setBackground(touristMapDrawable);
            }
        });

        FirebaseMessaging.getInstance().subscribeToTopic("checkin");
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "token: " + token);

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
                prevFocusX = scaleGestureDetector.getFocusX();
                prevFocusY = scaleGestureDetector.getFocusY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                float scaleFactor = scaleGestureDetector.getScaleFactor();
                scaleFactor = MAX_ZOOM >= scale * scaleFactor ? scaleFactor : MAX_ZOOM / scale;
                scaleFactor = MIN_ZOOM <= scale * scaleFactor ? scaleFactor : MIN_ZOOM / scale;
                float focusX = scaleGestureDetector.getFocusX();
                float focusY = scaleGestureDetector.getFocusY();

                float distanceX = focusX - prevFocusX;
                float distanceY = focusY - prevFocusY;

                // postScale
                transformMat.postTranslate(-focusX, -focusY);
                transformMat.postScale(scaleFactor, scaleFactor);
                transformMat.postTranslate(focusX, focusY);
//                transformMat.postTranslate(distanceX, distanceY);
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

        parentLayout.setOnTouchListener(new View.OnTouchListener() {
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

    private void reRender() {
        Matrix nodeIconTransform = new Matrix();
        Matrix gpsMarkTransform = new Matrix();
        nodeIconTransform.postTranslate(-8, -8);
        gpsMarkTransform.postTranslate(-32, -32);

        float[] point = new float[]{0, 0};
        transformMat.mapPoints(point);
        touristMap.setScaleX(scale);
        touristMap.setScaleY(scale);
        touristMap.setRotation(rotation);
        touristMap.setTranslationX(point[0]);
        touristMap.setTranslationY(point[1]);

        for (int i = 0; i < nodeImageList.size(); i++) {
            point[0] = nodeList.get(i * 2);
            point[1] = nodeList.get(i * 2 + 1);
            transformMat.mapPoints(point);
            nodeIconTransform.mapPoints(point);
            nodeImageList.get(i).setTranslationX(point[0]);
            nodeImageList.get(i).setTranslationY(point[1]);
        }
        point[0] = gpsDistortedX;
        point[1] = gpsDistortedY;
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);

    }

    private void translateToCurrent() {
        final float transX = parentLayoutWidth / 2 - gpsMarker.getTranslationX();
        final float transY = parentLayoutHeight / 2 - gpsMarker.getTranslationY();
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(parentLayoutWidth / 2 - gpsMarker.getTranslationX()) < Math.abs(deltaTransX)) {
                    transformMat.postTranslate(
                            parentLayoutWidth / 2 - gpsMarker.getTranslationX(),
                            parentLayoutHeight / 2 - gpsMarker.getTranslationY());
                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);
                    reRender();
                    if (Math.abs(parentLayoutWidth / 2 - gpsMarker.getTranslationX()) < parentLayoutWidth / 4) {
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
        Log.d(TAG, "" + rotation);

        final float deltaAngle = rotation / 10;

        final Handler rotationHandler = new Handler();
        Runnable rotationInterpolation = new Runnable() {
            @Override
            public void run() {
                transformMat.postTranslate(-parentLayoutWidth / 2, -parentLayoutHeight / 2);
                transformMat.postRotate(-deltaAngle);
                transformMat.postTranslate(parentLayoutWidth / 2, parentLayoutHeight / 2);
                rotation -= deltaAngle;
                reRender();
                if (Math.abs(rotation) <= Math.abs(deltaAngle)) {
                    transformMat.postTranslate(-parentLayoutWidth / 2, -parentLayoutHeight / 2);
                    transformMat.postRotate(-rotation);
                    transformMat.postTranslate(parentLayoutWidth / 2, parentLayoutHeight / 2);
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

    private void drawEdgeNode(RelativeLayout parentView) {
        String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour/";
        String edgePath = dirPath + mapTag + "_edge_length.txt";
        File edgeFile = new File(edgePath);
        readEdgeFile(edgeFile);
        readData();

        for (int i = 0; i < nodeList.size(); i += 2) {
            ImageView nodeImage = new ImageView(this);
            nodeImage.setImageDrawable(getResources().getDrawable(R.drawable.ftprint_trans));
            nodeImage.setLayoutParams(new FrameLayout.LayoutParams(16, 16));
            parentView.addView(nodeImage);
            nodeImage.setTranslationX(nodeList.get(i) - 8);
            nodeImage.setTranslationY(nodeList.get(i + 1) - 8);

            nodeImageList.add(nodeImage);
        }
    }

    private boolean readEdgeFile(File edgeFile) {

        edgeList.clear();
        try {
            //InputStream inputStream = context.getResources().openRawResource(resourceId);
            FileInputStream inputStream = new FileInputStream(edgeFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // first line
            String nextLine = bufferedReader.readLine();
            vertexNumber = Integer.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgeRatioMin = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            headStdX = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            headStdY = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            tailStdX = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            tailStdY = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgePixelLengthStd = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgeRealLengthStd = Float.valueOf(nextLine);

            //Log.i("TAG", "vertex number: " + vertexNumber + "," + edgeRatioMin + "," + headStandardX + "," + headStandardY + "," + tailStandardX + "," + tailStandardY);

            // read vertex positions
            for (int i = 0, vIter = 0; i < vertexNumber / 2; i++, vIter += 2) {
                nextLine = bufferedReader.readLine();

                float x = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y = Float.valueOf(nextLine);

                nextLine = bufferedReader.readLine();
                float x2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgePixelLength = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgeRealLength = Float.valueOf(nextLine);

                edgeList.add(x);
                edgeList.add(y);
                edgeList.add(x2);
                edgeList.add(y2);
                edgeList.add(edgePixelLength);
                edgeList.add(edgeRealLength);
            }

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception read file edge ...");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean readData() {
        float x, y, x2, y2, edgePixelLength, edgeRealLength, distanceToAddorSubtractX, distanceToAddorSubtractY;
        float mScaleFactor = 1f;

        if (scale <= 1) mScaleFactor = 2f;
        else if (scale > 1 && scale <= 1.3)
            mScaleFactor = 2.3f;
        else if (scale > 1.3 && scale <= 1.8)
            mScaleFactor = 2.6f;
        else if (scale > 1.8 && scale <= 2.7)
            mScaleFactor = 3.2f;
        else if (scale > 2.7 && scale <= 3)
            mScaleFactor = 3.8f;
        else if (scale > 3 && scale <= 3.5)
            mScaleFactor = 4.3f;
        else if (scale > 3.5 && scale <= 3.9)
            mScaleFactor = 4.8f;
        else if (scale > 3.9 && scale <= 4.3)
            mScaleFactor = 5.3f;
        else if (scale > 4.3 && scale <= 5)
            mScaleFactor = 5.9f;
        else if (scale > 5 && scale <= 5.5)
            mScaleFactor = 6.1f;
        else if (scale > 5.5 && scale <= 6)
            mScaleFactor = 6.6f;
        else if (scale > 6 && scale <= 6.5)
            mScaleFactor = 7.1f;
        else if (scale > 6.5 && scale <= 7)
            mScaleFactor = 7.6f;
        else if (scale > 7 && scale <= 7.5)
            mScaleFactor = 8.3f;
        else if (scale > 7.5) mScaleFactor = 8.3f;

        float width = 36f / mScaleFactor;
        float height = 36f / mScaleFactor;

        nodeList.clear();

        float headStandardX = headStdX;
        float headStandardY = headStdY;
        float tailStandardX = tailStdX;
        float tailStandardY = tailStdY;
        float edgePixelLengthStandard = edgePixelLengthStd;
        float edgeRealLengthStandard = edgeRealLengthStd;

        float distanceRatioStandard = edgePixelLengthStandard / (1.2f * width);
        float distanceVectorXStandard = Math.abs((headStandardX - tailStandardX));
        float distanceVectorYStandard = Math.abs((headStandardY - tailStandardY));
        float distanceToAddorSubtractXStandard = distanceVectorXStandard / distanceRatioStandard;
        float distanceToAddorSubtractYStandard = distanceVectorYStandard / distanceRatioStandard;
        float distanceRatioRealLength = distanceRatioStandard * edgeRealLengthStandard / edgePixelLengthStandard;
        //Log.i("TAG", "Node standard: " + distanceRatioRealLength + "," + distanceRatioStandard + "," + distanceVectorXStandard + "," + distanceVectorYStandard + "," + distanceToAddorSubtractXStandard + "," + distanceToAddorSubtractYStandard);
        for (int i = 0; i < edgeList.size(); i += 6) {
            x = edgeList.get(i);
            y = edgeList.get(i + 1);
            x2 = edgeList.get(i + 2);
            y2 = edgeList.get(i + 3);
            edgePixelLength = edgeList.get(i + 4);
            edgeRealLength = edgeList.get(i + 5);

            float distanceRatio = (distanceRatioRealLength * edgePixelLength / edgeRealLength);
            float distanceVectorX = Math.abs(x - x2);
            float distanceVectorY = Math.abs(y - y2);
//            distanceToAddorSubtractX = distanceVectorX / (edgePixelLength / distanceRatio);
//            distanceToAddorSubtractY = distanceVectorY / (edgePixelLength / distanceRatio);
            if (distanceVectorX > 80 || distanceVectorY > 80) distanceRatio = distanceRatio * 10;

            distanceToAddorSubtractX = (distanceVectorX / distanceRatio);
            distanceToAddorSubtractY = (distanceVectorY / distanceRatio);
            //Log.i("TAG", "Node normal: " + distanceRatio + "," + distanceVectorX + "," + distanceVectorY + "," + distanceToAddorSubtractX + "," + distanceToAddorSubtractY);
            nodeList.add(x);
            nodeList.add(y);

            if ((x == headStandardX && y == headStandardY && x2 == tailStandardX && y2 == tailStandardY) || (x2 == headStandardX && y2 == headStandardY && x == tailStandardX && y == tailStandardY)) {
                if (headStandardX >= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        nodeList.add(headStandardX - distanceToAddorSubtractXStandard);
                        nodeList.add(headStandardY - distanceToAddorSubtractYStandard);
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX >= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        nodeList.add(headStandardX - distanceToAddorSubtractXStandard);
                        nodeList.add(headStandardY + distanceToAddorSubtractYStandard);
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        nodeList.add(headStandardX + distanceToAddorSubtractXStandard);
                        nodeList.add(headStandardY + distanceToAddorSubtractYStandard);
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        nodeList.add(headStandardX + distanceToAddorSubtractXStandard);
                        nodeList.add(headStandardY - distanceToAddorSubtractYStandard);
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                }
            } else {
                if (x >= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x - distanceToAddorSubtractX >= x2) {
                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            nodeList.add((x - distanceToAddorSubtractX) + (width / 2));
                            nodeList.add((y - distanceToAddorSubtractY) + (height / 2));
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                } else if (x >= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x - distanceToAddorSubtractX >= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {

                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            nodeList.add(x - distanceToAddorSubtractX);
                            nodeList.add(y + distanceToAddorSubtractY);
                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
                    //Log.i("TAG", "AddSub: " + distanceVectorX/distanceToAddorSubtractX +"," + distanceVectorX%distanceToAddorSubtractX  + "," + distanceVectorY/distanceToAddorSubtractY + "," + distanceVectorY%distanceToAddorSubtractY);
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x + distanceToAddorSubtractX <= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            nodeList.add(x + distanceToAddorSubtractX);
                            nodeList.add(y + distanceToAddorSubtractY);
                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x + distanceToAddorSubtractX <= x2) {
//                        if(y- (y - distanceToAddorSubtractY) <width && (x + distanceToAddorSubtractX) - x < width) {
////                        if((y - distanceToAddorSubtractY + (width/2) > y-(width/2))&&(x + distanceToAddorSubtractX - (width/2) < x+(width/2))  ) {

                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            nodeList.add(x + distanceToAddorSubtractX);
                            nodeList.add(y - distanceToAddorSubtractY);
                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                }
            }

        }
        return true;
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String lat = intent.getStringExtra("lat");
            String lng = intent.getStringExtra("lng");
            Float latDistored = 0f, lngDistored = 0f;
            Log.d("receiver", "Got message: " + lat + ", " + lng);
            ImageView nodeImage = new ImageView(MapActivity.this);
            nodeImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle_red_400_24dp));
            nodeImage.setLayoutParams(new FrameLayout.LayoutParams(64, 64));
            parentLayout.addView(nodeImage);

            if (meshReady && warpMeshReady) {
                double imgX = realMesh.mapWidth * (Float.parseFloat(lng) - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
                double imgY = realMesh.mapHeight * (realMesh.maxLat - Float.parseFloat(lat)) / (realMesh.maxLat - realMesh.minLat);

                IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
                if (idxWeights.idx >= 0) {
                    double[] newPos = warpMesh.interpolatePosition(idxWeights);
                    lngDistored = (float) newPos[0];
                    latDistored = (float) newPos[1];
                }
            }

            nodeList.add(lngDistored);
            nodeList.add(latDistored);

            Matrix chekInIconTransform = new Matrix();
            chekInIconTransform.postTranslate(-32, -32);
            float[] point = new float[]{lngDistored, latDistored};
            transformMat.mapPoints(point);
            chekInIconTransform.mapPoints(point);
            nodeImage.setTranslationX(point[0]);
            nodeImage.setTranslationY(point[1]);

            nodeImageList.add(nodeImage);

        }
    };

    private void updateCheckin() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("checkin");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    private void handleLocationChange(Location currentLocation) {
        latitude = currentLocation.getLatitude();
        longitude = currentLocation.getLongitude();

        if (meshReady && warpMeshReady) {

            double imgX = realMesh.mapWidth * (longitude - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
            double imgY = realMesh.mapHeight * (realMesh.maxLat - latitude) / (realMesh.maxLat - realMesh.minLat);

            IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
            if (idxWeights.idx >= 0) {
                double[] newPos = warpMesh.interpolatePosition(idxWeights);
                gpsDistortedX = (float) newPos[0];
                gpsDistortedY = (float) newPos[1];
            }
        }
        Matrix gpsMarkTransform = new Matrix();
        gpsMarkTransform.postTranslate(-32, -32);
        float[] point = new float[]{gpsDistortedX, gpsDistortedY};
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));

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
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

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
}
