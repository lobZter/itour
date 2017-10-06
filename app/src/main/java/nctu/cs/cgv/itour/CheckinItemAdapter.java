package nctu.cs.cgv.itour;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.fragment.SavedCheckinFragment;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.moveFile;

/**
 * Created by lobZter on 2017/8/18.
 */

public class CheckinItemAdapter extends ArrayAdapter<Checkin> {

    private Context context;

    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private ImageView playBtn;

    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    public CheckinItemAdapter(Context context, List<Checkin> checkinItems) {
        super(context, 0, checkinItems);
        this.context = context;
    }

    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        final Checkin checkin = getItem(position);

        view = LayoutInflater.from(getContext()).inflate(R.layout.item_checkin_card, parent, false);

        TextView username = (TextView) view.findViewById(R.id.tv_username);
        TextView location = (TextView) view.findViewById(R.id.tv_location);
        TextView description = (TextView) view.findViewById(R.id.tv_description);
        username.setText(checkin.username);
        location.setText(checkin.location);
        description.setText(checkin.description);


        playBtn = (ImageView) view.findViewById(R.id.btn_play);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        progressTextCurrent = (TextView) view.findViewById(R.id.tv_progress_current);
        progressTextDuration = (TextView) view.findViewById(R.id.tv_progress_duration);
        progressBarHandler = new Handler();

        // set photo
        final ImageView photo = (ImageView) view.findViewById(R.id.photo);
        final String photoPath = getContext().getExternalCacheDir().toString() + "/" + checkin.photo;
        File photoFile = new File(photoPath);
        if (photoFile.exists()) {
            // load photo from storage
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photo.setImageBitmap(bitmap);
        } else {
            // download photo
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + checkin.photo, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    moveFile(getContext().getCacheDir().toString(),
                            checkin.photo,
                            getContext().getExternalCacheDir().toString());
                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                    photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    photo.setImageBitmap(bitmap);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                }
            });
        }

        // set action buttons
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final Button likeBtn = (Button) view.findViewById(R.id.btn_like);
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                    likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(false);
                } else {
                    likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                }
            }
        });

        final Button saveBtn = (Button) view.findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                if (checkin.saved) {
                    saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0);
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(false);
                } else {
                    saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                    saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(true);
                }
                checkin.saved = !checkin.saved;
            }
        });

        Button locateBtn = (Button) view.findViewById(R.id.btn_locate);
        locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).onLocateClick(checkin);
            }
        });

        if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
            likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
            likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
        }

        if (checkin.saved) {
            saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
            saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }
}
