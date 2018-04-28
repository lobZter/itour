package nctu.cs.cgv.itour.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.Utility;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.UserData;

import static nctu.cs.cgv.itour.MyApplication.latitude;
import static nctu.cs.cgv.itour.MyApplication.longitude;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.activity.MainActivity.CHECKIN_NOTIFICATION_REQUEST;

public class CheckinNotificationService extends Service {
    private static final String TAG = "CheckinNotification";
    private NotificationManager notificationManager;
    private String channelId = "checkin notification";
    private long currentTimestamp;
    private String uid;
    private UserData userData = null;
    private Query notiQuery;
    private ChildEventListener notiListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "打卡通知",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 300, 300, 300});
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            this.stopSelf();
            return START_NOT_STICKY;
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        currentTimestamp = System.currentTimeMillis() / 1000;
        notiQuery = databaseReference.child("notification").child(mapTag);
        notiListener = notiQuery.orderByChild("timestamp").startAt(currentTimestamp).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    nctu.cs.cgv.itour.object.Notification notification =
                            dataSnapshot.getValue(nctu.cs.cgv.itour.object.Notification.class);
                    if (notification == null) return;
                    if (notification.uid.equals(uid)) return;
                    if (notification.targetUid.equals("all") || notification.targetUid.equals(uid))
                        notifyCheckin(notification);
                } catch (Exception ignore) {

                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return START_STICKY;
    }

    private void checkDistance(nctu.cs.cgv.itour.object.Notification notification, String notificationKey) {
        float dist = Utility.gpsToMeter(latitude, longitude, Float.valueOf(notification.lat), Float.valueOf(notification.lng));
        if (dist <= 100f) {
            notification.title += "在離你" + String.valueOf((int) dist) + "公尺的地方打卡了";
            notifyCheckin(notification);
            pushNews(notification, notificationKey);
        } else if (300f <= dist && dist <= 600f) {
            notification.title += "在你周圍打卡了";
            notifyCheckin(notification);
            pushNews(notification, notificationKey);
        }
    }

    private void pushNews(final nctu.cs.cgv.itour.object.Notification notification, String notificationKey) {
        Map<String, Object> notificationValues = notification.toMap();
        Map<String, Object> notificationUpdates = new HashMap<>();
        notificationUpdates.put("/users/" + uid + "/data/" + mapTag + "/news/" + notificationKey, notificationValues);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.updateChildren(notificationUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                actionLog("push news", notification.location, notification.postId);
            }
        });
    }

    private void notifyCheckin(nctu.cs.cgv.itour.object.Notification notification) {
        Bitmap icon;
        final File externalCacheDir = getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + notification.photo).exists()) {
            // load photo from storage
            icon = BitmapFactory.decodeFile(externalCacheDir.toString() + "/" + notification.photo);
        } else {
            icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra("checkinNotificationIntent", true);
        notificationIntent.putExtra("lat", notification.lat);
        notificationIntent.putExtra("lng", notification.lng);
        notificationIntent.putExtra("key", notification.postId);
        notificationIntent.putExtra("title", notification.title);
        notificationIntent.putExtra("msg", notification.msg);
        PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), CHECKIN_NOTIFICATION_REQUEST, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setLargeIcon(icon);
        notificationBuilder.setVibrate(new long[]{0, 300, 300, 300, 300});
        notificationBuilder.setContentTitle(notification.title);
        notificationBuilder.setContentText(notification.msg);
        notificationBuilder.setContentIntent(intent);
        notificationBuilder.setChannelId(channelId);

        Notification builtNotification = notificationBuilder.build();
        builtNotification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify((int) (System.currentTimeMillis() / 1000), builtNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notiQuery.removeEventListener(notiListener);
    }
}
