package nctu.cs.cgv.itour.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

/**
 * Created by lobZter on 2017/7/11.
 */

public class PhotoCheckinDialogFragment extends DialogFragment {

    private static final String TAG = "PhotoCheckinDialogFragment";
    private String postId;

    private DatabaseReference databaseReference;

    public PhotoCheckinDialogFragment() {
    }

    public static PhotoCheckinDialogFragment newInstance(String postId) {
        PhotoCheckinDialogFragment photoCheckinDialogFragment = new PhotoCheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        photoCheckinDialogFragment.setArguments(args);
        return photoCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Checkin checkin = checkinMap.get(postId);

        ((TextView) view.findViewById(R.id.tv_location)).setText(checkin.location);
        ((TextView) view.findViewById(R.id.tv_description)).setText(checkin.description);
        ((TextView) view.findViewById(R.id.tv_name)).setText(checkin.username);
        final ImageView photo = (ImageView) view.findViewById(R.id.photo);

        final String path = getContext().getCacheDir().toString() + "/" + checkin.filename;
        File file = new File(path);
        if(file.exists()) {
            // load thumb from storage
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photo.setImageBitmap(bitmap);
        }
        else {
            // download thumb
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + checkin.filename, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    response.renameTo(new File(path));
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    photo.setImageBitmap(bitmap);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                }
            });
        }

        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
        final LinearLayout likeBtn = (LinearLayout) view.findViewById(R.id.btn_like);
        final ImageView likeIcon = (ImageView) likeBtn.getChildAt(0);
        final TextView likeText = (TextView) likeBtn.getChildAt(1);
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkin.like.get(uid)) {
                    likeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_border_black_24dp));
                    likeText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(false);
                    checkinMap.get(postId).like.put(uid, false);
                } else {
                    likeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_red_500_24dp));
                    likeText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                    checkinMap.get(postId).like.put(uid, true);
                }
            }
        });

        LinearLayout saveBtn = (LinearLayout) view.findViewById(R.id.btn_save);
        final ImageView saveIcon = (ImageView) saveBtn.getChildAt(0);
        final TextView saveText = (TextView) saveBtn.getChildAt(1);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkinMap.get(postId).saved) {
                    saveIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_border_black_24dp));
                    saveText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(false);
                } else {
                    saveIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_blue_24dp));
                    saveText.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(true);
                }
                checkinMap.get(postId).saved = !checkinMap.get(postId).saved;
            }
        });

        if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
            likeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_red_500_24dp));
            likeText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
        }

        if (checkin.saved) {
            saveIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_blue_24dp));
            saveText.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

}
