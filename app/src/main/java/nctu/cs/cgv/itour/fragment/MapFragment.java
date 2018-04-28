package nctu.cs.cgv.itour.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.Utility;
import nctu.cs.cgv.itour.activity.CheckinActivity;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.custom.ArrayAdapterSearchView;
import nctu.cs.cgv.itour.custom.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.CheckinNode;
import nctu.cs.cgv.itour.object.Node;
import nctu.cs.cgv.itour.object.SpotNode;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static nctu.cs.cgv.itour.MyApplication.CLUSTER_THRESHOLD;
import static nctu.cs.cgv.itour.MyApplication.MAX_ZOOM;
import static nctu.cs.cgv.itour.MyApplication.MIN_ZOOM;
import static nctu.cs.cgv.itour.MyApplication.OVERLAP_THRESHOLD;
import static nctu.cs.cgv.itour.MyApplication.ZOOM_THRESHOLD;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.spToPx;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private Context context;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float gpsDistortedX = -1;
    private float gpsDistortedY = -1;
    private int mapCenterX = 0;
    private int mapCenterY = 0;
    private int checkinIconWidth;
    private int checkinIconHeight;
    private int gpsMarkerPivotX;
    private int gpsMarkerPivotY;
    private int checkinIconPivotX;
    private int checkinIconPivotY;
    private int mergedCheckinIconPivotX;
    private int mergedCheckinIconPivotY;
    private int spotIconPivotX;
    private int spotIconPivotY;
    //    private int edgeNodeIconPivotX;
//    private int edgeNodeIconPivotY;
    // UI references
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private ImageView fogMap;
    private LinearLayout gpsMarker;
    private FloatingActionButton gpsBtn;
    private FloatingActionButton addBtn;
    private Bitmap fogBitmap;
    private ActionBar actionBar;
    private View seperator;
    // objects
//    private List<ImageNode> edgeNodeList;
//    private List<ImageNode> pathEdgeNodeList;


    private Map<String, CheckinNode> checkinNodeMap;
    private List<CheckinNode> checkinNodeList;
    private Map<String, CheckinNode> checkinClusterNodeMap;
    private List<CheckinNode> checkinClusterNodeList;


    public Map<String, SpotNode> spotNodeMap;
    private List<SpotNode> spotNodeList;
    private LayoutInflater inflater;
    private Handler translationHandler;
    // gestures
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    // settings
    private SharedPreferences preferences;
    // flags
    private boolean isGpsCurrent = false;
    private boolean isOrientationCurrent = true;
    private boolean checkinSwitch = true;
    private boolean spotSwitch = true;
//    private boolean fogSwitch = false;
//    private boolean edgeLengthSwitch = false;

    private String uid = ""; // Empty string means guest mode(getCurrentUser() == null)

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(context));

        // init objects
//        edgeNodeList = new ArrayList<>();
//        pathEdgeNodeList = new ArrayList<>();
        checkinNodeMap = new HashMap<>();
        checkinNodeList = new ArrayList<>();
        checkinClusterNodeMap = new HashMap<>();
        checkinClusterNodeList = new ArrayList<>();
        transformMat = new Matrix();
        inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        translationHandler = new Handler();

        // init size variables
        checkinIconWidth = (int) getResources().getDimension(R.dimen.checkin_icon_width);
        checkinIconHeight = (int) getResources().getDimension(R.dimen.checkin_icon_height);
        int mergedCheckinIconWidth = (int) getResources().getDimension(R.dimen.merged_checkin_icon_width);
        int mergedCheckinIconHeight = (int) getResources().getDimension(R.dimen.merged_checkin_icon_height);
        int spotIconWidth = (int) getResources().getDimension(R.dimen.spot_icon_width);
        int spotIconHeight = (int) getResources().getDimension(R.dimen.spot_icon_height);
