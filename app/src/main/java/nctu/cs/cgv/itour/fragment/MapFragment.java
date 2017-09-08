package nctu.cs.cgv.itour.fragment;

import android.app.Activity;
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
import android.support.v7.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Map;
import java.util.Objects;

import nctu.cs.cgv.itour.ArrayAdapterSearchView;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.AudioCheckinActivity;
import nctu.cs.cgv.itour.activity.PhotoCheckinActivity;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.ImageNode;
import nctu.cs.cgv.itour.object.MergedCheckinNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotList;
import nctu.cs.cgv.itour.object.SpotNode;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.dpToPx;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
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
    private int screenWidth = 0;
    private int screenHeight = 0;
    private int rootLayoutWidth = 0;
    private int rootLayoutHeight = 0;
    // UI references
//    private RelativeLayout rootLayout;
    private RelativeLayout rootLayout;
    private ImageView touristMap;
    private ImageView fogMap;
    private LinearLayout gpsMarker;
    private FloatingActionButton gpsBtn;
    private FloatingActionButton audioBtn;
    private FloatingActionButton photoBtn;
    private FloatingActionsMenu floatingActionsMenu;
    private RelativeLayout.LayoutParams layoutParams;
    private Bitmap fogBitmap;
    private ProgressDialog progressDialog;
    // objects
    private List<ImageNode> edgeNodeList;
    private List<ImageNode> pathEdgeNodeList;
    private List<MergedCheckinNode> mergedCheckinList;
    private List<MergedCheckinNode> spotCheckinList;
    private List<ImageNode> checkinList;
    private List<ImageView> primarySpotList;
    private SpotList spotList;
    private Mesh realMesh;
    private Mesh warpMesh;
    private EdgeNode edgeNode;
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

        // get screen size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // init objects
        realMesh = new Mesh(new File(dirPath + mapTag + "_mesh.txt"));
        realMesh.readBoundingBox(new File(dirPath + mapTag + "_bound_box.txt"));
        warpMesh = new Mesh(new File(dirPath + mapTag + "_warpMesh.txt"));
        spotList = new SpotList(new File(dirPath + mapTag + "_spot_list.txt"), realMesh, warpMesh);
        edgeNode = new EdgeNode(new File(dirPath + mapTag + "_edge_length.txt"));
        edgeNodeList = new ArrayList<>();
        pathEdgeNodeList = new ArrayList<>();
        checkinList = new ArrayList<>();
        mergedCheckinList = new ArrayList<>();
        spotCheckinList = new ArrayList<>();
        primarySpotList = new ArrayList<>();
        transformMat = new Matrix();
        progressDialog = new ProgressDialog(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        rootLayout = (RelativeLayout) view.findViewById(R.id.parent_layout);

        // load image from disk and set tourist map
        Bitmap touristMapBitmap = BitmapFactory.decodeFile(dirPath + mapTag + "_distorted_map.png");
        touristMapWidth = touristMapBitmap.getWidth();
        touristMapHeight = touristMapBitmap.getHeight();
        layoutParams = new RelativeLayout.LayoutParams(touristMapWidth, touristMapHeight);
        touristMap = new ImageView(context);
        touristMap.setLayoutParams(layoutParams);
        touristMap.setScaleType(ImageView.ScaleType.MATRIX);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        ((FrameLayout) view.findViewById(R.id.touristmap)).addView(touristMap);

        // draw fog
        fogBitmap = Bitmap.createBitmap(touristMapWidth, touristMapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fogBitmap);
        canvas.drawARGB(120, 0, 0, 0);
        fogMap = new ImageView(context);
        fogMap.setLayoutParams(layoutParams);
        fogMap.setScaleType(ImageView.ScaleType.MATRIX);
        fogMap.setImageBitmap(fogBitmap);
        fogMap.setPivotX(0);
        fogMap.setPivotY(0);
        switchFog();
        ((FrameLayout) view.findViewById(R.id.touristmap)).addView(fogMap);

        // draw edge primarySpot
        if (preferences.getBoolean("distance_indicator", false)) {
            edgeNodeList = edgeNode.getNodeList();
            for (ImageNode imageNode : edgeNodeList) {
                addEdgeNode(imageNode, "black");
            }
        }

        // set gpsMarker
        gpsMarker = (LinearLayout) view.findViewById(R.id.gps_marker);
        gpsMarker.setPivotX(gpsMarkerWidth / 2);
        gpsMarker.setPivotY(gpsMarkerHeight / 2 + gpsDirectionHeight);

        // set buttons
        gpsBtn = (FloatingActionButton) view.findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGpsCurrent && !isOrientationCurrent) rotateToNorth();
                else translateToCenter();
            }
        });
        audioBtn = (FloatingActionButton) view.findViewById(R.id.btn_audio);
        audioBtn.setVisibility(View.GONE); // prevent intercepting touch event for float action menu_search
        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, AudioCheckinActivity.class));
                floatingActionsMenu.collapseImmediately();
            }
        });
        photoBtn = (FloatingActionButton) view.findViewById(R.id.btn_photo);
        photoBtn.setVisibility(View.GONE); // prevent intercepting touch event for float action menu_search
        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, PhotoCheckinActivity.class));
                floatingActionsMenu.collapseImmediately();
            }
        });
        floatingActionsMenu = (FloatingActionsMenu) view.findViewById(R.id.menu_add);
        floatingActionsMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                // make it clickable
                audioBtn.setVisibility(View.VISIBLE);
                photoBtn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMenuCollapsed() {
                // prevent intercepting touch event for float action menu_search
                audioBtn.setVisibility(View.GONE);
                photoBtn.setVisibility(View.GONE);
            }
        });

        // draw spots
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.primarySpot.entrySet()) {
            addSpot(spotNodeEntry.getValue());
        }
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.secondarySpot.entrySet()) {
            addSpot(spotNodeEntry.getValue());
        }

