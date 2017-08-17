package nctu.cs.cgv.itour.fragment;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.photoPath;

/**
 * Created by lobZter on 2017/7/11.
 */

public class PhotoCheckinDialogFragment extends DialogFragment {

    private static final String TAG = "PhotoCheckinDialogFragment";

    private TextView locationText;
    private TextView descriptionText;
    private ImageView photo;

    private String location;
    private String description;
    private String filename;

    public PhotoCheckinDialogFragment() {
    }

    public static PhotoCheckinDialogFragment newInstance(Checkin checkin) {
        PhotoCheckinDialogFragment photoCheckinDialogFragment = new PhotoCheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("location", checkin.location);
        args.putString("description", checkin.description);
        args.putString("filename", checkin.filename);
        photoCheckinDialogFragment.setArguments(args);
        return photoCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        location = getArguments().getString("location", "");
        description = getArguments().getString("description", "");
        filename = getArguments().getString("filename", "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationText = (TextView) view.findViewById(R.id.tv_location);
        descriptionText = (TextView) view.findViewById(R.id.tv_description);
        photo = (ImageView) view.findViewById(R.id.photo);

        locationText.setText(location);
        descriptionText.setText(description);

        final String path = photoPath + filename;
        File file = new File(path);

        if(file.exists()) {
            // load thumb from storage
            photo.setImageBitmap(BitmapFactory.decodeFile(path));
        }
        else {
            // download thumb
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(file) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    photo.setImageBitmap(BitmapFactory.decodeFile(path));
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

                }
            });
        }
    }

}
