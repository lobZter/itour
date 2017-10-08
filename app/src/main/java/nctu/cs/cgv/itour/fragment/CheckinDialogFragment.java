package nctu.cs.cgv.itour.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

public class CheckinDialogFragment extends DialogFragment {

    private static final String TAG = "CheckinDialogFragment";

    private String postId;

    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private ImageView playBtn;
    private Handler progressBarHandler;
    private Runnable progressBarRunnable;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean audioReady = false;

    public static CheckinDialogFragment newInstance(String postId) {
        CheckinDialogFragment checkinDialogFragment = new CheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        checkinDialogFragment.setArguments(args);
        return checkinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Checkin checkin = checkinMap.get(postId);

        TextView username = (TextView) view.findViewById(R.id.tv_username);
        TextView location = (TextView) view.findViewById(R.id.tv_location);
        TextView description = (TextView) view.findViewById(R.id.tv_description);
        username.setText(checkin.username);
        location.setText(checkin.location);
        description.setText(checkin.description);

        setPhoto(view, checkin);
        setAudio(view, checkin);
        setActionBtn(view, checkin);
    }

    private void setPhoto(View view, final Checkin checkin) {
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
    }

    private void setAudio(View view, Checkin checkin) {
        playBtn = (ImageView) view.findViewById(R.id.btn_play);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioReady) {
                    if (isPlaying)
                        pauseAudio();
                    else
                        playAudio();
                }
            }
        });

        progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        progressTextCurrent = (TextView) view.findViewById(R.id.tv_progress_current);
        progressTextDuration = (TextView) view.findViewById(R.id.tv_progress_duration);
        progressBarHandler = new Handler();

        final String audioPath = getContext().getExternalCacheDir().toString() + "/" + checkin.audio;
        File audioFile = new File(audioPath);
        if (audioFile.exists()) {
            // load thumb from storage
            initAudio(audioPath);
        } else {
            // download thumb
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + checkin.audio, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    response.renameTo(new File(audioPath));
                    initAudio(audioPath);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

                }
            });
        }
    }

    private void setActionBtn(View view, final Checkin checkin) {
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

        if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
            likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
            likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
        }

        if (checkin.saved) {
            saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
            saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
        }
    }


    private void initAudio(final String filePath) {
        progressBar.setProgress(0);
        progressTextCurrent.setText("0:00");
        progressTextDuration.setText("0:00");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    progressBarHandler.removeCallbacks(progressBarRunnable);

                    isPlaying = false;
                    playBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_play_arrow_black_48dp));
                    initAudio(filePath);
                }
            });

            String str = String.format("%d:%02d", mediaPlayer.getDuration() / 1000, (mediaPlayer.getDuration() % 1000) * 60 / 1000);
            progressTextDuration.setText(str);

            progressBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPlaying && mediaPlayer != null) {
                        progressBar.setProgress(mediaPlayer.getCurrentPosition() * 100 / mediaPlayer.getDuration());
                        String str = String.format("%d:%02d", mediaPlayer.getCurrentPosition() / 1000, (mediaPlayer.getCurrentPosition() % 1000) * 60 / 1000);
                        progressTextCurrent.setText(str);
                    }
                    progressBarHandler.postDelayed(this, 100);
                }
            };
            progressBarRunnable.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioReady = true;
        playBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_48dp));
    }

    private void playAudio() {
        mediaPlayer.start();

        isPlaying = true;
        playBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_pause_black_48dp));
    }

    private void pauseAudio() {
        mediaPlayer.pause();

        isPlaying = false;
        playBtn.setImageDrawable(getContext().getDrawable(R.drawable.ic_play_arrow_black_48dp));
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
