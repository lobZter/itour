package nctu.cs.cgv.itour.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private Checkin checkin;

    public PhotoCheckinDialogFragment() {
    }

    public static PhotoCheckinDialogFragment newInstance(Checkin checkin) {
        PhotoCheckinDialogFragment photoCheckinDialogFragment = new PhotoCheckinDialogFragment();
        photoCheckinDialogFragment.checkin = checkin;
        return photoCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

}
