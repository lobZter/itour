package nctu.cs.cgv.itour.fragment;

import android.app.ProgressDialog;
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
import android.util.Log;
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

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nctu.cs.cgv.itour.ArrayAdapterSearchView;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.AudioCheckinActivity;
import nctu.cs.cgv.itour.activity.PhotoCheckinActivity;
import nctu.cs.cgv.itour.activity.SpotInfoActivity;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.ImageNode;
import nctu.cs.cgv.itour.object.MergedCheckinNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotList;
import nctu.cs.cgv.itour.object.SpotNode;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.spotList;
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
    private final int gpsMarkerWidth = 48;
    private final int gpsMarkerHeight = 48;
    private final int gpsDirectionWidth = 32;
    private final int gpsDirectionHeight = 32;
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
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private ImageView fogMap;
    private LinearLayout gpsMarker;
    private FloatingActionButton gpsBtn;
    private Bitmap fogBitmap;
    private ActionBar actionBar;
    // objects
    private List<ImageNode> edgeNodeList;
    private List<ImageNode> pathEdgeNodeList;
    private List<MergedCheckinNode> mergedCheckinNodeList;
    private List<ImageNode> checkinNodeList;
    private Mesh realMesh;
    private Mesh warpMesh;
    private EdgeNode edgeNode;
    private LayoutInflater inflater;
    // Firebase real-time database
    private DatabaseReference databaseReference;
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
        MapFragment mapFragment = new MapFragment();
        return mapFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);

        // init objects
        realMesh = new Mesh(new File(dirPath + mapTag + "_mesh.txt"));
        realMesh.readBoundingBox(new File(dirPath + mapTag + "_bound_box.txt"));
        warpMesh = new Mesh(new File(dirPath + mapTag + "_warpMesh.txt"));
        edgeNode = new EdgeNode(new File(dirPath + mapTag + "_edge_length.txt"));
        edgeNodeList = new ArrayList<>();
        pathEdgeNodeList = new ArrayList<>();
        checkinNodeList = new ArrayList<>();
        mergedCheckinNodeList = new ArrayList<>();
        transformMat = new Matrix();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        rootLayout = (RelativeLayout) view.findViewById(R.id.parent_layout);
        gpsMarker = (LinearLayout) view.findViewById(R.id.gps_marker);
        gpsBtn = (FloatingActionButton) view.findViewById(R.id.btn_gps);
        FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.touristmap);
        FloatingActionButton audioBtn = (FloatingActionButton) view.findViewById(R.id.btn_audio);
        FloatingActionButton photoBtn = (FloatingActionButton) view.findViewById(R.id.btn_photo);
        final FloatingActionsMenu floatingActionsMenu = (FloatingActionsMenu) view.findViewById(R.id.menu_add);

        // set subtitle
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setSubtitle("Map");

        // set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
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
        for (final SpotNode spotNode : spotList.getSpotsList()) {
            spotNode.icon = inflater.inflate(R.layout.item_spot, null);
            spotNode.icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, SpotInfoActivity.class);
                    intent.putExtra("spotName", spotNode.name);
                    startActivity(intent);
                }
            });
            ((TextView) spotNode.icon.findViewById(R.id.spot_name)).setText(spotNode.name);
            rootLayout.addView(spotNode.icon);
        }

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
        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, AudioCheckinActivity.class));
                floatingActionsMenu.collapseImmediately();
            }
        });
        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, PhotoCheckinActivity.class));
                floatingActionsMenu.collapseImmediately();
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
                reRender();
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
                translateToSpot(spotList.spotNodeMap.get(autocompleteStr));
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
        Matrix checkinIconTransform = new Matrix();
        gpsMarkTransform.postTranslate(-gpsMarkerPivotX, -gpsMarkerPivotY);
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
        for (SpotNode spotNode : spotList.getSpotsList()) {
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            Matrix spotIconTransform = new Matrix();
            spotIconTransform.postTranslate(-dpToPx(context, 12 / 2), -dpToPx(context, 12 / 2));
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

            for (int i = spotList.primarySpotMaxIdx + 1; i < spotList.length; i++) {
                SpotNode spotNode = spotList.getSpotsList().get(i);
                spotNode.icon.setVisibility(View.GONE);
            }

        } else {
            for (ImageNode imageNode : checkinNodeList) {
                imageNode.icon.setVisibility(View.VISIBLE);
            }

            for (MergedCheckinNode mergedCheckinNode : mergedCheckinNodeList) {
                mergedCheckinNode.icon.setVisibility(View.GONE);
            }

            for (int i = spotList.primarySpotMaxIdx + 1; i < spotList.length; i++) {
                SpotNode spotNode = spotList.getSpotsList().get(i);
                spotNode.icon.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addEdgeNode(ImageNode imageNode, String iconColor) {
        imageNode.icon = new ImageView(context);
        if (iconColor.equals("blue"))
            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_trans);
        if (iconColor.equals("black"))
            ((ImageView) imageNode.icon).setImageResource(R.drawable.ftprint_black_trans);
        imageNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
        imageNode.icon.setTranslationX(imageNode.x - nodeIconWidth / 2);
        imageNode.icon.setTranslationY(imageNode.y - nodeIconHeight / 2);
        rootLayout.addView(imageNode.icon);
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

    public void addCheckin(final Checkin checkin) {
        float[] imgPx = gpsToImgPx(realMesh, warpMesh, Float.valueOf(checkin.lat), Float.valueOf(checkin.lng));
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
        rootLayout.addView(checkinNode.icon);

        addMergedCheckin(checkin.location, imgPx[0], imgPx[1]);
    }

    public void addCheckins(ArrayList<Checkin> checkins) {
        for(Checkin checkin : checkins) {
            addCheckin(checkin);
        }
        reRender();
    }

    private void addMergedCheckin(String spotName, float x, float y) {

        SpotNode spotNode = spotList.spotNodeMap.get(spotName);
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
                if(mergedCheckinNode.onSpot) continue;

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
                translateToPos(v);
            }
        });
        mergedCheckinNode.onSpot = onSpot;
        rootLayout.addView(mergedCheckinNode.icon);
        mergedCheckinNodeList.add(mergedCheckinNode);
        return mergedCheckinNode;
    }

    private void showDialog(Checkin checkin) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (Objects.equals(checkin.type, "audio")) {
            AudioCheckinDialogFragment audioCheckinDialogFragment = AudioCheckinDialogFragment.newInstance(checkin);
            audioCheckinDialogFragment.show(fragmentManager, "fragment_audio_checkin_dialog");
        } else if (Objects.equals(checkin.type, "photo")) {
            PhotoCheckinDialogFragment photoCheckinDialogFragment = PhotoCheckinDialogFragment.newInstance(checkin);
            photoCheckinDialogFragment.show(fragmentManager, "fragment_photo_checkin_dialog");
        }
    }

    public void translateToCurrent() {
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
//        final float alpha = 0.5f;
//        final float threshold = 0.5f;
//
//        final Handler translationHandler = new Handler();
//        Runnable translationInterpolation = new Runnable() {
//            @Override
//            public void run() {
//                float fromX = gpsMarker.getTranslationX();
//                float fromY = gpsMarker.getTranslationY();
//                float toX = mapCenterX - gpsMarkerPivotX;
//                float toY = mapCenterY - gpsMarkerPivotY;
//
//                if (Math.abs(fromX - toX) <= threshold || Math.abs(fromY - toY) <= threshold) {
//                    transformMat.setTranslate(toX, toY);
//                    reRender();
//                    isGpsCurrent = true;
//                    gpsBtn.setImageResource(R.drawable.ic_gps_fixed_blue_24dp);
//                    translationHandler.removeCallbacks(this);
//                } else {
//                    transformMat.setTranslate(
//                            lerp(fromX, toX, alpha),
//                            lerp(fromY, toY, alpha)
//                    );
//                    reRender();
//                    translationHandler.postDelayed(this, 2);
//                }
//            }
//        };
//        translationHandler.postDelayed(translationInterpolation, 2);
    }

    public void translateToSpot(final SpotNode spotNode) {
        final float transX = mapCenterX - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2));
        final float transY = mapCenterY - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;
        final float deltaScale = (2.2f - scale) / 10f;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(mapCenterX - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2))) <= Math.abs(deltaTransX) ||
                        Math.abs(mapCenterY - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2))) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(
                            mapCenterX - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2)),
                            mapCenterY - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2)));

                    if (scale < 2.2) {
                        transformMat.postTranslate(-spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2), -spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
                        transformMat.postScale(2.2f / scale, 2.2f / scale);
                        transformMat.postTranslate(spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2), spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
                        scale = 2.2f;
                    }

                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {

                    transformMat.postTranslate(deltaTransX, deltaTransY);

                    if (scale < 2.2) {
                        transformMat.postTranslate(-spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2), -spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
                        transformMat.postScale((scale + deltaScale) / scale, (scale + deltaScale) / scale);
                        transformMat.postTranslate(spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2), spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
                        scale += deltaScale;
                    }

                    reRender();
                    if (Math.abs(mapCenterY - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2))) < 300) {
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

    public void translateToPos(final View view) {
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

    public void translateToGps(float lat, float lng) {

        if (scale < 2.2) {
            transformMat.postTranslate(-mapCenterX, -mapCenterY);
            transformMat.postScale(2.2f / scale, 2.2f / scale);
            transformMat.postTranslate(mapCenterX, mapCenterY);
            scale = 2.2f;
        }

        float[] point = gpsToImgPx(realMesh, warpMesh, lat, lng);
        Matrix checkInIconTransform = new Matrix();
        checkInIconTransform.postTranslate(-checkinIconWidth / 2, -checkinIconHeight);
        transformMat.mapPoints(point);
        checkInIconTransform.mapPoints(point);

        final float transX = mapCenterX - point[0];
        final float transY = mapCenterY - point[1];
        transformMat.postTranslate(transX, transY);

        reRender();


        gpsBtn.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
    }

    public void rotateToNorth() {
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

    public void handleLocationChange(float lat, float lng) {

        float[] imgPx = gpsToImgPx(realMesh, warpMesh, lat, lng);

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
