package nctu.cs.cgv.itour;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.maplist.DownloadFileAsyncTask;
import nctu.cs.cgv.itour.maplist.MapListAdapter;
import nctu.cs.cgv.itour.maplist.MapListItem;
import nctu.cs.cgv.itour.maplist.RecyclerItemClickListener;

import static nctu.cs.cgv.itour.MyApplication.serverURL;
import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.config.Utility.dpToPx;

public class MapListActivity extends AppCompatActivity {

    private final String TAG = "MapListActivity";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MapListAdapter mapListAdapter;
    private List<MapListItem> mapListItems;
    public static String mapTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_list);

        initCollapsingToolbar();
        initRecyclerView();
        initSwipeRefreshLayout();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
    }

    private void initCollapsingToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(" ");
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setExpanded(true);

        // hiding & showing the title when toolbar expanded & collapsed
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
    }

    private void initRecyclerView() {

        mapListItems = new ArrayList<>();
        mapListAdapter = new MapListAdapter(MapListActivity.this, mapListItems);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setAdapter(mapListAdapter);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(MapListActivity.this, 2);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(MapListActivity.this, 10), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        mapTag = mapListItems.get(position).mapTag;
                        File distortedMapFile = new File(dirPath + mapTag + "_distorted_map.png");
                        File meshFile = new File(dirPath + mapTag + "_mesh.txt");
                        File warpMeshFile = new File(dirPath + mapTag + "_warpMesh.txt");
                        File boundBoxFile = new File(dirPath + mapTag + "_bound_box.txt");
                        File edgeLengthFile = new File(dirPath + mapTag + "_edge_length.txt");
                        if (distortedMapFile.exists() && meshFile.exists() && warpMeshFile.exists() && boundBoxFile.exists() && edgeLengthFile.exists()) {
                            Intent intent = new Intent(MapListActivity.this, MapActivity.class);
                            intent.putExtra("MAP", mapTag);
                            startActivity(intent);
                        } else {
                            new DownloadFileAsyncTask(MapListActivity.this).execute(mapTag);
                        }
                    }
                })
        );
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                downloadAndUpdate();
            }
        });
    }

    private void downloadAndUpdate() {
        final String jsonFilePath = dirPath + "jsonexample.json";
        final File jsonFile = new File(jsonFilePath);

        // download then call updateAdapter()
        // TODO automatic download at the first time
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(serverURL + "jsonexample.json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    // delete old file before save
                    jsonFile.delete();
                    jsonFile.getParentFile().mkdirs();
                    jsonFile.createNewFile();

                    FileOutputStream fileOutputStream = new FileOutputStream(jsonFilePath);
                    fileOutputStream.write(responseBody);
                    fileOutputStream.close();
                    updateAdapter(new File(jsonFilePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MapListActivity.this, "Network error, can not download map list.", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void initListItem() {
        final String jsonFilePath = dirPath + "jsonexample.json";
        final File jsonFile = new File(jsonFilePath);

        if (!jsonFile.exists()) {
            downloadAndUpdate();
        } else {
            updateAdapter(jsonFile);
        }
    }

    private void updateAdapter(File file) {

        try {
            // file to json string
            FileInputStream inputSteam = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputSteam));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            String jsonString = stringBuilder.toString();
            reader.close();
            inputSteam.close();

            // Parse json and update list adapter
            JSONArray jsonArray = new JSONArray(jsonString);
            mapListItems.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json_data = jsonArray.getJSONObject(i);
                MapListItem touristMapPackage = new MapListItem();
                touristMapPackage.mapThumb = json_data.optString("map_img");
                touristMapPackage.mapName = json_data.optString("map_name");
                touristMapPackage.mapTag = json_data.optString("map_tag");
                mapListItems.add(touristMapPackage);
            }
            mapListAdapter.notifyDataSetChanged();
        } catch (IOException | JSONException e) {
            if (e instanceof JSONException) file.delete();
            e.printStackTrace();
            Toast.makeText(MapListActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        } finally {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int gpsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (gpsPermission + storagePermission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation();
            } else {
                requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            initListItem();
        }
    }

    private void showExplanation() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Needed")
                .setMessage("We need to store map package on the device and track your GPS location to run this app!")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_MULTIPLE_REQUEST);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean gpsPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storagePermission && gpsPermission)
                    {
                        initListItem();
                    } else {
                        showExplanation();
                    }
                }
                break;
        }
    }
}
