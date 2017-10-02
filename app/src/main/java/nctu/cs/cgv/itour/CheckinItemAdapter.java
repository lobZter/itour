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

/**
 * Created by lobZter on 2017/8/18.
 */

public class CheckinItemAdapter extends ArrayAdapter<Checkin> {

    private static final int PHOTO_VIEW_CODE = 0;
    private static final int AUDIO_VIEW_CODE = 1;

    private Context context;

    private ProgressBar progressBar;
    private TextView progressTextCurrent;
    private TextView progressTextDuration;
    private ImageView playBtn;

    private Handler progressBarHandler;
    private Runnable progressBarRunnable;

    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private boolean audioReady = false;

    public CheckinItemAdapter(Context context, List<Checkin> checkinItems) {
        super(context, 0, checkinItems);
        this.context = context;
    }

    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        final Checkin checkin = getItem(position);

        if (getItemViewType(position) == AUDIO_VIEW_CODE)
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_audio_checkin_card, parent, false);
        else
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_photo_checkin_card, parent, false);

        TextView username = (TextView) view.findViewById(R.id.tv_username);
        TextView location = (TextView) view.findViewById(R.id.tv_location);
        username.setText(checkin.username);
        location.setText(checkin.location);

        if (checkin.type.equals("audio")) {
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

            final String path = getContext().getCacheDir().toString() + "/" + checkin.filename;
            File file = new File(path);
            if (file.exists()) {
                initAudio(path);
            } else {
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
        }

        if (checkin.type.equals("photo")) {
            TextView description = (TextView) view.findViewById(R.id.tv_description);
            description.setText(checkin.description);
            final ImageView photo = (ImageView) view.findViewById(R.id.photo);
            final String path = getContext().getCacheDir().toString() + "/" + checkin.filename;
            File file = new File(path);
            if (file.exists()) {
                // load photo from storage
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                photo.setImageBitmap(bitmap);
            } else {
                // download photo
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
        }

        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        final LinearLayout likeBtn = (LinearLayout) view.findViewById(R.id.btn_like);
        final ImageView likeIcon = (ImageView) likeBtn.getChildAt(0);
        final TextView likeText = (TextView) likeBtn.getChildAt(1);
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                    likeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_border_black_24dp));
                    likeText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(false);
                } else {
                    likeIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_red_500_24dp));
                    likeText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                    databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                }
            }
        });

        final LinearLayout saveBtn = (LinearLayout) view.findViewById(R.id.btn_save);
        final ImageView saveIcon = (ImageView) saveBtn.getChildAt(0);
        final TextView saveText = (TextView) saveBtn.getChildAt(1);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkin.saved) {
                    saveIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_border_black_24dp));
                    saveText.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(false);
                } else {
                    saveIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_blue_24dp));
                    saveText.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                    databaseReference.child("user").child(uid).child("saved").child(checkin.key).setValue(true);
                }
                checkin.saved = !checkin.saved;
            }
        });

        final LinearLayout locateBtn = (LinearLayout) view.findViewById(R.id.btn_locate);
        locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) context).onLocateClick(checkin);
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

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).type.equals("audio"))
            return AUDIO_VIEW_CODE;
        else
            return PHOTO_VIEW_CODE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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
        playBtn.setImageDrawable(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.ic_play_arrow_black_48dp, null));
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

}