//        addPrimarySpot();

        updateCheckin();

        setTouchListener();

        setHasOptionsMenu(true);

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                rootLayoutWidth = rootLayout.getWidth();
                rootLayoutHeight = rootLayout.getHeight();
                touristMap.setScaleType(ImageView.ScaleType.MATRIX);
                fogMap.setScaleType(ImageView.ScaleType.MATRIX);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu, inflater);

        ArrayList<String> array = new ArrayList<>();
        array.addAll(spotList.getSpots());
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_search, array);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final ArrayAdapterSearchView searchView = (ArrayAdapterSearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String autocompleteStr = adapter.getItem(position);
                searchView.clearFocus();
                searchView.setText(autocompleteStr);
                translateToSpot(spotList.primarySpot.get(autocompleteStr));
            }
        });
        searchView.setAdapter(adapter);
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
        gpsMarkTransform.postTranslate(-(gpsMarkerWidth / 2), -(gpsMarkerHeight / 2 + gpsDirectionHeight));
        nodeIconTransform.postTranslate(-nodeIconWidth / 2, -nodeIconHeight / 2);
        checkinIconTransform.postTranslate(-checkinIconWidth / 2, -checkinIconHeight);
        float[] point = new float[]{0, 0};

        // transform tourist map (ImageView)
        transformMat.mapPoints(point);
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
        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.primarySpot.entrySet()) {
            SpotNode spotNode = spotNodeEntry.getValue();
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            Matrix spotIconTransform = new Matrix();
            spotIconTransform.postTranslate(-dpToPx(context, 12 / 2), -dpToPx(context, 12 / 2));
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }

        for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.secondarySpot.entrySet()) {
            SpotNode spotNode = spotNodeEntry.getValue();
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            Matrix spotIconTransform = new Matrix();
            spotIconTransform.postTranslate(-dpToPx(context, 12 / 2), -dpToPx(context, 12 / 2));
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
        }

        isMerged = scale < 2.2;

        if (isMerged) {
            for (ImageNode imageNode : checkinList) {
                imageNode.icon.setVisibility(View.GONE);
            }

            for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
                mergedCheckinNode.icon.setVisibility(View.VISIBLE);
            }

            for (MergedCheckinNode spotCheckinNode : spotCheckinList) {
                spotCheckinNode.icon.setVisibility(View.VISIBLE);
            }

            for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.secondarySpot.entrySet()) {
                SpotNode spotNode = spotNodeEntry.getValue();
                spotNode.icon.setVisibility(View.GONE);
            }

        } else {
            for (ImageNode imageNode : checkinList) {
                imageNode.icon.setVisibility(View.VISIBLE);
            }

            for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
                mergedCheckinNode.icon.setVisibility(View.GONE);
            }

            for (MergedCheckinNode spotCheckinNode : spotCheckinList) {
                spotCheckinNode.icon.setVisibility(View.GONE);
            }

            for (Map.Entry<String, SpotNode> spotNodeEntry : spotList.secondarySpot.entrySet()) {
                SpotNode spotNode = spotNodeEntry.getValue();
                spotNode.icon.setVisibility(View.VISIBLE);
            }
        }

        // transform checkins
        for (ImageNode imageNode : checkinList) {
            point[0] = imageNode.x;
            point[1] = imageNode.y;
            transformMat.mapPoints(point);
            checkinIconTransform.mapPoints(point);
            imageNode.icon.setTranslationX(point[0]);
            imageNode.icon.setTranslationY(point[1]);
        }

        for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
            point[0] = mergedCheckinNode.x;
            point[1] = mergedCheckinNode.y;
            transformMat.mapPoints(point);
            Matrix mergedCheckinIconTransform = new Matrix();
            mergedCheckinIconTransform.postTranslate(-dpToPx(context, 32 / 2), -dpToPx(context, 32));
            mergedCheckinIconTransform.mapPoints(point);
            mergedCheckinNode.icon.setTranslationX(point[0]);
            mergedCheckinNode.icon.setTranslationY(point[1]);
        }

        for (MergedCheckinNode spotCheckinNode : spotCheckinList) {
            point[0] = spotCheckinNode.x;
            point[1] = spotCheckinNode.y;
            transformMat.mapPoints(point);
            Matrix mergedCheckinIconTransform = new Matrix();
            mergedCheckinIconTransform.postTranslate(-dpToPx(context, 32 / 2), -dpToPx(context, 32));
            mergedCheckinIconTransform.mapPoints(point);
            spotCheckinNode.icon.setTranslationX(point[0]);
            spotCheckinNode.icon.setTranslationY(point[1]);
        }


