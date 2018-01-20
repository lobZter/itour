package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.Utility.moveFile;

/**
 * Created by lobZter on 2018/1/20.
 */

public class GridPhotoAdapter extends ArrayAdapter<Checkin> {

    private Context context;

    public GridPhotoAdapter(Context context, ArrayList<Checkin> checkins) {
        super(context, 0, checkins);
        this.context = context;
    }

    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.item_grid_photo, parent, false);

        final ImageView photo = (ImageView) view.findViewById(R.id.photo);

        Checkin checkin = getItem(position);
        final String filename = checkin.photo;
        if (!filename.equals("")) {
            final File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && new File(externalCacheDir.toString() + "/" + filename).exists()) {
                // load photo from storage
                Bitmap bitmap = BitmapFactory.decodeFile(externalCacheDir.toString() + "/" + filename);
                photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                photo.setImageBitmap(bitmap);
            } else {
                // download photo
                AsyncHttpClient client = new AsyncHttpClient();
                client.get(fileDownloadURL + "?filename=" + filename, new FileAsyncHttpResponseHandler(context) {
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

        return view;
    }
}