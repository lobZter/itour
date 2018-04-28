package nctu.cs.cgv.itour;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Notification;

import static nctu.cs.cgv.itour.MyApplication.APPServerURL;
import static nctu.cs.cgv.itour.MyApplication.actionLogPath;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.warpMesh;
import static nctu.cs.cgv.itour.activity.MainActivity.CHECKIN_NOTIFICATION_REQUEST;

/**
 * Created by lobZter on 2017/6/21.
 */

public class Utility {

    private static final String TAG = "Utility";

    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static int spToPx(Context context, float sp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()));
    }

    public static float[] gpsToImgPx(float lat, float lng) {
        float realMeshX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        float realMeshY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(realMeshX, realMeshY);
        if (idxWeights.idx >= 0) {
            return warpMesh.interpolatePosition(idxWeights);
        } else {
            return new float[]{-1, -1};
        }
    }

    public static float[] imgPxToGps(float imgX, float imgY) {
        float[] realMeshPos = new float[]{0, 0};
        IdxWeights idxWeights = warpMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            realMeshPos = realMesh.interpolatePosition(idxWeights);
            realMeshPos[0] = realMeshPos[0] / realMesh.mapWidth * (realMesh.maxLon - realMesh.minLon) + realMesh.minLon;
            realMeshPos[1] = realMesh.maxLat - realMeshPos[1] / realMesh.mapHeight * (realMesh.maxLat - realMesh.minLat);
        }
        return realMeshPos;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + "/" + inputFile);
            out = new FileOutputStream(outputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + "/" + inputFile).delete();


        } catch (Exception e) {
            Log.e("moveFile", e.getMessage());
        }
    }

    public static void actionLog(String log, String location, String postId) {
        if (!logFlag || FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/actionLog";
        RequestParams requestParams = new RequestParams();
        requestParams.put("log", log);
        if (location == null || location.equals("")) {
            requestParams.put("location", "location");
        } else {
            requestParams.put("location", location);
        }
        requestParams.put("postId", postId);
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

        File file = new File(actionLogPath + "/" + "actionLog-" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".json");
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

    public static float gpsToMeter(float lat1, float lon1, float lat2, float lon2) {
        double R = 6378.137; // Radius of earth in KM
        double dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        double dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return (float) d * 1000; // meters
    }

    public static float isNearBy(float lat1, float lon1, float lat2, float lon2) {
        if (Math.abs(lat1 - lat2) >= 0.0091f || Math.abs(lon1 - lon2) >= 0.0091f) {
            return 999f;
        } else {
            return gpsToMeter(lat1, lon1, lat2, lon2);
        }
    }

//    public void pushNotification(final Checkin checkin) {
//        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
//        String msg = checkin.location.equals("") ? checkin.description : checkin.location + " | " + checkin.description;
//        final String notificationKey = databaseReference.child("notification").child(mapTag).push().getKey();
//        final Notification notification = new Notification(checkin.key,
//                checkin.uid,
//                "all",
//                checkin.username,
//                msg,
//                checkin.photo,
//                checkin.location,
//                checkin.lat,
//                checkin.lng,
//                System.currentTimeMillis() / 1000);
//        Map<String, Object> notificationValues = notification.toMap();
//        Map<String, Object> notificationUpdates = new HashMap<>();
//        notificationUpdates.put("/notification/" + mapTag + "/" + notificationKey, notificationValues);
//        databaseReference.updateChildren(notificationUpdates, new DatabaseReference.CompletionListener() {
//            @Override
//            public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
//                actionLog("push notification: " + notification.msg, checkin.location, checkin.key);
//            }
//        });
//    }

    public static void pushNews(final nctu.cs.cgv.itour.object.Notification notification, String notificationKey) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (notificationKey.equals(""))
            notificationKey =  databaseReference.child("users").child(uid).child("news").child(mapTag).push().getKey();
        Map<String, Object> notificationValues = notification.toMap();
        Map<String, Object> notificationUpdates = new HashMap<>();
        notificationUpdates.put("/users/" + uid + "/news/" + mapTag + "/" + notificationKey, notificationValues);
        databaseReference.updateChildren(notificationUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                actionLog("push news", notification.location, notification.postId);
            }
        });
    }

    public static void notifyCheckin(Context context,
                                     nctu.cs.cgv.itour.object.Notification notification,
                                     NotificationManager notificationManager,
                                     String channelId) {
//        Bitmap icon;
//        if (!notification.photo.equals("")) {
//            try {
//                icon = Glide.with(getApplicationContext())
//                        .asBitmap()
//                        .load(fileDownloadURL + "?filename=" + notification.photo)
//                        .submit()
//                        .get();
//            } catch (InterruptedException e) {
//                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
//            } catch (ExecutionException e) {
//                icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
//            }
//        } else {
//            icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
//        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra("checkinNotificationIntent", true);
        notificationIntent.putExtra("lat", notification.lat);
        notificationIntent.putExtra("lng", notification.lng);
        notificationIntent.putExtra("key", notification.postId);
        notificationIntent.putExtra("location", notification.location);
        notificationIntent.putExtra("title", notification.title);
        notificationIntent.putExtra("msg", notification.msg);
        PendingIntent intent = PendingIntent.getActivity(context, CHECKIN_NOTIFICATION_REQUEST, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
//        notificationBuilder.setLargeIcon(icon);
        notificationBuilder.setVibrate(new long[]{0, 300, 300, 300, 300});
        notificationBuilder.setContentTitle(notification.title);
        notificationBuilder.setContentText(notification.msg);
        notificationBuilder.setContentIntent(intent);
        notificationBuilder.setChannelId(channelId);

        android.app.Notification builtNotification = notificationBuilder.build();
        builtNotification.flags |= android.app.Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify((int) (System.currentTimeMillis() / 1000), builtNotification);
    }
}