//        point = gpsToImgPx(realMesh, warpMesh, 25.17018f, 121.43994f);
//        transformMat.mapPoints(point);
//        Matrix transformMatrix = new Matrix();
//        transformMatrix.postTranslate(-dpToPx(48 / 2), -dpToPx(48));
//        transformMatrix.mapPoints(point);
//        primarySpotList.get(0).setTranslationX(point[0]);
//        primarySpotList.get(0).setTranslationY(point[1]);
    }

    private void addEdgeNode(ImageNode imageNode, String iconColor) {
        imageNode.icon = new ImageView(context);
        if (iconColor.equals("blue"))
            imageNode.icon.setImageResource(R.drawable.ftprint_trans);
        if (iconColor.equals("black"))
            imageNode.icon.setImageResource(R.drawable.ftprint_black_trans);
        imageNode.icon.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
        imageNode.icon.setTranslationX(imageNode.x - nodeIconWidth / 2);
        imageNode.icon.setTranslationY(imageNode.y - nodeIconHeight / 2);
        rootLayout.addView(imageNode.icon);
    }

    private void removeEdgeNode(ImageNode imageNode) {
        rootLayout.removeView(imageNode.icon);
    }

    private void addSpot(SpotNode spotNode) {
        // create icon
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View icon = inflater.inflate(R.layout.item_spot, null);
        TextView spotNameView = (TextView) icon.findViewById(R.id.spot_name);
        spotNameView.setText(spotNode.name);
        spotNode.icon = icon;
        // transform icon
        Matrix iconTransform = new Matrix();
        final float[] gpsDistorted = {spotNode.x, spotNode.y};
        iconTransform.postTranslate(-dpToPx(context, 12 / 2), -dpToPx(context, 12 / 2));
        transformMat.mapPoints(gpsDistorted);
        iconTransform.mapPoints(gpsDistorted);
        icon.setTranslationX(gpsDistorted[0]);
        icon.setTranslationY(gpsDistorted[1]);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (ImageNode imageNode : pathEdgeNodeList) {
                    removeEdgeNode(imageNode);
                }

                EdgeNode.Vertex from = edgeNode.findVertex(gpsDistortedX, gpsDistortedY);
                EdgeNode.Vertex to = edgeNode.findVertex(gpsDistorted[0], gpsDistorted[1]);
                edgeNode.shortestPath(from, to);
                pathEdgeNodeList = edgeNode.getPathNodeList();
                for (ImageNode imageNode : pathEdgeNodeList) {
                    addEdgeNode(imageNode, "blue");
                }
            }
        });
        // add to rootlayout
        rootLayout.addView(icon);
    }

    private void addPrimarySpot() {
        // create icon
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(context.getDrawable(R.drawable.fuyou_temple));
//        layoutParams = new RelativeLayout.LayoutParams(dpToPx(48), dpToPx(48));

        // transform icon
        Matrix iconTransform = new Matrix();
        float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, 25.17018f, 121.43994f);
