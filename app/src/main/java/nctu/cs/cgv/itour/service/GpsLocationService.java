package nctu.cs.cgv.itour.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.Utility;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.SpotNode;
import nctu.cs.cgv.itour.object.UserData;

import static nctu.cs.cgv.itour.MyApplication.APPServerURL;
import static nctu.cs.cgv.itour.MyApplication.gpsLogPath;
import static nctu.cs.cgv.itour.MyApplication.latitude;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.longitude;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.spotList;

public class GpsLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "GpsLocationService";
    private final float FOG_UPDATE_THRESHOLD = 0.00001f;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private float lastFogClearLat = 0;
    private float lastFogClearLng = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!googleApiClient.isConnected())
            googleApiClient.connect();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient.isConnected()) {
            stopLocationUpdate();
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected()");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(), errorCode: " + String.valueOf(cause));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(), errorCode: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = (float) location.getLatitude();
        longitude = (float) location.getLongitude();

        sendGpsUpdate();

        // check whether it should update fog or not
        double distance = Math.sqrt(Math.pow(lastFogClearLat - location.getLatitude(), 2.0) + Math.pow(lastFogClearLng - location.getLongitude(), 2.0));
        if (distance > FOG_UPDATE_THRESHOLD) {
            gpsLog(location);
            sendFogUpdate();
            lastFogClearLat = (float) location.getLatitude();
            lastFogClearLng = (float) location.getLongitude();
        }
    }

    private void gpsLog(Location location) {
        if (!logFlag || FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/gpsUpdate";
        RequestParams requestParams = new RequestParams();
        requestParams.put("lat", String.valueOf(location.getLatitude()));
        requestParams.put("lng", String.valueOf(location.getLongitude()));
        requestParams.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        requestParams.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        requestParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        client.post(url, requestParams, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });

        File file = new File(gpsLogPath + "/" + "gpsLog-" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            OutputStreamWriter file_writer = new OutputStreamWriter(new FileOutputStream(file, true));
            BufferedWriter buffered_writer = new BufferedWriter(file_writer);
            buffered_writer.write("{" + requestParams.toString() + "},\n");
            buffered_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void initLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdate() {
        initLocationRequest();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void sendGpsUpdate() {
        // send message to activities by broadcasting
        Intent intent = new Intent("gpsUpdate");
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendFogUpdate() {
        // send message to activities by broadcasting
        Intent intent = new Intent("fogUpdate");
        intent.putExtra("lat", latitude);
        intent.putExtra("lng", longitude);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}