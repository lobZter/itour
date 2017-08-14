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
import android.location.Location;
import android.media.MediaRecorder;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.ArrayAdapterSearchView;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.AudioCheckinActivity;
import nctu.cs.cgv.itour.activity.PhotoCheckinActivity;
import nctu.cs.cgv.itour.map.RotationGestureDetector;
import nctu.cs.cgv.itour.object.CheckinInfo;
import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.ImageNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotNode;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.arlib.floatingsearchview.util.Util.dpToPx;
import static nctu.cs.cgv.itour.MyApplication.audioPath;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
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
    private String mapTag;
    private Context context;
    // variables
    private Matrix transformMat;
    private float scale = 1;
    private float rotation = 0;
    private float lat = 0, lng = 0;
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
    private LinkedList<Float> nodeList;
    private LinkedList<ImageView> nodeImageList;
//    private LinkedList<Float> checkinList;
//    private LinkedList<ImageView> checkinIconList;
//    private ArrayList<LinearLayout> spotList;
    private Map<String, SpotNode> spotList;
    private ArrayList<SpotNode> bigCheckinList;
    private ArrayList<ImageNode> checkinList;
    private Mesh realMesh;
    private Mesh warpMesh;
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
    private boolean isMerged = true;

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
        mapTag = getArguments().getString("mapTag", "Tamsui");
        mapTag = "nctu";
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
        nodeList = new LinkedList<>();
        nodeImageList = new LinkedList<>();
