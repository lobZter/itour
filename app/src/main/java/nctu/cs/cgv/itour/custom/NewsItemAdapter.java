package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Notification;

/**
 * Created by lobZter on 2017/7/10.
 */

public class NewsItemAdapter extends RecyclerView.Adapter<NewsItemAdapter.ViewHolder> {

    private static final String TAG = "NewsItemAdapter";
    private ArrayList<Notification> notifications;
    private Context context;

    public NewsItemAdapter(Context context, ArrayList<Notification> notifications) {
        this.notifications = notifications;
        this.context = context;
    }

    private Context getContext() {
        return context;
    }

    @Override
    public NewsItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item_news, parent, false);

        NewsItemAdapter.ViewHolder viewHolder = new NewsItemAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(NewsItemAdapter.ViewHolder viewHolder, int position) {
        Notification notification = notifications.get(position);

        viewHolder.title.setText(notification.title);
        viewHolder.msg.setText(notification.msg);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public Notification getItem(int index) {
        return notifications.get(index);
    }

    public void addAll(Collection<Notification> notificationList) {
        notifications.addAll(notificationList);
        notifyDataSetChanged();
    }

    public void add(Notification notification) {
        notifications.add(notification);
        notifyDataSetChanged();
    }

    public void clear() {
        notifications.clear();
        notifyDataSetChanged();
    }

    public void insert(Notification notification, int index) {
        notifications.add(index, notification);
        notifyItemInserted(index);
    }

    public void remove(int index) {
        notifications.remove(index);
        notifyItemRemoved(index);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView msg;

        public ViewHolder(View view) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(view);

            title = view.findViewById(R.id.tv_title);
            msg = view.findViewById(R.id.tv_msg);
        }
    }
}