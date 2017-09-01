package nctu.cs.cgv.itour;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.fragment.AudioCheckinDialogFragment;
import nctu.cs.cgv.itour.fragment.PhotoCheckinDialogFragment;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;

/**
 * Created by lobZter on 2017/8/18.
 */

public class CheckinItemAdapter extends ArrayAdapter<Checkin> {

    private FragmentManager fragmentManager;

    public CheckinItemAdapter(Context context, List<Checkin> checkins, FragmentManager fragmentManager) {
        super(context, 0, checkins);
        this.fragmentManager = fragmentManager;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Checkin checkin = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_checkin, parent, false);
        }

        TextView username = (TextView) convertView.findViewById(R.id.username);
        TextView location = (TextView) convertView.findViewById(R.id.location);
        TextView description = (TextView) convertView.findViewById(R.id.description);
        username.setText(checkin.username);
        location.setText(checkin.location);
        description.setText(checkin.description);

        final ImageView photo = (ImageView) convertView.findViewById(R.id.photo);
        if (checkin.type.equals("audio")) {
            photo.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_mic_black_48dp));
        }
        if (checkin.type.equals("photo")) {
            final String path = getContext().getCacheDir().toString() + "/" + checkin.filename;
            File file = new File(path);
            if (file.exists()) {
                // load thumb from storage
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                photo.setImageBitmap(bitmap);
            } else {
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
        }

//        convertView.findViewById(R.id.parent_layout).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (Objects.equals(checkin.type, "audio")) {
//                    AudioCheckinDialogFragment audioCheckinDialogFragment = AudioCheckinDialogFragment.newInstance(checkin);
//                    audioCheckinDialogFragment.show(fragmentManager, "fragment_audio_checkin_dialog");
//                } else if (Objects.equals(checkin.type, "photo")) {
//                    PhotoCheckinDialogFragment photoCheckinDialogFragment = PhotoCheckinDialogFragment.newInstance(checkin);
//                    photoCheckinDialogFragment.show(fragmentManager, "fragment_photo_checkin_dialog");
//                }
//            }
//        });

        return convertView;
    }
}