//        iconTransform.postTranslate(-dpToPx(48 / 2), -dpToPx(48 / 2));
        transformMat.mapPoints(gpsDistorted);
        iconTransform.mapPoints(gpsDistorted);
        icon.setTranslationX(gpsDistorted[0]);
        icon.setTranslationY(gpsDistorted[1]);
        // add to rootlayout
        primarySpotList.add(icon);
        rootLayout.addView(icon);
    }

    private void updateCheckin() {
        progressDialog.setMessage("Load check-in...");
        progressDialog.show();

        databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("checkin").child(mapTag);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Checkin checkin = issue.getValue(Checkin.class);
                        checkin.key = issue.getKey();
                        handleCheckinMsg(issue.getKey(), checkin);
                    }
                }

                if (scale < 2.2) {
                    for (ImageNode imageNode : checkinList) {
                        imageNode.icon.setVisibility(View.GONE);
                    }

                    for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
                        mergedCheckinNode.icon.setVisibility(View.VISIBLE);
                    }

                    isMerged = true;
                } else {
                    for (ImageNode imageNode : checkinList) {
                        imageNode.icon.setVisibility(View.VISIBLE);
                    }

                    for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
                        mergedCheckinNode.icon.setVisibility(View.GONE);
                    }

                    isMerged = false;
                }
                // transform
                reRender();

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });
    }

    private void showDialog(final String postId) { // postId: unique key for data query

        progressDialog.show();

        Query query = databaseReference.child("checkinIcon").child(mapTag).child(postId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                if (dataSnapshot.exists()) {
                    Checkin checkin = dataSnapshot.getValue(Checkin.class);
                    checkin.key = postId;
                    showDialog(checkin);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });
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

    private void translateToCenter() {
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

    private void translateToSpot(final SpotNode spotNode) {
        final float transX = rootLayoutWidth / 2 - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2));
        final float transY = rootLayoutHeight / 3 - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2));
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;
        final float deltaScale = (2.2f - scale) / 10f;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rootLayoutWidth / 2 - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2))) <= Math.abs(deltaTransX) ||
                        Math.abs(rootLayoutHeight / 3 - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2))) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(
                            rootLayoutWidth / 2 - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2)),
                            rootLayoutHeight / 3 - (spotNode.icon.getTranslationY() - dpToPx(context, 12 / 2)));

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
                    if (Math.abs(rootLayoutWidth / 2 - (spotNode.icon.getTranslationX() - dpToPx(context, 12 / 2))) < rootLayoutHeight / 4) {
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

    private void translateToPos(final View view) {
        final float transX = rootLayoutWidth / 2 - view.getTranslationX();
        final float transY = rootLayoutHeight / 3 - view.getTranslationY();
        final float deltaTransX = transX / 10f;
        final float deltaTransY = transY / 10f;
        final float deltaScale = (2.2f - scale) / 10f;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rootLayoutWidth / 2 - view.getTranslationX()) <= Math.abs(deltaTransX) ||
                        Math.abs(rootLayoutHeight / 3 - view.getTranslationY()) <= Math.abs(deltaTransY)) {
                    transformMat.postTranslate(
                            rootLayoutWidth / 2 - view.getTranslationX(),
                            rootLayoutHeight / 3 - view.getTranslationY());

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
                    if (Math.abs(rootLayoutWidth / 2 - view.getTranslationX()) < rootLayoutHeight / 4) {
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

    public void handleLocationChange(double lat, double lng) {

        float[] point = gpsToImgPx(realMesh, warpMesh, (float) lat, (float) lng);

        gpsDistortedX = point[0];
        gpsDistortedY = point[1];

        reRender();

        // check whether it should update fog or not
        double distance = Math.sqrt(Math.pow(lastFogClearPosX - gpsDistortedX, 2.0) + Math.pow(lastFogClearPosY - gpsDistortedY, 2.0));
        if (distance > 5.0) {
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

    public void handleCheckinMsg(final String postId, double lat, double lng) {

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

        // add new icon ImageView
        ImageView checkinIcon = new ImageView(context);
        checkinIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location_on_red_600_24dp));
        checkinIcon.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));
        checkinIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(postId);
            }
        });
        rootLayout.addView(checkinIcon);

        // transform to distorted gps value
        Matrix checkInIconTransform = new Matrix();
        checkInIconTransform.postTranslate(-checkinIconWidth / 2, -checkinIconHeight);
        float[] point = new float[]{lngDistorted, latDistorted};
        transformMat.mapPoints(point);
        checkInIconTransform.mapPoints(point);
        checkinIcon.setTranslationX(point[0]);
        checkinIcon.setTranslationY(point[1]);
