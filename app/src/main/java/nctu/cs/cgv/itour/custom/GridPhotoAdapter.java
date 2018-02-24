package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;

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
            Glide.with(context)
                    .load(fileDownloadURL + "?filename=" + filename)
                    .into(photo);
        }

        return view;
    }
}