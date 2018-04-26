package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collection;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;

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

    @NonNull
    @Override
    public CheckinItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View checkinCardView = inflater.inflate(R.layout.item_checkin_card, parent, false);

        return new ViewHolder(checkinCardView);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckinItemAdapter.ViewHolder viewHolder, int position) {
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

    public void remove(int index) {
        checkins.remove(index);
        notifyItemRemoved(index);
    }

    private void setPhoto(final ViewHolder viewHolder, final String filename) {

        if (filename.equals("")) {
            viewHolder.photo.setVisibility(View.GONE);
            return;
        } else {
            viewHolder.photo.setVisibility(View.VISIBLE);
        }

        Glide.with(context)
                .load(fileDownloadURL + "?filename=" + filename)
                .apply(new RequestOptions().placeholder(R.drawable.ic_broken_image_black_48dp))
                .into(viewHolder.photo);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView username;
        TextView location;
        TextView like;
        TextView description;

        ViewHolder(View view) {
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