//        checkinIconList.add(checkinIcon);
    }

    public void handleCheckinMsg(final String postId, final Checkin checkin) {

        float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, Float.valueOf(checkin.lat), Float.valueOf(checkin.lng));
        ImageNode checkinNode = new ImageNode(gpsDistorted[0], gpsDistorted[1]);

        // create icon ImageView
        ImageView checkinIcon = new ImageView(context);
        checkinIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location_on_red_600_24dp));
        checkinIcon.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));
        checkinIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(checkin);
            }
        });
        // add to rootlayout
        rootLayout.addView(checkinIcon);
        // add into list
        checkinNode.icon = checkinIcon;
        checkinList.add(checkinNode);

        // add into spot
        // find out whether the location is in spotlist or not.
        SpotNode spotNode = spotList.primarySpot.get(checkin.location);
        if (spotNode != null) {
            // this is the first checkinIcon of this spot
            if (spotNode.checkins == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View icon = inflater.inflate(R.layout.item_bigcheckin, null);
                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        translateToPos(v);
                    }
                });
                rootLayout.addView(icon);

                MergedCheckinNode mergedCheckinNode = new MergedCheckinNode(spotNode.x, spotNode.y);
                mergedCheckinNode.icon = icon;
                spotNode.checkins = mergedCheckinNode;
                spotCheckinList.add(mergedCheckinNode);
            }
            spotNode.checkins.checkinList.add(checkinNode);
            TextView checkinNumCircle = (TextView) spotNode.checkins.icon.findViewById(R.id.checkin_num);
            checkinNumCircle.setText(String.valueOf(spotNode.checkins.checkinList.size()));
        } else {
            // find cluster to join or create a new one
            boolean newCluster = true;
            for (MergedCheckinNode mergedCheckinNode : mergedCheckinList) {
                double distance = Math.pow(checkinNode.x - mergedCheckinNode.x, 2) + Math.pow(checkinNode.y - mergedCheckinNode.y, 2);
                if (distance < 20500) {
                    int clusterSize = mergedCheckinNode.checkinList.size();
                    mergedCheckinNode.checkinList.add(checkinNode);
                    TextView checkinNumCircle = (TextView) mergedCheckinNode.icon.findViewById(R.id.checkin_num);
                    checkinNumCircle.setText(String.valueOf(clusterSize + 1));

                    mergedCheckinNode.x = (mergedCheckinNode.x * clusterSize + checkinNode.x) / (clusterSize + 1);
                    mergedCheckinNode.y = (mergedCheckinNode.y * clusterSize + checkinNode.y) / (clusterSize + 1);

                    newCluster = false;
                    break;
                }
            }

            // cluster not found, create a new cluster
            if (newCluster) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View icon = inflater.inflate(R.layout.item_bigcheckin, null);
                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        translateToPos(v);
                    }
                });
                rootLayout.addView(icon);

                MergedCheckinNode mergedCheckinNode = new MergedCheckinNode(checkinNode.x, checkinNode.y);
                mergedCheckinNode.checkinList.add(checkinNode);
                mergedCheckinNode.icon = icon;

                mergedCheckinList.add(mergedCheckinNode);
                TextView checkinNumCircle = (TextView) mergedCheckinNode.icon.findViewById(R.id.checkin_num);
                checkinNumCircle.setText(String.valueOf(mergedCheckinNode.checkinList.size()));
            }
        }
        reRender();
    }

    public void switchFog() {
        if (preferences.getBoolean("fog", false)) {
            fogMap.setVisibility(View.VISIBLE);
        } else {
            fogMap.setVisibility(View.GONE);
        }
    }
}