//        int edgeNodeIconWidth = (int) getResources().getDimension(R.dimen.edge_node_width);
//        int edgeNodeIconHeight = (int) getResources().getDimension(R.dimen.edge_node_height);
        int gpsMarkerWidth = (int) getResources().getDimension(R.dimen.gps_marker_width);
        int gpsMarkerHeight = (int) getResources().getDimension(R.dimen.gps_marker_height);
        int gpsDirectionHeight = (int) getResources().getDimension(R.dimen.gps_direction_height);
        int gpsMarkerPadding = (int) getResources().getDimension(R.dimen.gps_marker_padding);

        // init pivot variables
        gpsMarkerPivotX = gpsMarkerWidth / 2 + gpsMarkerPadding;
        gpsMarkerPivotY = gpsDirectionHeight + gpsMarkerHeight / 2 + gpsMarkerPadding;
        checkinIconPivotX = checkinIconWidth / 3;
        checkinIconPivotY = checkinIconHeight;
        mergedCheckinIconPivotX = mergedCheckinIconWidth / 3;
        mergedCheckinIconPivotY = mergedCheckinIconHeight;
        spotIconPivotX = spotIconWidth / 2;
        spotIconPivotY = spToPx(context, 14) / 2;
//        edgeNodeIconPivotX = edgeNodeIconWidth / 2;
//        edgeNodeIconPivotY = edgeNodeIconHeight / 2;

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        rootLayout = view.findViewById(R.id.parent_layout);
        gpsMarker = view.findViewById(R.id.gps_marker);
        gpsBtn = view.findViewById(R.id.btn_gps);
        addBtn = view.findViewById(R.id.btn_add);
        FrameLayout frameLayout = view.findViewById(R.id.touristmap);
        seperator = view.findViewById(R.id.seperator);

        // set subtitle
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setSubtitle("Map");

        // set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + "/" + mapTag + "_distorted_map.png");
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
//        fogBitmap = Bitmap.createBitmap(touristMapWidth, touristMapHeight, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(fogBitmap);
//        canvas.drawARGB(120, 0, 0, 0);
//        fogMap = new ImageView(context);
//        fogMap.setLayoutParams(new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight));
//        fogMap.setScaleType(ImageView.ScaleType.MATRIX);
//        fogMap.setImageBitmap(fogBitmap);
//        fogMap.setPivotX(0);
//        fogMap.setPivotY(0);
//        frameLayout.addView(fogMap);

        // draw edge distance indicator
//        edgeNodeList = edgeNode.getNodeList();
//        for (ImageNode imageNode : edgeNodeList) {
//            addEdgeNode(imageNode, "black");
//        }

        // draw spots
        spotNodeMap = new LinkedHashMap<>(spotList.nodeMap);    // for search query
        spotNodeList = new ArrayList<>(spotNodeMap.values());   // for transformation
        for (SpotNode spotNode : spotNodeList) {
            addSpotNode(spotNode);
        }

        // set gpsMarker
        gpsMarker.setPivotX(gpsMarkerPivotX);
        gpsMarker.setPivotY(gpsMarkerPivotY);

        // set buttons
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gpsMarker.getVisibility() == View.GONE) {
                    Toast.makeText(context, getString(R.string.toast_gps_outside), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isGpsCurrent)
                    translateToImgPx(gpsDistortedX, gpsDistortedY, true);
                else if (!isOrientationCurrent)
                    rotateToNorth();
            }
        });
        if (uid.equals("")) {
            addBtn.setVisibility(View.GONE);
        }
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gpsMarker.getVisibility() == View.GONE) {
                    Toast.makeText(context,
                            getString(R.string.toast_gps_outside) + "\n" + getString(R.string.toast_cannot_checkin),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(context, CheckinActivity.class));
            }
        });

        setTouchListener();

        setHasOptionsMenu(true);

