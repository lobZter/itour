package nctu.cs.cgv.itour.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nctu.cs.cgv.itour.ArrayAdapterSearchView;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.CheckinActivity;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.activity.SpotInfoActivity;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.ImageNode;
import nctu.cs.cgv.itour.object.MergedCheckinNode;
import nctu.cs.cgv.itour.object.Node;
import nctu.cs.cgv.itour.object.SpotNode;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.edgeNode;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.dpToPx;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    // constants
    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 6.0f;
    private final float ZOOM_THRESHOLD = 2.2f;
    private final float FOG_UPDATE_THRESHOLD = 5.0f;
    private final int CLUSTER_THRESHOLD = 20500;
    private final int nodeIconWidth = 16;
    private final int nodeIconHeight = 16;
    private final int checkinIconWidth = 64;
    private final int checkinIconHeight = 64;
    private Context context;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float gpsDistortedX = 0;
    private float gpsDistortedY = 0;
    private float lastFogClearPosX = 0;
    private float lastFogClearPosY = 0;
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
    private View seperator;
    private ImageView touristMap;
    private ImageView fogMap;
    private LinearLayout gpsMarker;
    private FloatingActionButton gpsBtn;
    private FloatingActionButton addBtn;
    private Bitmap fogBitmap;
    private ActionBar actionBar;
    // objects
    private List<ImageNode> edgeNodeList;
    private List<ImageNode> pathEdgeNodeList;
    private Map<String, SpotNode> spotNodeMap;
    private List<SpotNode> spotNodeList;
    private List<ImageNode> checkinNodeList;
    private List<MergedCheckinNode> mergedCheckinNodeList;
    private LayoutInflater inflater;
    // gestures
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    // settings
    private SharedPreferences preferences;
    // flags
    private boolean isGpsCurrent = false;
    private boolean isOrientationCurrent = true;
    private boolean isMerged = true;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // init objects
        edgeNodeList = new ArrayList<>();
        pathEdgeNodeList = new ArrayList<>();
        checkinNodeList = new ArrayList<>();
        mergedCheckinNodeList = new ArrayList<>();
        transformMat = new Matrix();
        inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        rootLayout = (RelativeLayout) view.findViewById(R.id.parent_layout);
        seperator = view.findViewById(R.id.seperator);
        gpsMarker = (LinearLayout) view.findViewById(R.id.gps_marker);
        gpsBtn = (FloatingActionButton) view.findViewById(R.id.btn_gps);
        addBtn = (FloatingActionButton) view.findViewById(R.id.btn_add);
        FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.touristmap);

        // set subtitle
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setSubtitle("Map");

        // set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        int touristMapWidth = touristMapBitmap.getWidth();
        int touristMapHeight = touristMapBitmap.getHeight();
        touristMap = new ImageView(context);
        touristMap.setLayoutParams(new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight));
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        frameLayout.addView(touristMap);

        // draw fog
        fogBitmap = Bitmap.createBitmap(touristMapWidth, touristMapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fogBitmap);
        canvas.drawARGB(120, 0, 0, 0);
        fogMap = new ImageView(context);
        fogMap.setLayoutParams(new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight));
        fogMap.setScaleType(ImageView.ScaleType.MATRIX);
        fogMap.setImageBitmap(fogBitmap);
        fogMap.setPivotX(0);
        fogMap.setPivotY(0);
        frameLayout.addView(fogMap);

        // draw edge distance indicator
        edgeNodeList = edgeNode.getNodeList();
        for (ImageNode imageNode : edgeNodeList) {
            addEdgeNode(imageNode, "black");
        }

        // draw spots
        spotIconPivotX = (int) getResources().getDimension(R.dimen.spot_icon_width) / 2;
        spotIconPivotY = (int) getResources().getDimension(R.dimen.spot_icon_height) / 2;
        spotNodeMap = new LinkedHashMap<>(spotList.nodeMap);
        for (Map.Entry<String, SpotNode> nodeEntry : spotNodeMap.entrySet()) {
            addSpotNode(nodeEntry.getValue());
        }
        spotNodeList = new ArrayList<>(spotNodeMap.values());

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
                    translateToPos(gpsDistortedX, gpsDistortedY, true);
                else if (!isOrientationCurrent)
                    rotateToNorth();
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, CheckinActivity.class));
            }
        });

        setTouchListener();

        setHasOptionsMenu(true);

        switchFog(preferences.getBoolean("fog", false));
        switchDistanceIndicator(preferences.getBoolean("distance_indicator", false));

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                mapCenterX = rootLayout.getWidth() / 2;
                mapCenterY = rootLayout.getHeight() / 5 * 2;
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // set search view autocomplete
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_search, new ArrayList<>(spotList.getSpotsName()));
        final ArrayAdapterSearchView searchView = (ArrayAdapterSearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setAdapter(adapter);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String autocompleteStr = adapter.getItem(position);
                searchView.clearFocus();
                searchView.setText(autocompleteStr);
                Node node = spotList.nodeMap.get(autocompleteStr);
                translateToPos(node.x, node.y, false);
                actionLog("Search for " + autocompleteStr);
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (actionBar != null) {
            if (getUserVisibleHint()) {
                actionBar.setSubtitle("Map");
            }
        }
    }

    private void setTouchListener() {
        gestureDetector = new GestureDetector(
                context, new GestureDetector.SimpleOnGestureListener() {

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
                context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

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

    private void reRender() {

        Matrix gpsMarkTransform = new Matrix();
        Matrix nodeIconTransform = new Matrix();
        Matrix spotIconTransform = new Matrix();
        Matrix checkinIconTransform = new Matrix();
        gpsMarkTransform.postTranslate(-gpsMarkerPivotX, -gpsMarkerPivotY);
        spotIconTransform.postTranslate(-spotIconPivotX, -spotIconPivotY);
        nodeIconTransform.postTranslate(-nodeIconWidth / 2, -nodeIconHeight / 2);
        checkinIconTransform.postTranslate(-checkinIconWidth / 2, -checkinIconHeight);
        float[] point = new float[]{0, 0};
        transformMat.mapPoints(point);

        // transform tourist map (ImageView)
        touristMap.setScaleX(scale);
        touristMap.setScaleY(scale);
        touristMap.setRotation(rotation);
        touristMap.setTranslationX(point[0]);
        touristMap.setTranslationY(point[1]);
        // transform fog map
        fogMap.setScaleX(scale);
        fogMap.setScaleY(scale);
        fogMap.setRotation(rotation);
        fogMap.setTranslationX(point[0]);
        fogMap.setTranslationY(point[1]);

        // transform gpsMarker
        point[0] = gpsDistortedX;
        point[1] = gpsDistortedY;
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);

        // transform nodeImage
        for (ImageNode imageNode : edgeNodeList) {
            point[0] = imageNode.x;
            point[1] = imageNode.y;
            transformMat.mapPoints(point);
            nodeIconTransform.mapPoints(point);
            imageNode.icon.setTranslationX(point[0]);
            imageNode.icon.setTranslationY(point[1]);
        }
        for (ImageNode imageNode : pathEdgeNodeList) {
            point[0] = imageNode.x;
            point[1] = imageNode.y;
            transformMat.mapPoints(point);
            nodeIconTransform.mapPoints(point);
            imageNode.icon.setTranslationX(point[0]);
            imageNode.icon.setTranslationY(point[1]);
        }

        // transform spot
        for (SpotNode spotNode : spotNodeMap.values()) {
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }

        // transform mergedCheckinNode
        for (ImageNode imageNode : checkinNodeList) {
            point[0] = imageNode.x;
            point[1] = imageNode.y;
            transformMat.mapPoints(point);
            checkinIconTransform.mapPoints(point);
            imageNode.icon.setTranslationX(point[0]);
            imageNode.icon.setTranslationY(point[1]);
        }

        for (MergedCheckinNode mergedCheckinNode : mergedCheckinNodeList) {
            point[0] = mergedCheckinNode.x;
            point[1] = mergedCheckinNode.y;
            transformMat.mapPoints(point);
            Matrix mergedCheckinIconTransform = new Matrix();
            mergedCheckinIconTransform.postTranslate(-dpToPx(context, 32 / 2), -dpToPx(context, 32));
            mergedCheckinIconTransform.mapPoints(point);
            mergedCheckinNode.icon.setTranslationX(point[0]);
            mergedCheckinNode.icon.setTranslationY(point[1]);
        }

        isMerged = scale < ZOOM_THRESHOLD;

        if (isMerged) {
            for (ImageNode imageNode : checkinNodeList) {
                imageNode.icon.setVisibility(View.GONE);
            }

            for (MergedCheckinNode mergedCheckinNode : mergedCheckinNodeList) {
                mergedCheckinNode.icon.setVisibility(View.VISIBLE);
            }

            for (int i = spotList.primarySpotMaxIdx + 1; i < spotNodeList.size(); i++) {
                SpotNode spotNode = spotNodeList.get(i);
                spotNode.icon.setVisibility(View.GONE);
            }

        } else {
            for (ImageNode imageNode : checkinNodeList) {
                imageNode.icon.setVisibility(View.VISIBLE);
            }

            for (MergedCheckinNode mergedCheckinNode : mergedCheckinNodeList) {
                mergedCheckinNode.icon.setVisibility(View.GONE);
            }

            for (int i = spotList.primarySpotMaxIdx + 1; i < spotNodeList.size(); i++) {
                SpotNode spotNode = spotNodeList.get(i);
                spotNode.icon.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addSpotNode(final SpotNode spotNode) {
        View icon = inflater.inflate(R.layout.item_spot, null);
        spotNode.icon = icon;
        spotNode.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SpotInfoActivity.class);
                intent.putExtra("spotName", spotNode.name);
                startActivity(intent);
                actionLog("Browse Spot: " + spotNode.name);
            }
        });
        ((TextView) spotNode.icon.findViewById(R.id.spot_name)).setText(spotNode.name);
        rootLayout.addView(icon, rootLayout.indexOfChild(seperator));
    }

    private void addEdgeNode(ImageNode imageNode, String iconColor) {
        imageNode.icon = new ImageView(context);
        if (iconColor.equals("blue"))
            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_trans);
        if (iconColor.equals("black"))
            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_black_trans);
        imageNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
        rootLayout.addView(imageNode.icon, rootLayout.indexOfChild(seperator));
    }

    public void showPathIdicator(SpotNode spotNode) {
        for (ImageNode imageNode : pathEdgeNodeList) {
            rootLayout.removeView(imageNode.icon);
        }

        EdgeNode.Vertex from = edgeNode.findVertex(gpsDistortedX, gpsDistortedY);
        EdgeNode.Vertex to = edgeNode.findVertex(spotNode.x, spotNode.y);
        edgeNode.shortestPath(from, to);
        pathEdgeNodeList = edgeNode.getPathNodeList();
        for (ImageNode imageNode : pathEdgeNodeList) {
            addEdgeNode(imageNode, "blue");
        }
    }

    private void showDialog(Checkin checkin) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        CheckinDialogFragment checkinDialogFragment = CheckinDialogFragment.newInstance(checkin.key);
        checkinDialogFragment.show(fragmentManager, "fragment_audio_checkin_dialog");

        actionLog("Browse Checkin: " + checkin.location);
    }

    public void addCheckin(final Checkin checkin) {
        float[] imgPx = gpsToImgPx(Float.valueOf(checkin.lat), Float.valueOf(checkin.lng));
        ImageNode checkinNode = new ImageNode(imgPx[0], imgPx[1]);
        checkinNodeList.add(checkinNode);

        // create icon ImageView
        checkinNode.icon = new ImageView(context);
        checkinNode.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(checkin);
            }
        });
        checkinNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));
        ((ImageView) checkinNode.icon).setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location_on_red_600_24dp));
        rootLayout.addView(checkinNode.icon, rootLayout.indexOfChild(seperator) + 1);

        addMergedCheckin(checkin.location, imgPx[0], imgPx[1]);
    }

    public void addCheckins() {

        for (Map.Entry<String, Checkin> entry : ((MainActivity) getActivity()).checkinMap.entrySet()) {
            Checkin checkin = entry.getValue();
            addCheckin(checkin);
        }
        reRender();
    }

    public void removeCheckin(final Checkin checkin) {

    }

    private void addMergedCheckin(String spotName, float x, float y) {

        SpotNode spotNode = spotNodeMap.get(spotName);
        if (spotNode != null) {
            // add into spot
            if (spotNode.mergedCheckinNode == null) {
                // no checkin on spot yet
                spotNode.mergedCheckinNode = newMergedCheckin(spotNode.x, spotNode.y, true);
            } else {
                spotNode.mergedCheckinNode.checkinNum++;
                TextView checkinsNumCircle = (TextView) spotNode.mergedCheckinNode.icon.findViewById(R.id.checkin_num);
                checkinsNumCircle.setText(String.valueOf(spotNode.mergedCheckinNode.checkinNum));
            }
        } else {
            // add into cluster
            boolean newCluster = true;
            for (MergedCheckinNode mergedCheckinNode : mergedCheckinNodeList) {
                if (mergedCheckinNode.onSpot) continue;

                double distance = Math.pow(x - mergedCheckinNode.x, 2) + Math.pow(y - mergedCheckinNode.y, 2);
                if (distance < CLUSTER_THRESHOLD) {
                    int clusterSize = mergedCheckinNode.checkinNum;
                    mergedCheckinNode.x = (mergedCheckinNode.x * clusterSize + x) / (clusterSize + 1);
                    mergedCheckinNode.y = (mergedCheckinNode.y * clusterSize + y) / (clusterSize + 1);

                    mergedCheckinNode.checkinNum++;
                    TextView checkinsNumCircle = (TextView) mergedCheckinNode.icon.findViewById(R.id.checkin_num);
                    checkinsNumCircle.setText(String.valueOf(mergedCheckinNode.checkinNum));

                    newCluster = false;
                    break;
                }
            }

            // cluster not found, create a new one
            if (newCluster) {
                newMergedCheckin(x, y, false);
            }
        }
    }

    private MergedCheckinNode newMergedCheckin(float x, float y, boolean onSpot) {
        MergedCheckinNode mergedCheckinNode = new MergedCheckinNode(x, y);
        mergedCheckinNode.icon = inflater.inflate(R.layout.item_bigcheckin, null);
        mergedCheckinNode.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateToView(v);
            }
        });
        mergedCheckinNode.onSpot = onSpot;
        rootLayout.addView(mergedCheckinNode.icon);
        mergedCheckinNodeList.add(mergedCheckinNode);
        return mergedCheckinNode;
    }

    public void translateToPos(final float x, final float y, final boolean toCurrent) {

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                float[] point = new float[]{x, y};
                transformMat.mapPoints(point);
                float distanceToCenterX = mapCenterX - point[0];
                float distanceToCenterY = mapCenterY - point[1];
                float scaleTo22 = 2.2f - scale;

                if (Math.abs(distanceToCenterX) <= Math.abs(30) || Math.abs(distanceToCenterY) <= Math.abs(30)) {
                    transformMat.postTranslate(distanceToCenterX, distanceToCenterY);
                    if (scale < 2.2) {
                        transformMat.postTranslate(-point[0], -point[1]);
                        transformMat.postScale(2.2f / scale, 2.2f / scale);
                        transformMat.postTranslate(point[0], point[1]);
                        scale = 2.2f;
                    }
                    reRender();
                    translationHandler.removeCallbacks(this);
                    if(toCurrent) {
                        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
                        isGpsCurrent = true;
                    } else {
                        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
                        isGpsCurrent = false;
                    }
                } else {
                    transformMat.postTranslate(distanceToCenterX / 5, distanceToCenterY / 5);
                    if (scale < 2.2) {
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

    private void translateToView(final View view) {
        final float transX = mapCenterX - view.getTranslationX();
        final float transY = mapCenterY - view.getTranslationY();
        final float deltaTransX = transX / 10f;
        final float deltaTransY = transY / 10f;
        final float deltaScale = (2.2f - scale) / 10f;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(mapCenterX - view.getTranslationX()) <= Math.abs(deltaTransX) ||
                        Math.abs(mapCenterY - view.getTranslationY()) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(
                            mapCenterX - view.getTranslationX(),
                            mapCenterY - view.getTranslationY());

                    if (scale < 2.2) {
                        transformMat.postTranslate(-view.getTranslationX(), -view.getTranslationY());
                        transformMat.postScale(2.2f / scale, 2.2f / scale);
                        transformMat.postTranslate(view.getTranslationX(), view.getTranslationY());
                        scale = 2.2f;
                    }

                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);

                    if (scale < 2.2) {
                        transformMat.postTranslate(-view.getTranslationX(), -view.getTranslationY());
                        transformMat.postScale((scale + deltaScale) / scale, (scale + deltaScale) / scale);
                        transformMat.postTranslate(view.getTranslationX(), view.getTranslationY());
                        scale += deltaScale;
                    }

                    reRender();
                    if (Math.abs(mapCenterX - view.getTranslationX()) < 300) {
                        // slow down
                        translationHandler.postDelayed(this, 5);
                    } else {
                        translationHandler.postDelayed(this, 2);
                    }
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 2);
        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
    }

    public void rotateToNorth() {
        final Handler rotationHandler = new Handler();
        Runnable rotationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rotation) <= Math.abs(6)) {
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

    public void handleLocationChange(float lat, float lng) {

        float[] imgPx = gpsToImgPx(lat, lng);

        gpsDistortedX = imgPx[0];
        gpsDistortedY = imgPx[1];

        reRender();

        // check whether it should update fog or not
        double distance = Math.sqrt(Math.pow(lastFogClearPosX - gpsDistortedX, 2.0) + Math.pow(lastFogClearPosY - gpsDistortedY, 2.0));
        if (distance > FOG_UPDATE_THRESHOLD) {
            // update fog map
            Paint paint = new Paint();
            paint.setColor(Color.TRANSPARENT);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));
            Canvas canvas = new Canvas(fogBitmap);
            canvas.drawCircle(gpsDistortedX, gpsDistortedY, 25, paint);
            fogMap.postInvalidate();

            lastFogClearPosX = gpsDistortedX;
            lastFogClearPosY = gpsDistortedY;
        }
    }

    public void handleSensorChange(float rotation) {
        final float RADIAN = 57.296f;
        if (gpsMarker != null) {
            gpsMarker.setRotation(rotation * RADIAN);
        }
    }

    public void switchFog(boolean flag) {
        if (flag) {
            fogMap.setVisibility(View.VISIBLE);
        } else {
            fogMap.setVisibility(View.GONE);
        }
    }

    public void switchDistanceIndicator(boolean flag) {
        if (flag) {
            for (ImageNode imageNode : edgeNodeList) {
                imageNode.icon.setVisibility(View.VISIBLE);
            }
        } else {
            for (ImageNode imageNode : edgeNodeList) {
                imageNode.icon.setVisibility(View.GONE);
            }
        }
    }
}
