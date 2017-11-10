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
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;
import static nctu.cs.cgv.itour.activity.MainActivity.savedPostId;

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
        TextView like = (TextView) view.findViewById(R.id.tv_like);
        TextView description = (TextView) view.findViewById(R.id.tv_description);

        if (checkin != null) {
            username.setText(checkin.username);
            location.setText(checkin.location);
            description.setText(checkin.description);

            String likeStr = "";
            if (checkin.like != null && checkin.like.size() > 0) {
                likeStr = String.valueOf(checkin.like.size()) + getContext().getString(R.string.checkin_card_like_num);
            }
            like.setText(likeStr);


            setPhoto(view, checkin.photo);
            setAudio(view, checkin.audio);
            setActionBtn(view, checkin);
        } else {
            username.setText("");
            location.setText("");
            description.setText(getString(R.string.tv_checkin_remove));
            like.setText("");

            setPhoto(view, "");
            setAudio(view, "");
        }
    }

    private void setPhoto(View view, final String filename) {

        final ImageView photo = (ImageView) view.findViewById(R.id.photo);

        if (filename.equals("")) {
            photo.setVisibility(View.GONE);
            return;
        }

        final File externalCacheDir = getContext().getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + filename).exists()) {
            // load photo from storage
            Bitmap bitmap = BitmapFactory.decodeFile(externalCacheDir.toString() + "/" + filename);
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photo.setImageBitmap(bitmap);
        } else {
            // download photo
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    Bitmap bitmap = BitmapFactory.decodeFile(response.toString());
                    photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    photo.setImageBitmap(bitmap);

                    if (externalCacheDir != null) {
                        String path = response.toString();
                        String dirPath = path.substring(0, path.lastIndexOf("/"));
                        File rename = new File(dirPath + "/" + filename);
                        response.renameTo(rename);
                        moveFile(dirPath, filename, externalCacheDir.toString());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                }
            });
        }
    }

    private void setAudio(View view, final String filename) {

        if (filename.equals("")) {
            View audioLayout = view.findViewById(R.id.audio);
            View audioDivider = view.findViewById(R.id.audio_divider);
            audioLayout.setVisibility(View.GONE);
            audioDivider.setVisibility(View.GONE);
            return;
        }

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

        final File externalCacheDir = getContext().getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + filename).exists()) {
            initAudio(externalCacheDir.toString() + "/" + filename);
        } else {
            // download audio
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(getContext()) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    initAudio(response.toString());

                    if (externalCacheDir != null) {
                        String path = response.toString();
                        String dirPath = path.substring(0, path.lastIndexOf("/"));
                        File rename = new File(dirPath + "/" + filename);
                        response.renameTo(rename);
                        moveFile(dirPath, filename, externalCacheDir.toString());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

                }
            });
        }
    }

    private void setActionBtn(View view, final Checkin checkin) {
        final Button likeBtn = (Button) view.findViewById(R.id.btn_like);
        final Button saveBtn = (Button) view.findViewById(R.id.btn_save);
        final TextView like = (TextView) view.findViewById(R.id.tv_like);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                        likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                        likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
                        String likeStr = "";
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.like.size() - 1) + getContext().getString(R.string.checkin_card_like_num);
                        }
                        like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).removeValue();
                        checkin.like.remove(uid);
                        checkinMap.get(checkin.key).like.remove(uid);
                        actionLog("cancel like checkin: " + checkin.location + ", " + checkin.key);
                    } else {
                        likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                        likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
                        String likeStr;
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.like.size() + 1) + getContext().getString(R.string.checkin_card_like_num);
                        } else {
                            likeStr = "1" + getContext().getString(R.string.checkin_card_like_num);
                        }
                        like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                        checkin.like.put(uid, true);
                        checkinMap.get(checkin.key).like.put(uid, true);
                        actionLog("like checkin: " + checkin.location + ", " + checkin.key);
                    }
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                        saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                        saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).removeValue();
                        savedPostId.remove(checkin.key);
                        actionLog("cancel save checkin: " + checkin.location + ", " + checkin.key);
                    } else {
                        saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                        saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).setValue(true);
                        savedPostId.put(checkin.key, true);
                        actionLog("save checkin: " + checkin.location + ", " + checkin.key);
                    }
                }
            });

            if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
            }

            if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
            }
        }
    }

    private void initAudio(final String filePath) {
        progressBar.setProgress(0);
        progressTextCurrent.setText(getString(R.string.default_start_time));
        progressTextDuration.setText(getString(R.string.default_start_time));

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progressBarHandler.removeCallbacksAndMessages(null);
                audioReady = false;
                initAudio(filePath);
            }
        });
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();

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
        isPlaying = false;
        playBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_black_48dp, null));
    }

    private void playAudio() {
        mediaPlayer.start();

        isPlaying = true;
        playBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_black_48dp, null));
    }

    private void pauseAudio() {
        mediaPlayer.pause();

        isPlaying = false;
        playBtn.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_black_48dp, null));
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