//        switchFog(preferences.getBoolean("fog", false));
//        switchDistanceIndicator(preferences.getBoolean("distance_indicator", false));
        switchSpotIcon(preferences.getBoolean("spot", true));

        // load checkin after map view set.
        ((MainActivity) getActivity()).queryCheckin();
        if (!uid.equals("")) {
            ((MainActivity) getActivity()).querySavedPostId();
        }

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
                Node node = spotList.nodeMap.get(autocompleteStr);
                translateToImgPx(node.x, node.y, false);
                searchView.clearFocus();
                searchView.setText(autocompleteStr);
                // send action log to server
                actionLog("search: ", autocompleteStr, "");
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (getUserVisibleHint()) {
            if (actionBar != null) {
                actionBar.setSubtitle(getString(R.string.subtitle_map));
            }

            if (gpsMarker != null && gpsMarker.getVisibility() == View.GONE) {
                Toast.makeText(context, getString(R.string.toast_gps_outside), Toast.LENGTH_SHORT).show();
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
                translationHandler.removeCallbacksAndMessages(null);
                boolean res = false;
                res |= scaleGestureDetector.onTouchEvent(event);
                res |= rotationGestureDetector.onTouchEvent(event);
                res |= gestureDetector.onTouchEvent(event);
                return res;
            }
        });
    }

    private void reRender() {
        reRender(true);
    }

    private void reRender(boolean performMerge) {

        boolean isMerged = performMerge && scale < ZOOM_THRESHOLD;

        Matrix gpsMarkTransform = new Matrix();
        Matrix spotIconTransform = new Matrix();
//        Matrix nodeIconTransform = new Matrix();
        Matrix checkinIconTransform = new Matrix();
        Matrix mergedCheckinIconTransform = new Matrix();
        gpsMarkTransform.postTranslate(-gpsMarkerPivotX, -gpsMarkerPivotY);
        spotIconTransform.postTranslate(-spotIconPivotX, -spotIconPivotY);
//        nodeIconTransform.postTranslate(-edgeNodeIconPivotX, -edgeNodeIconPivotY);
        checkinIconTransform.postTranslate(-checkinIconPivotX, -checkinIconPivotY);
        mergedCheckinIconTransform.postTranslate(-mergedCheckinIconPivotX, -mergedCheckinIconPivotY);
        float[] point = new float[]{0, 0};

        // transform tourist map (ImageView)
        transformMat.mapPoints(point);
        touristMap.setScaleX(scale);
        touristMap.setScaleY(scale);
        touristMap.setRotation(rotation);
        touristMap.setTranslationX(point[0]);
        touristMap.setTranslationY(point[1]);

//        // transform fog map
//        if (fogSwitch) {
//            fogMap.setScaleX(scale);
//            fogMap.setScaleY(scale);
//            fogMap.setRotation(rotation);
//            fogMap.setTranslationX(point[0]);
//            fogMap.setTranslationY(point[1]);
//        }

        // transform gpsMarker
        point[0] = gpsDistortedX;
        point[1] = gpsDistortedY;
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);

        // transform nodeImage
