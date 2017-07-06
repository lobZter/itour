package nctu.cs.cgv.itour;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import nctu.cs.cgv.itour.map.RotationGestureDetector;

import static nctu.cs.cgv.itour.MyApplication.dirPath;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private String mapTag;
    // view objects
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private FloatingSearchView searchBar;
    // gestures
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;

    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private int touristMapWidth = 0;
    private int touristMapHeight = 0;


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
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0, getStatusBarHeight(), 0, 0);
        searchBar.setLayoutParams(layoutParams);
        // TODO: set search bar autocomplete suggestions
        searchBar.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                //get suggestions based on newQuery
//                searchView.swapSuggestions(newSuggestions);
            }
        });


        // load image from disk and set tourist map
        Log.d(TAG, dirPath + mapTag +"_distorted_map.png");
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag +"_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
        touristMap = new ImageView(getContext());
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
        rootLayout.addView(touristMap);


        searchBar.bringToFront();


        transformMat = touristMap.getImageMatrix();

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

//        for (int i = 0; i < nodeImageList.size(); i++) {
//            point[0] = nodeList.get(i * 2);
//            point[1] = nodeList.get(i * 2 + 1);
//            transformMat.mapPoints(point);
//            nodeIconTransform.mapPoints(point);
//            nodeImageList.get(i).setTranslationX(point[0]);
//            nodeImageList.get(i).setTranslationY(point[1]);
//        }
//        point[0] = gpsDistortedX;
//        point[1] = gpsDistortedY;
//        transformMat.mapPoints(point);
//        gpsMarkTransform.mapPoints(point);
//        gpsMarker.setTranslationX(point[0]);
//        gpsMarker.setTranslationY(point[1]);
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