//        checkinList = new LinkedList<>();
//        checkinIconList = new LinkedList<>();
        checkinList = new ArrayList<>();
        bigCheckinList = new ArrayList<>();
        spotList = new HashMap<>();
        transformMat = new Matrix();
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
        touristMap = new ImageView(context);
        touristMap.setImageBitmap(touristMapBitmap);
        touristMap.setScaleType(ImageView.ScaleType.FIT_START);
        touristMap.setPivotX(0);
        touristMap.setPivotY(0);
        rootLayout.addView(touristMap);

        // draw fog
        fogBitmap = Bitmap.createBitmap(touristMapWidth, touristMapHeight, Bitmap.Config.ARGB_8888);
        if (preferences.getBoolean("fog", false)) {
            Canvas canvas = new Canvas(fogBitmap);
            canvas.drawARGB(120, 0, 0, 0);
        }
        fogMap = new ImageView(context);
        fogMap.setImageBitmap(fogBitmap);
        fogMap.setScaleType(ImageView.ScaleType.FIT_START);
        fogMap.setPivotX(0);
        fogMap.setPivotY(0);
        rootLayout.addView(fogMap);

        // draw edge nodes
        if (preferences.getBoolean("distance_indicator", false)) {
            EdgeNode edgeNode = new EdgeNode(dirPath + mapTag + "_edge_length.txt");
            nodeList = edgeNode.getNodeList();
            for (int i = 0; i < nodeList.size(); i += 2) {
                ImageView nodeImage = new ImageView(context);
                nodeImage.setImageResource(R.drawable.ftprint_black_trans);
                nodeImage.setLayoutParams(new RelativeLayout.LayoutParams(nodeIconWidth, nodeIconHeight));
                nodeImage.setTranslationX(nodeList.get(i) - nodeIconWidth / 2);
                nodeImage.setTranslationY(nodeList.get(i + 1) - nodeIconHeight / 2);
                nodeImageList.add(nodeImage);
                rootLayout.addView(nodeImage);
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
                if (isGpsCurrent) rotateToNorth();
                else translateToCurrent();
            }
        });
        audioBtn = (FloatingActionButton) view.findViewById(R.id.btn_audio);
        audioBtn.setVisibility(View.GONE); // prevent intercepting touch event for float action menu_search
        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AudioCheckinActivity.class);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("mapTag", mapTag);
                startActivity(intent);
                floatingActionsMenu.collapseImmediately();
            }
        });
        photoBtn = (FloatingActionButton) view.findViewById(R.id.btn_photo);
        photoBtn.setVisibility(View.GONE); // prevent intercepting touch event for float action menu_search
        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PhotoCheckinActivity.class);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("mapTag", mapTag);
                startActivity(intent);
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

        progressDialog = new ProgressDialog(context);

        addSpot(24.78778f, 120.9984433f, "中正堂");
        addCheckins(24.7875075f, 120.999106f, 4);

        setTouchListener();

        updateCheckin();

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
    public void  onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu,inflater);

        ArrayList<String> array = new ArrayList<>();
        array.add("交大藝文中心");
        array.add("交大圖書館");
        array.add("交大游泳池");
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.item_search, array);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final ArrayAdapterSearchView searchView = (ArrayAdapterSearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                searchView.setText(adapter.getItem(position).toString());

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
        for (int i = 0; i < nodeImageList.size(); i++) {
            point[0] = nodeList.get(i * 2);
            point[1] = nodeList.get(i * 2 + 1);
            transformMat.mapPoints(point);
            nodeIconTransform.mapPoints(point);
            nodeImageList.get(i).setTranslationX(point[0]);
            nodeImageList.get(i).setTranslationY(point[1]);
        }

        // transform checkinIcon
//        for (int i = 0; i < checkinIconList.size(); i++) {
//            point[0] = checkinList.get(i * 2);
//            point[1] = checkinList.get(i * 2 + 1);
//            transformMat.mapPoints(point);
//            checkinIconTransform.mapPoints(point);
//            checkinIconList.get(i).setTranslationX(point[0]);
//            checkinIconList.get(i).setTranslationY(point[1]);
//        }


        if(scale < 2.5 && isMerged == false) {
            for (ImageNode imageNode : checkinList) {
                imageNode.icon.setVisibility(View.GONE);
            }

            for(Map.Entry<String, SpotNode> spotNodeEntry : spotList.entrySet()) {
                spotNodeEntry.getValue().checkin.setVisibility(View.VISIBLE);
            }

            for (SpotNode spotNode : bigCheckinList) {
                spotNode.checkin.setVisibility(View.VISIBLE);
            }

            isMerged = true;
        }

        if(scale >= 2.5 && isMerged == true) {
            for (ImageNode imageNode : checkinList) {
                imageNode.icon.setVisibility(View.VISIBLE);
            }

            for(Map.Entry<String, SpotNode> spotNodeEntry : spotList.entrySet()) {
                spotNodeEntry.getValue().checkin.setVisibility(View.GONE);
            }

            for (SpotNode spotNode : bigCheckinList) {
                spotNode.checkin.setVisibility(View.GONE);
            }

            isMerged = false;
        }

        // transform spot and its checkins
        for(Map.Entry<String, SpotNode> spotNodeEntry : spotList.entrySet()) {
            SpotNode spotNode = spotNodeEntry.getValue();
            point[0] = spotNode.x;
            point[1] = spotNode.y;
            Matrix spotIconTransform = new Matrix();
            spotIconTransform.postTranslate(-dpToPx(16/2), -dpToPx(16/2));
            transformMat.mapPoints(point);
            spotIconTransform.mapPoints(point);
            spotNode.icon.setTranslationX(point[0]);
            spotNode.icon.setTranslationY(point[1]);
            if (isMerged) {
                Matrix bigCheckinIconTransform = new Matrix();
                bigCheckinIconTransform.postTranslate(-dpToPx(48/2) + dpToPx(16/2), -dpToPx(48) + dpToPx(16/2));
                bigCheckinIconTransform.mapPoints(point);
                spotNode.checkin.setTranslationX(point[0]);
                spotNode.checkin.setTranslationY(point[1]);
            }
        }

        // transform checkins
        if(!isMerged) {
            for (ImageNode imageNode : checkinList) {
                point[0] = imageNode.x;
                point[1] = imageNode.y;
                transformMat.mapPoints(point);
                checkinIconTransform.mapPoints(point);
                imageNode.icon.setTranslationX(point[0]);
                imageNode.icon.setTranslationY(point[1]);
            }
        }

        if(isMerged) {
            for (SpotNode spotNode : bigCheckinList) {
                point[0] = spotNode.x;
                point[1] = spotNode.y;
                transformMat.mapPoints(point);
                Matrix bigCheckinIconTransform = new Matrix();
                bigCheckinIconTransform.postTranslate(-dpToPx(48/2), -dpToPx(48));
                bigCheckinIconTransform.mapPoints(point);
                spotNode.checkin.setTranslationX(point[0]);
                spotNode.checkin.setTranslationY(point[1]);
            }
        }


    }

    private void addCheckins(float spotLat, float spotLng, int checkNum) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View bigCheckin = inflater.inflate(R.layout.item_bigcheckin, null);
        TextView checkinNumCircle = (TextView) bigCheckin.findViewById(R.id.checkin_num);
        checkinNumCircle.setText(String.valueOf(checkNum));

        float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, spotLat, spotLng);
        SpotNode spotNode = new SpotNode(gpsDistorted[0], gpsDistorted[1]);
        spotNode.checkin = bigCheckin;

        // transform gps marker
        Matrix iconTransform = new Matrix();
        iconTransform.postTranslate(-dpToPx(48/2), -dpToPx(48/2));
        transformMat.mapPoints(gpsDistorted);
        iconTransform.mapPoints(gpsDistorted);
        bigCheckin.setTranslationX(gpsDistorted[0]);
        bigCheckin.setTranslationY(gpsDistorted[1]);
        rootLayout.addView(bigCheckin);
        bigCheckinList.add(spotNode);
    }

    private void addSpot(float spotLat, float spotLng, String spotName) {

        // calculate position
        float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, spotLat, spotLng);
        SpotNode spotNode = new SpotNode(gpsDistorted[0], gpsDistorted[1], spotName);

        // create icon
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View spot = inflater.inflate(R.layout.item_spot, null);
        TextView spotNameView = (TextView) spot.findViewById(R.id.spot_name);
        spotNameView.setText(spotName);
        // transform icon
        Matrix iconTransform = new Matrix();
        iconTransform.postTranslate(-dpToPx(16/2), -dpToPx(16/2));
        transformMat.mapPoints(gpsDistorted);
        iconTransform.mapPoints(gpsDistorted);
        spot.setTranslationX(gpsDistorted[0]);
        spot.setTranslationY(gpsDistorted[1]);
        // add to rootlayout
        rootLayout.addView(spot);
        // add into list
        spotNode.icon = spot;
        spotList.put(spotName, spotNode);
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
                        CheckinInfo checkinInfo = issue.getValue(CheckinInfo.class);
                        handleCheckinMsg(issue.getKey(), checkinInfo);
                    }
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });
    }

    private void showDialog(String postId) { // postId: unique key for data query

        progressDialog.show();

        Query query = databaseReference.child("checkin").child(mapTag).child(postId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                if (dataSnapshot.exists()) {
                    CheckinInfo checkinInfo = dataSnapshot.getValue(CheckinInfo.class);
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    if (Objects.equals(checkinInfo.type, "audio")) {
                        AudioCheckinDialogFragment audioCheckinDialogFragment = AudioCheckinDialogFragment.newInstance(checkinInfo);
                        audioCheckinDialogFragment.show(fragmentManager, "fragment_audio_checkin_dialog");
                    } else if (Objects.equals(checkinInfo.type, "photo")) {
                        PhotoCheckinDialogFragment photoCheckinDialogFragment = PhotoCheckinDialogFragment.newInstance(checkinInfo);
                        photoCheckinDialogFragment.show(fragmentManager, "fragment_photo_checkin_dialog");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });
    }

    private void showDialog(String postId, CheckinInfo checkinInfo) { // postId: unique key for data query
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (Objects.equals(checkinInfo.type, "audio")) {
            AudioCheckinDialogFragment audioCheckinDialogFragment = AudioCheckinDialogFragment.newInstance(checkinInfo);
            audioCheckinDialogFragment.show(fragmentManager, "fragment_audio_checkin_dialog");
        } else if (Objects.equals(checkinInfo.type, "photo")) {
            PhotoCheckinDialogFragment photoCheckinDialogFragment = PhotoCheckinDialogFragment.newInstance(checkinInfo);
            photoCheckinDialogFragment.show(fragmentManager, "fragment_photo_checkin_dialog");
        }
    }

    private void translateToCurrent() {
        final float transX = rootLayoutWidth / 2 - gpsMarker.getTranslationX() - gpsMarkerWidth / 2;
        final float transY = rootLayoutHeight / 3 - gpsMarker.getTranslationY() - gpsDirectionHeight - gpsMarkerHeight / 2;
        final float deltaTransX = transX / 10;
        final float deltaTransY = transY / 10;

        final Handler translationHandler = new Handler();
        Runnable translationInterpolation = new Runnable() {
            @Override
            public void run() {
                if (Math.abs(rootLayoutWidth / 2 - gpsMarker.getTranslationX()) < Math.abs(deltaTransX)) {
                    transformMat.postTranslate(
                            rootLayoutWidth / 2 - gpsMarker.getTranslationX() - gpsMarkerWidth / 2,
                            rootLayoutHeight / 3 - gpsMarker.getTranslationY() - gpsDirectionHeight - gpsMarkerHeight / 2);
                    reRender();
                    translationHandler.removeCallbacks(this);
                } else {
                    transformMat.postTranslate(deltaTransX, deltaTransY);
                    reRender();
                    if (Math.abs(rootLayoutWidth / 2 - gpsMarker.getTranslationX()) < rootLayoutHeight / 4) {
                        // slow down
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

    public void handleLocationChange(Location currentLocation) {

        lat = (float) currentLocation.getLatitude();
        lng = (float) currentLocation.getLongitude();
        double imgX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        double imgY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);

        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
//        Log.d(TAG, "handleLocationChange(): idxWeights.idx:" + idxWeights.idx);
        if (idxWeights.idx >= 0) {
            double[] newPos = warpMesh.interpolatePosition(idxWeights);
            gpsDistortedX = (float) newPos[0];
            gpsDistortedY = (float) newPos[1];
        }
//        Log.d(TAG, "handleLocationChange(): gpsDistortedX:" + gpsDistortedX);
//        Log.d(TAG, "handleLocationChange(): gpsDistortedY:" + gpsDistortedY);


        // transform gps marker
        float[] point = new float[]{gpsDistortedX, gpsDistortedY};
        Matrix gpsMarkTransform = new Matrix();
        gpsMarkTransform.postTranslate(-(gpsMarkerWidth / 2), -(gpsMarkerHeight / 2 + gpsDirectionHeight));
        transformMat.mapPoints(point);
        gpsMarkTransform.mapPoints(point);
        gpsMarker.setTranslationX(point[0]);
        gpsMarker.setTranslationY(point[1]);


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
        gpsMarker.setRotation(rotation * RADIAN);
    }

    public void handleCheckinMsg(final String postId, float lat, float lng) {

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
//        checkinList.add(lngDistorted);
//        checkinList.add(latDistorted);

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

    public void handleCheckinMsg(final String postId, final CheckinInfo checkinInfo) {

        float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, Float.valueOf(checkinInfo.lat), Float.valueOf(checkinInfo.lng));
        ImageNode imageNode = new ImageNode(gpsDistorted[0], gpsDistorted[1]);

        // create icon ImageView
        ImageView checkinIcon = new ImageView(context);
        checkinIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location_on_red_600_24dp));
        checkinIcon.setLayoutParams(new RelativeLayout.LayoutParams(checkinIconWidth, checkinIconHeight));
        checkinIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(postId, checkinInfo);
            }
        });
        // transform icon
        Matrix checkInIconTransform = new Matrix();
        checkInIconTransform.postTranslate(-checkinIconWidth / 2, -checkinIconHeight);
        transformMat.mapPoints(gpsDistorted);
        checkInIconTransform.mapPoints(gpsDistorted);
        checkinIcon.setTranslationX(gpsDistorted[0]);
        checkinIcon.setTranslationY(gpsDistorted[1]);
        // add to rootlayout
        rootLayout.addView(checkinIcon);
        // add into list
        imageNode.icon = checkinIcon;
        checkinList.add(imageNode);

        // add into spot
        // find out whether the location is in spotlist or not.
        SpotNode spotNode = spotList.get(checkinInfo.location);
        if (spotNode != null) {
            // this is the first checkin of this spot
            if (spotNode.checkinList.size() == 0) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View bigCheckin = inflater.inflate(R.layout.item_bigcheckin, null);

                // transform gps marker
                Matrix bigCheckinIconTransform = new Matrix();
                bigCheckinIconTransform.postTranslate(-dpToPx(48/2), -dpToPx(48));
                float[] point = new float[]{spotNode.x, spotNode.y};
                transformMat.mapPoints(point);
                bigCheckinIconTransform.mapPoints(point);
                bigCheckin.setTranslationX(point[0]);
                bigCheckin.setTranslationY(point[1]);
                rootLayout.addView(bigCheckin);

                spotNode.checkin = bigCheckin;
            }

            spotNode.checkinList.add(imageNode);
            TextView checkinNumCircle = (TextView) spotNode.checkin.findViewById(R.id.checkin_num);
            checkinNumCircle.setText(String.valueOf(spotNode.checkinList.size()));
        }
    }
//
//    // transform spot and its checkins
//        for(Map.Entry<String, SpotNode> spotNodeEntry : spotList.entrySet()) {
//        SpotNode spotNode = spotNodeEntry.getValue();
//        point[0] = spotNode.x;
//        point[1] = spotNode.y;
//        Matrix spotIconTransform = new Matrix();
//        spotIconTransform.postTranslate(-dpToPx(16/2), -dpToPx(16/2));
//        transformMat.mapPoints(point);
//        spotIconTransform.mapPoints(point);
//        spotNode.icon.setTranslationX(point[0]);
//        spotNode.icon.setTranslationY(point[1]);
//        if (isMerged) {
//            Matrix bigCheckinIconTransform = new Matrix();
//            bigCheckinIconTransform.postTranslate(-dpToPx(48/2) + dpToPx(16/2), -dpToPx(48) + dpToPx(16/2));
//            bigCheckinIconTransform.mapPoints(point);
//            spotNode.checkin.setTranslationX(point[0]);
//            spotNode.checkin.setTranslationY(point[1]);
//        }
//    }

}