//        if (edgeLengthSwitch) {
//            for (ImageNode imageNode : edgeNodeList) {
//                point[0] = imageNode.x;
//                point[1] = imageNode.y;
//                transformMat.mapPoints(point);
//                nodeIconTransform.mapPoints(point);
//                imageNode.icon.setTranslationX(point[0]);
//                imageNode.icon.setTranslationY(point[1]);
//            }
//            for (ImageNode imageNode : pathEdgeNodeList) {
//                point[0] = imageNode.x;
//                point[1] = imageNode.y;
//                transformMat.mapPoints(point);
//                nodeIconTransform.mapPoints(point);
//                imageNode.icon.setTranslationX(point[0]);
//                imageNode.icon.setTranslationY(point[1]);
//            }
//        }

        // transform spot
        if (spotSwitch) {
            if (isMerged) {
                for (SpotNode spotNode : spotNodeList) {
                    if (spotNode.order == 2) {
                        spotNode.icon.setVisibility(View.GONE);
                    }
                }
            }
//            } else {
//                for (SpotNode spotNode : spotNodeList) {
//                    if (spotNode.order == 2) {
//                        spotNode.icon.setVisibility(View.VISIBLE);
//                    }
//                }
//            }

            for (SpotNode spotNode : spotNodeList) {
                point[0] = spotNode.x;
                point[1] = spotNode.y;
                transformMat.mapPoints(point);
                spotIconTransform.mapPoints(point);
                spotNode.icon.setTranslationX(point[0]);
                spotNode.icon.setTranslationY(point[1]);
            }
        }

        // transform checkinClusterNode
        if (checkinSwitch) {
            if (isMerged) {
                for (CheckinNode checkinNode : checkinNodeList) {
                    checkinNode.icon.setVisibility(View.GONE);
                }

                for (CheckinNode checkinClusterNode : checkinClusterNodeList) {
                    checkinClusterNode.icon.setVisibility(View.VISIBLE);
                }
            }
//            } else {
//                for (CheckinNode checkinNode : checkinNodeList) {
//                    checkinNode.icon.setVisibility(View.VISIBLE);
//                }
//
//                for (CheckinNode checkinClusterNode : checkinClusterNodeList) {
//                    checkinClusterNode.icon.setVisibility(View.GONE);
//                }
//            }

            for (CheckinNode checkinNode : checkinNodeList) {
                point[0] = checkinNode.x;
                point[1] = checkinNode.y;
                transformMat.mapPoints(point);
                checkinIconTransform.mapPoints(point);
                checkinNode.icon.setTranslationX(point[0]);
                checkinNode.icon.setTranslationY(point[1]);
            }

            for (CheckinNode checkinClusterNode : checkinClusterNodeList) {
                point[0] = checkinClusterNode.x;
                point[1] = checkinClusterNode.y;
                transformMat.mapPoints(point);
                mergedCheckinIconTransform.mapPoints(point);
                checkinClusterNode.icon.setTranslationX(point[0]);
                checkinClusterNode.icon.setTranslationY(point[1]);
            }
        }
    }

    private void addSpotNode(final SpotNode spotNode) {
        View icon = inflater.inflate(R.layout.item_spot, null);
        spotNode.icon = icon;
        ((TextView) spotNode.icon.findViewById(R.id.spot_name)).setText(spotNode.name);
        rootLayout.addView(icon, rootLayout.indexOfChild(seperator));
    }

