package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.moveFile;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;
import static nctu.cs.cgv.itour.activity.MainActivity.savedPostId;

/**
 * Created by lobZter on 2017/8/18.
 */

public class CheckinItemAdapter extends RecyclerView.Adapter<CheckinItemAdapter.ViewHolder> {

    private static final String TAG = "CheckinItemAdapter";
    private ArrayList<Checkin> checkins;
    private Context context;

    public CheckinItemAdapter(Context context, ArrayList<Checkin> checkins) {
        this.checkins = checkins;
        this.context = context;
    }

    private Context getContext() {
        return context;
    }

    @Override
    public CheckinItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View checkinCardView = inflater.inflate(R.layout.item_checkin_card, parent, false);

        ViewHolder viewHolder = new ViewHolder(checkinCardView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CheckinItemAdapter.ViewHolder viewHolder, int position) {
        Checkin checkin = checkins.get(position);

        viewHolder.username.setText(checkin.username);
        viewHolder.location.setText(checkin.location);
        viewHolder.description.setText(checkin.description);

        int likeNum = checkin.likeNum;
        if (checkin.like != null && checkin.like.size() > 0) {
            likeNum += checkin.like.size();
        }
        viewHolder.like.setText(String.valueOf(likeNum));

        setPhoto(viewHolder, checkin.photo);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return checkins.size();
    }

    public Checkin getItem(int index) {
        return checkins.get(index);
    }

    public void addAll(Collection<Checkin> checkinList) {
        checkins.addAll(checkinList);
        notifyDataSetChanged();
    }

    public void add(Checkin checkin) {
        checkins.add(checkin);
        notifyDataSetChanged();
    }

    public void clear() {
        checkins.clear();
        notifyDataSetChanged();
    }

    public void insert(Checkin checkin, int index) {
        checkins.add(index, checkin);
        notifyItemInserted(index);
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView username;
        TextView location;
        TextView like;
        TextView description;

        public ViewHolder(View view) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(view);

            photo = view.findViewById(R.id.photo);
            username = view.findViewById(R.id.tv_username);
            location = view.findViewById(R.id.tv_location);
            like = view.findViewById(R.id.tv_like);
            description = view.findViewById(R.id.tv_description);
        }
    }
}
