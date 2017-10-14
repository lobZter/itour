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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class GpsLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "GpsLocationService";
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

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
//        AsyncHttpClient client = new AsyncHttpClient();
//        client.get("https://food-map-lobst3rd.c9users.io/gpsUpdate?lat=" + location.getLatitude() + "&lng=" + location.getLongitude(),
//                new AsyncHttpResponseHandler() {
//
//                    @Override
//                    public void onStart() {
//                        // called before request is started
//                    }
//
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
//                        // called when response HTTP status is "200 OK"
//                    }
//
//                    @Override
//                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
//                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
//                    }
//
//                    @Override
//                    public void onRetry(int retryNo) {
//                        // called when request is retried
//                    }
//                });


        sendMessage(location);
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
//        locationRequest.setInterval(5000);
//        locationRequest.setFastestInterval(2000);
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

    private void sendMessage(Location location) {
        // send message to activities by broadcasting
        Intent intent = new Intent("gpsLocation");
        intent.putExtra("lat", (float) location.getLatitude());
        intent.putExtra("lng", (float) location.getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}