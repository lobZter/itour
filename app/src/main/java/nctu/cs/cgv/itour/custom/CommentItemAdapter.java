package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.Comment;

public class CommentItemAdapter extends RecyclerView.Adapter<CommentItemAdapter.ViewHolder> {

    private static final String TAG = "CommentItemAdapter";
    private ArrayList<Comment> comments;

    public CommentItemAdapter(Context context, ArrayList<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View commentItemView = inflater.inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(commentItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Comment comment = comments.get(position);
        viewHolder.username.setText(comment.username);
        viewHolder.msg.setText(comment.msg);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return comments.size();
    }

    public Comment getItem(int index) {
        return comments.get(index);
    }

    public void addAll(Collection<Comment> commentCollection) {
        comments.addAll(commentCollection);
        notifyDataSetChanged();
    }

    public void add(Comment comment) {
        comments.add(comment);
        notifyDataSetChanged();
    }

    public void clear() {
        comments.clear();
        notifyDataSetChanged();
    }

    public void insert(Comment comment, int index) {
        comments.add(index, comment);
        notifyItemInserted(index);
    }

    public void remove(int index) {
        comments.remove(index);
        notifyItemRemoved(index);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        TextView msg;

        ViewHolder(View view) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(view);

            username = view.findViewById(R.id.tv_username);
            msg = view.findViewById(R.id.tv_msg);
        }
    }
}