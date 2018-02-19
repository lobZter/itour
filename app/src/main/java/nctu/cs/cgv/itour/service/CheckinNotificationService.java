package nctu.cs.cgv.itour.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
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

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.activity.MainActivity.CHECKIN_NOTIFICATION_REQUEST;

public class CheckinNotificationService extends Service {

    private NotificationManager notificationManager;
    private int CUSTOM_ID = 666;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("notification").child(mapTag);

        query.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                nctu.cs.cgv.itour.object.Notification notification = dataSnapshot.getValue(nctu.cs.cgv.itour.object.Notification.class);
                if (notification.targetUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) || notification.targetUid.equals("all"))
                    notifyCheckin(notification);
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

        return START_NOT_STICKY;
    }

    private void notifyCheckin(nctu.cs.cgv.itour.object.Notification notification) {

        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
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
        notificationBuilder.setVibrate(new long[] {0, 300, 300, 300, 300});
        notificationBuilder.setContentTitle(notification.title);
        notificationBuilder.setContentText(notification.msg);
        notificationBuilder.setContentIntent(intent);

//        Notification notification = notificationBuilder.build();
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;

//        notificationManager.cancelAll();
        notificationManager.notify(CUSTOM_ID, notificationBuilder.build());
    }
}