//    private void addEdgeNode(ImageNode imageNode, String iconColor) {
//        imageNode.icon = new ImageView(context);
//        if (iconColor.equals("blue"))
//            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_trans);
//        if (iconColor.equals("black"))
//            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_black_trans);
//        imageNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
//        rootLayout.addView(imageNode.icon, rootLayout.indexOfChild(seperator));
//    }
//
//    public void showPathIdicator(SpotNode spotNode) {
//        for (ImageNode imageNode : pathEdgeNodeList) {
//            rootLayout.removeView(imageNode.icon);
//        }
//
//        EdgeNode.Vertex from = edgeNode.findVertex(gpsDistortedX, gpsDistortedY);
//        EdgeNode.Vertex to = edgeNode.findVertex(spotNode.x, spotNode.y);
//        edgeNode.shortestPath(from, to);
//        pathEdgeNodeList = edgeNode.getPathNodeList();
//        for (ImageNode imageNode : pathEdgeNodeList) {
//            addEdgeNode(imageNode, "blue");
//        }
//    }

    public void showCheckinDialog(String key) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        CheckinDialogFragment checkinDialogFragment = CheckinDialogFragment.newInstance(key);
        checkinDialogFragment.show(fragmentManager, "fragment_checkin_dialog");
    }

    public void showCheckinListDialog(CheckinNode checkinNode) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        CheckinListDialogFragment checkinListDialogFragment = CheckinListDialogFragment.newInstance(checkinNode);
        checkinListDialogFragment.show(fragmentManager, "fragment_checkin_list_dialog");
    }

    public void addCheckin(final Checkin checkin) {
        float[] imgPx = gpsToImgPx(Float.valueOf(checkin.lat), Float.valueOf(checkin.lng));

        addCheckinIcon(checkin, imgPx[0], imgPx[1]);
        addCheckinClusterIcon(checkin, imgPx[0], imgPx[1]);
        changeCheckin(checkin); // set heat checkin icon
//        reRender();
    }

    // change with hot checkin icon
    public void changeCheckin(final Checkin checkin) {
        View checkinNodeView = checkinNodeMap.get(checkin.key).icon;
        View checkinClusterNodeView = checkinClusterNodeMap.get(checkin.key).icon;

        boolean allPopularFlag = checkin.popularTargetUid.containsKey("all") && checkin.popularTargetUid.get("all");
        boolean targetPopularFlag = !uid.equals("") && checkin.popularTargetUid.containsKey(uid) && checkin.popularTargetUid.get(uid);
        int checkinLikeNum = checkin.likeNum;
        if (checkin.like != null) checkinLikeNum += checkin.like.size();
        if (allPopularFlag || targetPopularFlag || checkinLikeNum >= 5) {
            if (uid.equals(checkin.uid)) {
                ((ImageView) checkinNodeView).setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.self_hot_checkin_icon_60px, null));
            } else {
                ((ImageView) checkinNodeView).setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.hot_checkin_icon_60px, null));
            }
            ImageView clusterIcon = checkinClusterNodeView.findViewById(R.id.checkin_icon);
            clusterIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.hot_checkin_icon_96px, null));
        }

        reRender();
    }

    // checkins that their icon overlay
    private void addCheckinIcon(final Checkin checkin, float x, float y) {

        // search for exist node
        for (final CheckinNode checkinNode : checkinNodeList) {
            double distance = Math.pow(x - checkinNode.x, 2) + Math.pow(y - checkinNode.y, 2);
            if (distance < OVERLAP_THRESHOLD) {
                checkinNode.checkinList.add(checkin);
                checkinNodeMap.put(checkin.key, checkinNode);
                if (checkinNode.checkinList.size() > 1) {
                    checkinNode.icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showCheckinListDialog(checkinNode);
                        }
                    });
                }
                if (uid.equals(checkin.uid)) {
                    ((ImageView) checkinNode.icon).setImageDrawable(
                            ResourcesCompat.getDrawable(getResources(), R.drawable.self_checkin_icon_60px, null));
                }
                return;
            }
        }

        // create new node
        CheckinNode checkinNode = new CheckinNode(x, y, Float.valueOf(checkin.lat), Float.valueOf(checkin.lng));
        checkinNode.icon = new ImageView(context);
        checkinNode.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCheckinDialog(checkin.key);
            }
        });
        checkinNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));

        if (uid.equals(checkin.uid)) {
            ((ImageView) checkinNode.icon).setImageDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.self_checkin_icon_60px, null));
        } else {
            ((ImageView) checkinNode.icon).setImageDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.checkin_icon_60px, null));
        }
        rootLayout.addView(checkinNode.icon, rootLayout.indexOfChild(seperator));
        checkinNode.checkinList.add(checkin);
        checkinNodeList.add(checkinNode);
        checkinNodeMap.put(checkin.key, checkinNode);
    }

    // nearby checkins, checkins with same location
    private void addCheckinClusterIcon(final Checkin checkin, final float x, final float y) {

        String location = "";
        if (spotNodeMap.containsKey(checkin.location)) {
            location = checkin.location;
        } else {
            float minDist = 101f;
            for (SpotNode spot : spotNodeList) {
                float dist = Utility.isNearBy(Float.valueOf(checkin.lat), Float.valueOf(checkin.lng),
                        Float.valueOf(spot.lat), Float.valueOf(spot.lng));
                if (dist <= 100f && dist < minDist) {
                    location = spot.name;
                    minDist = dist;
                }
            }
        }

        if (location.equals("")) {
            // search for exist cluster node
            for (final CheckinNode checkinClusterNode : checkinClusterNodeList) {
                if (checkinClusterNode.onSpot) continue;

                double distance = Utility.isNearBy(Float.valueOf(checkin.lat), Float.valueOf(checkin.lng),
                        checkinClusterNode.lat, checkinClusterNode.lng);
                if (distance < CLUSTER_THRESHOLD) {
                    int clusterSize = checkinClusterNode.checkinList.size();
                    checkinClusterNode.lat = (checkinClusterNode.lat * clusterSize + Float.valueOf(checkin.lat)) / (clusterSize + 1);
                    checkinClusterNode.lng = (checkinClusterNode.lng * clusterSize + Float.valueOf(checkin.lng)) / (clusterSize + 1);
                    float[] imgPx = Utility.gpsToImgPx(checkinClusterNode.lat, checkinClusterNode.lng);
                    checkinClusterNode.x = imgPx[0];
                    checkinClusterNode.y = imgPx[1];
                    checkinClusterNode.icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClusterNodeClick(checkinClusterNode);
                        }
                    });
                    checkinClusterNode.checkinList.add(checkin);
                    TextView checkinsNumCircle = checkinClusterNode.icon.findViewById(R.id.checkin_num);
                    int checkinsNum = checkinClusterNode.checkinList.size();
                    checkinsNumCircle.setText(checkinsNum < 10 ?
                            " " + String.valueOf(checkinsNum) : String.valueOf(checkinsNum));
                    checkinClusterNodeMap.put(checkin.key, checkinClusterNode);
                    return;
                }
            }
            // cluster not found, create a new one
            CheckinNode checkinNode = new CheckinNode(
                    x, y, Float.valueOf(checkin.lat), Float.valueOf(checkin.lng),
                    inflater.inflate(R.layout.item_merged_checkin, null));
            checkinNode.icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCheckinDialog(checkin.key);
                }
            });
            checkinNode.checkinList.add(checkin);
            TextView checkinsNumCircle = checkinNode.icon.findViewById(R.id.checkin_num);
            checkinsNumCircle.setText(" 1");
            rootLayout.addView(checkinNode.icon, rootLayout.indexOfChild(seperator));
            checkinClusterNodeList.add(checkinNode);
            checkinClusterNodeMap.put(checkin.key, checkinNode);

        } else { // add into spot
            final SpotNode spotNode = spotNodeMap.get(location);
            if (spotNode.checkinNode == null) { // no checkin on spot yet
                spotNode.checkinNode = new CheckinNode(spotNode.x, spotNode.y,
                        Float.valueOf(spotNode.lat), Float.valueOf(spotNode.lng),
                        inflater.inflate(R.layout.item_merged_checkin, null));
                spotNode.checkinNode.icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCheckinDialog(checkin.key);
                    }
                });
                spotNode.checkinNode.onSpot = true;
                spotNode.checkinNode.checkinList.add(checkin);
                TextView checkinsNumCircle = spotNode.checkinNode.icon.findViewById(R.id.checkin_num);
                checkinsNumCircle.setText(" 1");
                rootLayout.addView(spotNode.checkinNode.icon, rootLayout.indexOfChild(seperator));
                checkinClusterNodeList.add(spotNode.checkinNode);
                checkinClusterNodeMap.put(checkin.key, spotNode.checkinNode);
            } else {
                spotNode.checkinNode.icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClusterNodeClick(spotNode.checkinNode);
                    }
                });
                spotNode.checkinNode.checkinList.add(checkin);
                TextView checkinsNumCircle = spotNode.checkinNode.icon.findViewById(R.id.checkin_num);
                int checkinsNum = spotNode.checkinNode.checkinList.size();
                checkinsNumCircle.setText(checkinsNum < 10 ?
                        " " + String.valueOf(checkinsNum) : String.valueOf(checkinsNum));
                checkinClusterNodeMap.put(checkin.key, spotNode.checkinNode);
            }
        }
    }

    public void onLocateClick(String lat, String lng) {
        float[] imgPx = Utility.gpsToImgPx(Float.valueOf(lat), Float.valueOf(lng));
        translateToImgPx(imgPx[0], imgPx[1], false);
    }

    public void onLocateClick(String location) {
        CheckinNode checkinClusterNode = spotNodeMap.get(location).checkinNode;
        onClusterNodeClick(checkinClusterNode);
    }

    public void onLocateCheckinClick(String postId) {
        CheckinNode checkinClusterNode = checkinClusterNodeMap.get(postId);
        onClusterNodeClick(checkinClusterNode);
    }

    public void onClusterNodeClick(CheckinNode checkinNode) {
        translateToImgPx(checkinNode.x, checkinNode.y, false);
        checkinNode.icon.setVisibility(View.GONE);
        for (Checkin checkinInSpot : checkinNode.checkinList) {
            View checkinIcon = checkinNodeMap.get(checkinInSpot.key).icon;
            checkinIcon.setVisibility(View.VISIBLE);
        }
    }

    public void translateToImgPx(final float x, final float y, final boolean toCurrent) {

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
                    reRender(false);
                    translationHandler.postDelayed(this, 5);
                }
            }
        };
        translationHandler.postDelayed(translationInterpolation, 5);
    }

    public void rotateToNorth() {
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

    public void handleGpsUpdate(float lat, float lng) {

        if (getView() == null) {
            return;
        }

        // GPS is within tourist map.
        if (lat >= realMesh.minLat && lat <= realMesh.maxLat && lng >= realMesh.minLon && lng <= realMesh.maxLon) {

            if (gpsMarker.getVisibility() != View.VISIBLE) {
                gpsMarker.setVisibility(View.VISIBLE);
            }

            float[] imgPx = gpsToImgPx(lat, lng);

            if (imgPx[0] != -1 && imgPx[1] != -1) {
                gpsDistortedX = imgPx[0];
                gpsDistortedY = imgPx[1];

                reRender();
            }

        } else { // GPS outside.

            if (gpsMarker.getVisibility() != View.GONE) {
                gpsMarker.setVisibility(View.GONE);
                Toast.makeText(context, getString(R.string.toast_gps_outside), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void handleFogUpdate(double lat, double lng) {
//        if (lat >= realMesh.minLat && lat <= realMesh.maxLat && lng >= realMesh.minLon && lng <= realMesh.maxLon) {
//            float[] imgPx = gpsToImgPx(lat, lng);
//            if (imgPx[0] != -1 && imgPx[1] != -1) {
//                // update fog map
//                Paint paint = new Paint();
//                paint.setColor(Color.TRANSPARENT);
//                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//                paint.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));
//                Canvas canvas = new Canvas(fogBitmap);
//                canvas.drawCircle(imgPx[0], imgPx[1], 25, paint);
//                fogMap.postInvalidate();
//            }
//        }
    }

    public void handleSensorChange(float r) {
        final float RADIAN = 57.296f;
        if (gpsMarker != null) {
            gpsMarker.setRotation(r * RADIAN + rotation);
        }
    }

//    public void switchFog(boolean flag) {
//        fogSwitch = flag;
//        if (flag) {
//            fogMap.setVisibility(View.VISIBLE);
//        } else {
//            fogMap.setVisibility(View.GONE);
//        }
//        reRender();
//    }

//    public void switchDistanceIndicator(boolean flag) {
//        edgeLengthSwitch = flag;
//        if (flag) {
//            for (ImageNode imageNode : edgeNodeList) {
//                imageNode.icon.setVisibility(View.VISIBLE);
//            }
//        } else {
//            for (ImageNode imageNode : edgeNodeList) {
//                imageNode.icon.setVisibility(View.GONE);
//            }
//        }
//        reRender();
//    }

    public void switchCheckinIcon(boolean flag) {
        checkinSwitch = flag;
        if (flag) {
            for (CheckinNode checkinNode : checkinNodeList) {
                checkinNode.icon.setVisibility(View.VISIBLE);
            }
            for (CheckinNode checkinClusterNode : checkinClusterNodeList) {
                checkinClusterNode.icon.setVisibility(View.VISIBLE);
            }
        } else {
            for (CheckinNode checkinNode : checkinNodeList) {
                checkinNode.icon.setVisibility(View.GONE);
            }
            for (CheckinNode checkinClusterNode : checkinClusterNodeList) {
                checkinClusterNode.icon.setVisibility(View.GONE);
            }
        }
        reRender();
    }

    public void switchSpotIcon(boolean flag) {
        spotSwitch = flag;
        if (flag) {
            for (SpotNode spotNode : spotNodeList) {
                spotNode.icon.setVisibility(View.VISIBLE);
            }
        } else {
            for (SpotNode spotNode : spotNodeList) {
                spotNode.icon.setVisibility(View.GONE);
            }
        }
        reRender();
    }
}
