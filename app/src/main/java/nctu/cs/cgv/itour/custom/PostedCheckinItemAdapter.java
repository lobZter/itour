package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.util.List;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;
import static nctu.cs.cgv.itour.activity.MainActivity.savedPostId;

/**
 * Created by lobst3rd on 2017/10/10.
 */

public class PostedCheckinItemAdapter extends ArrayAdapter<Checkin> {

    private static final String TAG = "PostedCheckinItemAdapter";
    private Context context;

    public PostedCheckinItemAdapter(Context context, List<Checkin> checkinItems) {
        super(context, 0, checkinItems);
        this.context = context;
    }

    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        final Checkin checkin = getItem(position);

        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.item_posted_checkin_card, parent, false);
            viewHolder.username = (TextView) view.findViewById(R.id.tv_username);
            viewHolder.location = (TextView) view.findViewById(R.id.tv_location);
            viewHolder.like = (TextView) view.findViewById(R.id.tv_like);
            viewHolder.description = (TextView) view.findViewById(R.id.tv_description);

            viewHolder.photo = (ImageView) view.findViewById(R.id.photo);

            viewHolder.audioLayout = view.findViewById(R.id.audio);
            viewHolder.audioDivider = view.findViewById(R.id.audio_divider);
            viewHolder.playBtn = (ImageView) view.findViewById(R.id.btn_play);
            viewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
            viewHolder.progressTextCurrent = (TextView) view.findViewById(R.id.tv_progress_current);
            viewHolder.progressTextDuration = (TextView) view.findViewById(R.id.tv_progress_duration);

            viewHolder.likeBtn = (Button) view.findViewById(R.id.btn_like);
            viewHolder.saveBtn = (Button) view.findViewById(R.id.btn_save);
            viewHolder.deleteBtn = (Button) view.findViewById(R.id.btn_delete);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.username.setText(checkin.username);
        viewHolder.location.setText(checkin.location);
        viewHolder.description.setText(checkin.description);

        int likeNum = checkin.likeNum;
        if (checkin.like != null && checkin.like.size() > 0) {
            likeNum += checkin.like.size();
        }
        String likeStr = likeNum > 0 ? String.valueOf(likeNum) + context.getString(R.string.checkin_card_like_num) : "";
        viewHolder.like.setText(likeStr);

        setPhoto(viewHolder, checkin.photo);
        setAudio(viewHolder, checkin.audio);
        setActionBtn(viewHolder, checkin);

        return view;
    }

    private void setPhoto(final ViewHolder viewHolder, final String filename) {

        if (filename.equals("")) {
            viewHolder.photo.setVisibility(View.GONE);
            return;
        } else {
            viewHolder.photo.setVisibility(View.VISIBLE);
        }

        final File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + filename).exists()) {
            // load photo from storage
            Bitmap bitmap = BitmapFactory.decodeFile(externalCacheDir.toString() + "/" + filename);
            viewHolder.photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            viewHolder.photo.setImageBitmap(bitmap);
        } else {
            // download photo
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(context) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    Bitmap bitmap = BitmapFactory.decodeFile(response.toString());
                    viewHolder.photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    viewHolder.photo.setImageBitmap(bitmap);

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

    private void setAudio(final ViewHolder viewHolder, final String filename) {

        if (filename.equals("")) {
            viewHolder.audioLayout.setVisibility(View.GONE);
            viewHolder.audioDivider.setVisibility(View.GONE);
            return;
        } else {
            viewHolder.audioLayout.setVisibility(View.VISIBLE);
            viewHolder.audioDivider.setVisibility(View.VISIBLE);
        }

        final MediaPlayer[] mediaPlayer = new MediaPlayer[1];
        final Handler progressBarHandler = new Handler();
        final Runnable progressBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer[0] != null && mediaPlayer[0].isPlaying()) {
                    viewHolder.progressBar.setProgress(mediaPlayer[0].getCurrentPosition() * 100 / mediaPlayer[0].getDuration());
                    String str = String.format("%d:%02d", mediaPlayer[0].getCurrentPosition() / 1000, (mediaPlayer[0].getCurrentPosition() % 1000) * 60 / 1000);
                    viewHolder.progressTextCurrent.setText(str);
                }
                progressBarHandler.postDelayed(this, 100);
            }
        };

        viewHolder.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer[0] != null) {
                    if (mediaPlayer[0].isPlaying())
                        pauseAudio(viewHolder, mediaPlayer[0]);
                    else
                        playAudio(viewHolder, mediaPlayer[0]);
                }
            }
        });

        final File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + filename).exists()) {
            mediaPlayer[0] = initAudio(viewHolder, externalCacheDir.toString() + "/" + filename, progressBarHandler, progressBarRunnable);
        } else {
            // download audio
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(context) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    mediaPlayer[0] = initAudio(viewHolder, response.toString(), progressBarHandler, progressBarRunnable);

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

    private void setActionBtn(final ViewHolder viewHolder, final Checkin checkin) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            viewHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, context.getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });

            viewHolder.saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, context.getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            viewHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                        viewHolder.likeBtn.setTextColor(ContextCompat.getColor(context, R.color.md_black_1000));
                        viewHolder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
                        String likeStr = "";
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.likeNum + checkin.like.size() - 1) + context.getString(R.string.checkin_card_like_num);
                        }
                        viewHolder.like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).removeValue();
                        checkin.like.remove(uid);
                        checkinMap.get(checkin.key).like.remove(uid);
                        actionLog("cancel like checkin: " + checkin.location + ", " + checkin.key);
                    } else {
                        viewHolder.likeBtn.setTextColor(ContextCompat.getColor(context, R.color.md_red_500));
                        viewHolder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
                        String likeStr;
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.likeNum + checkin.like.size() + 1) + context.getString(R.string.checkin_card_like_num);
                        } else {
                            likeStr = String.valueOf(checkin.likeNum + 1) + getContext().getString(R.string.checkin_card_like_num);
                        }
                        viewHolder.like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                        checkin.like.put(uid, true);
                        checkinMap.get(checkin.key).like.put(uid, true);
                        actionLog("like checkin: " + checkin.location + ", " + checkin.key);
                    }
                }
            });

            viewHolder.saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                        viewHolder.saveBtn.setTextColor(ContextCompat.getColor(context, R.color.md_black_1000));
                        viewHolder.saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).removeValue();
                        savedPostId.remove(checkin.key);
                        actionLog("cancel checkin: " + checkin.location + ", " + checkin.key);
                    } else {
                        viewHolder.saveBtn.setTextColor(ContextCompat.getColor(context, R.color.gps_marker_color));
                        viewHolder.saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).setValue(true);
                        savedPostId.put(checkin.key, true);
                        actionLog("save checkin: " + checkin.location + ", " + checkin.key);
                    }
                }
            });

            viewHolder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.dialog_delete_title)
                            .setMessage(R.string.dialog_delete_message)
                            .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                                    FirebaseDatabase.getInstance().getReference().child("checkin").child(mapTag).child(checkin.key).removeValue();
                                    actionLog("remove posted checkin: " + checkin.toMap().toString());
                                    checkinMap.remove(checkin.key);
                                    remove(checkin);
                                }
                            })
                            .setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                }
            });

            if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                viewHolder.likeBtn.setTextColor(ContextCompat.getColor(context, R.color.md_red_500));
                viewHolder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
            } else {
                viewHolder.likeBtn.setTextColor(ContextCompat.getColor(context, R.color.md_black_1000));
                viewHolder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
            }

            if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                viewHolder.saveBtn.setTextColor(ContextCompat.getColor(context, R.color.gps_marker_color));
                viewHolder.saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
            } else {
                viewHolder.saveBtn.setTextColor(ContextCompat.getColor(context, R.color.md_black_1000));
                viewHolder.saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0);
            }
        }
    }

    private MediaPlayer initAudio(final ViewHolder viewHolder, final String filePath, final Handler progressBarHandler, final Runnable progressBarRunnable) {
        viewHolder.progressBar.setProgress(0);
        viewHolder.progressTextCurrent.setText(context.getString(R.string.default_start_time));
        viewHolder.progressTextDuration.setText(context.getString(R.string.default_start_time));

        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progressBarHandler.removeCallbacksAndMessages(null);
                initAudio(viewHolder, filePath, progressBarHandler, progressBarRunnable);
            }
        });
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();

            String str = String.format("%d:%02d", mediaPlayer.getDuration() / 1000, (mediaPlayer.getDuration() % 1000) * 60 / 1000);
            viewHolder.progressTextDuration.setText(str);

            progressBarHandler.postDelayed(progressBarRunnable, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        viewHolder.playBtn.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_play_arrow_black_48dp, null));
        return mediaPlayer;
    }

    private void playAudio(final ViewHolder viewHolder, final MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        viewHolder.playBtn.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pause_black_48dp, null));
    }

    private void pauseAudio(final ViewHolder viewHolder, final MediaPlayer mediaPlayer) {
        mediaPlayer.pause();
        viewHolder.playBtn.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_play_arrow_black_48dp, null));
    }

    private static class ViewHolder {
        TextView username;
        TextView location;
        TextView like;
        TextView description;

        ImageView photo;

        View audioLayout;
        View audioDivider;
        ImageView playBtn;
        ProgressBar progressBar;
        TextView progressTextCurrent;
        TextView progressTextDuration;

        Button likeBtn;
        Button saveBtn;
        Button deleteBtn;
    }
}