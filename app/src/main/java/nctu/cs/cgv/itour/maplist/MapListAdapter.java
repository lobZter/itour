package nctu.cs.cgv.itour.maplist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.Config;
import nctu.cs.cgv.itour.R;

/**
 * Created by lobZter on 2017/6/21.
 */

public class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.ViewHolder> {

    private Context context;
    private List<MapListItem> mapList;

    public MapListAdapter(Context context, List<MapListItem> mapList) {
        this.context = context;
        this.mapList = mapList;
    }

    @Override
    public int getItemCount() {
        return mapList.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_touristmap, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        MapListItem currentItem = mapList.get(position);
        holder.title.setText(currentItem.mapName);

        String thumbURL = Config.URL + currentItem.mapThumb;
        final String thumbPath = Config.dirPath + "thumb/" + currentItem.mapThumb;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        File thumbFile = new File(thumbPath);

        if(thumbFile.exists()) {
            holder.thumb.setImageBitmap(BitmapFactory.decodeFile(thumbPath, options));
        }
        else {
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(thumbURL, new FileAsyncHttpResponseHandler(thumbFile) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    holder.thumb.setImageBitmap(BitmapFactory.decodeFile(thumbPath, options));
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

                }
            });
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView thumb;

        ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            thumb = (ImageView) view.findViewById(R.id.thumb);
        }
    }
}
