package nctu.cs.cgv.itour.fragment;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
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
import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

public class AudioCheckinDialogFragment extends DialogFragment {

    private static final String TAG = "AudioCheckinDialogFragment";

    private String postId;

    private DatabaseReference databaseReference;

    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private ImageView playBtn;

    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private boolean audioReady = false;

    public AudioCheckinDialogFragment() {
    }

    public static AudioCheckinDialogFragment newInstance(String postId) {
        AudioCheckinDialogFragment audioCheckinDialogFragment = new AudioCheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        audioCheckinDialogFragment.setArguments(args);
        return audioCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();
        progressBarHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Checkin checkin = checkinMap.get(postId);

        ((TextView) view.findViewById(R.id.tv_name)).setText(checkin.username);
        ((TextView) view.findViewById(R.id.tv_location)).setText(checkin.location);

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

        final String path = getContext().getCacheDir().toString() + "/" + checkin.filename;
        File file = new File(path);
        if (file.exists()) {
            // load thumb from storage
            initAudio(path);
        } else {
            // download thumb
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + checkin.filename, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    response.renameTo(new File(path));
                    initAudio(path);
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
