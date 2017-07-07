package nctu.cs.cgv.itour;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.arlib.floatingsearchview.FloatingSearchView;

import java.io.File;
import java.util.LinkedList;

import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Mesh;

import static nctu.cs.cgv.itour.MyApplication.dirPath;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    // constants
    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    private final int gpsMarkerWidth = 48;
    private final int gpsMarkerHeight = 48;
    private final int nodeIconWidth = 16;
    private final int nodeIconHeight = 16;
    private final int checkinIconWidth = 64;
    private final int checkinIconHeight = 64;
    private String mapTag;
    // views
    private RelativeLayout rootLayout;
    private FloatingSearchView searchBar;
    private ImageView touristMap;
    private ImageView gpsMarker;
    private FloatingActionButton gpsBtn;
    private RelativeLayout.LayoutParams layoutParams;
    // objects
    private LinkedList<Float> nodeList;
    private LinkedList<ImageView> nodeImageList;
    private Mesh realMesh;
    private Mesh warpMesh;
    // gestures
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    private int touristMapWidth = 0;
    private int touristMapHeight = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float lat = 0, lng = 0;
    private float gpsDistortedX = 0, gpsDistortedY = 0;
    private int initialOffsetX = 0, initialOffsetY = 0;
    // flags
    private boolean isGpsCurrent = false;


    public static MapFragment newInstance(String mapTag) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString("mapTag", mapTag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapTag = getArguments().getString("mapTag", "nctu");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        rootLayout = (RelativeLayout) view.findViewById(R.id.parent_layout);

        // set search bar
        searchBar = (FloatingSearchView) view.findViewById(R.id.floating_search_view);
        // set search bar margin-top with status bar height
        layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0, getStatusBarHeight(), 0, 0);
        searchBar.setLayoutParams(layoutParams);
        searchBar.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                // TODO: set search bar autocomplete suggestions
                //get suggestions based on newQuery
//                searchView.swapSuggestions(newSuggestions);
            }
        });

        // load image from disk and set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
        touristMap = new ImageView(getContext());
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setScaleType(ImageView.ScaleType.FIT_START);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        rootLayout.addView(touristMap);

        // draw edge nodes
        EdgeNode edgeNode = new EdgeNode(dirPath + mapTag + "_edge_length.txt");
        nodeList = edgeNode.getNodeList();
        nodeImageList = new LinkedList<>();
        for (int i = 0; i < nodeList.size(); i += 2) {
            ImageView nodeImage = new ImageView(getContext());
            nodeImage.setImageDrawable(getResources().getDrawable(R.drawable.ftprint_trans));
            nodeImage.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
            nodeImage.setTranslationX(nodeList.get(i) - nodeIconWidth / 2);
            nodeImage.setTranslationY(nodeList.get(i + 1) - nodeIconHeight / 2);
            nodeImageList.add(nodeImage);
            rootLayout.addView(nodeImage);
        }

        // set gpsMarker
        gpsMarker = new ImageView(getContext());
        gpsMarker.setImageDrawable(getResources().getDrawable(R.drawable.circle));
        gpsMarker.setLayoutParams(new RelativeLayout.LayoutParams(gpsMarkerWidth, gpsMarkerHeight));
        rootLayout.addView(gpsMarker);

        // init objects
        realMesh = new Mesh(new File(dirPath + mapTag + "_mesh.txt"));
        realMesh.readBoundingBox(new File(dirPath + mapTag + "_bound_box.txt"));
        warpMesh = new Mesh(new File(dirPath + mapTag + "_warpMesh.txt"));

        // calculate screen initial offset
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        initialOffsetY = screenHeight / 2 - touristMapHeight / 2;
        // transform to center vertical
        transformMat = new Matrix();
        transformMat.postTranslate(initialOffsetX, initialOffsetY);
        reRender();

        // set buttons
        gpsBtn = (FloatingActionButton) view.findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGpsCurrent)
                    rotateToNorth();
                else {
                    translateToCurrent();
                }
            }
        });

        searchBar.bringToFront();

        setTouchListener();
    }

    private void setTouchListener() {
        gestureDetector = new GestureDetector(
                getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                transformMat.postTranslate(-distanceX, -distanceY);
                isGpsCurrent = false;
                gpsBtn.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_gps_fixed_black_24dp));
                reRender();

                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(
                getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {

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

    private void reRender() {
        Matrix gpsMarkTransform = new Matrix();
        Matrix nodeIconTransform = new Matrix();
        gpsMarkTransform.postTranslate(-gpsMarkerWidth / 2, -gpsMarkerHeight / 2);
        nodeIconTransform.postTranslate(-nodeIconWidth / 2, -nodeIconHeight / 2);
        float[] point = new float[]{0, 0};

        // change scale type here to prevent image corp
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
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

        // transform nodeImage
        for (int i = 0; i < nodeImageList.size(); i++) {
            point[0] = nodeList.get(i * 2);
            point[1] = nodeList.get(i * 2 + 1);
            transformMat.mapPoints(point);
            nodeIconTransform.mapPoints(point);
            nodeImageList.get(i).setTranslationX(point[0]);
            nodeImageList.get(i).setTranslationY(point[1]);
        }
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
        gpsBtn.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_gps_fixed_blue_24dp));
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

    public void handleLocationChange(Location currentLocation) {

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
        Matrix gpsMarkTransform = new Matrix();
        gpsMarkTransform.postTranslate(-32, -32);
        float[] point = new float[]{gpsDistortedX, gpsDistortedY};
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);
    }

    public void handleCheckinMsg(float lat, float lng) {

        // calculate distorted gps value
        float latDistorted = 0;
        float lngDistorted = 0;
        double imgX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        double imgY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            double[] newPos = warpMesh.interpolatePosition(idxWeights);
            lngDistorted = (float) newPos[0];
            latDistorted = (float) newPos[1];
        }
        nodeList.add(lngDistorted);
        nodeList.add(latDistorted);

        // add new icon ImageView
        ImageView nodeImage = new ImageView(getContext());
        nodeImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle_red_400_24dp));
        nodeImage.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));
        rootLayout.addView(nodeImage);

        // transform to distorted gps value
        Matrix checkInIconTransform = new Matrix();
        checkInIconTransform.postTranslate(-checkinIconWidth/2, -checkinIconHeight/2);
        float[] point = new float[]{lngDistorted, latDistorted};
        transformMat.mapPoints(point);
        checkInIconTransform.mapPoints(point);
        nodeImage.setTranslationX(point[0]);
        nodeImage.setTranslationY(point[1]);
        nodeImageList.add(nodeImage);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
