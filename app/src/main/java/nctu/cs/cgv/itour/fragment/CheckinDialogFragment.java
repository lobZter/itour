package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.custom.CommentItemAdapter;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.Comment;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;
import static nctu.cs.cgv.itour.activity.MainActivity.savedPostId;

public class CheckinDialogFragment extends DialogFragment {

    private static final String TAG = "CheckinDialogFragment";

    private String postId;

    public static CheckinDialogFragment newInstance(String postId) {
        CheckinDialogFragment checkinDialogFragment = new CheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        checkinDialogFragment.setArguments(args);
        return checkinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Checkin checkin = checkinMap.get(postId);
        actionLog("browse checkin", checkin.location, checkin.key);

        TextView username = view.findViewById(R.id.tv_username);
        TextView location = view.findViewById(R.id.tv_location);
        TextView like = view.findViewById(R.id.tv_like);
        TextView description = view.findViewById(R.id.tv_description);

        if (checkin != null) {
            username.setText(checkin.username);
            location.setText(checkin.location);
            description.setText(checkin.description);

            int likeNum = checkin.likeNum;
            if (checkin.like != null && checkin.like.size() > 0) {
                likeNum += checkin.like.size();
            }
            String likeStr = likeNum > 0 ? String.valueOf(likeNum) + Objects.requireNonNull(getContext()).getString(R.string.checkin_card_like_num) : "";
            like.setText(likeStr);

            setPhoto(view, checkin.photo);
            setActionBtn(view, checkin);
            setComment(view, checkin);
        } else {
            username.setText("");
            location.setText("");
            description.setText(getString(R.string.tv_checkin_remove));
            like.setText("");

            setPhoto(view, "");
        }
    }

    private void setPhoto(final View view, final String filename) {

        final ImageView photo = view.findViewById(R.id.photo);

        if (filename.equals("")) {
            photo.setVisibility(View.GONE);
            return;
        }

        Glide.with(Objects.requireNonNull(getContext()))
                .load(fileDownloadURL + "?filename=" + filename)
                .apply(new RequestOptions().placeholder(R.drawable.ic_broken_image_black_48dp))
                .into(photo);
    }

    private void setActionBtn(final View view, final Checkin checkin) {
        final Button likeBtn = view.findViewById(R.id.btn_like);
        final Button saveBtn = view.findViewById(R.id.btn_save);
        final TextView like = view.findViewById(R.id.tv_like);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), getString(R.string.toast_guest_function), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            likeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                        likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                        likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border_black_24dp, 0, 0, 0);
                        String likeStr = "";
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.likeNum + checkin.like.size() - 1) + getContext().getString(R.string.checkin_card_like_num);
                        }
                        like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).removeValue();
                        checkin.like.remove(uid);
                        checkinMap.get(checkin.key).like.remove(uid);
                        actionLog("cancel like checkin", checkin.location, checkin.key);
                    } else {
                        likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                        likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
                        String likeStr;
                        if (checkin.like != null && checkin.like.size() > 0) {
                            likeStr = String.valueOf(checkin.likeNum + checkin.like.size() + 1) + getContext().getString(R.string.checkin_card_like_num);
                        } else {
                            likeStr = String.valueOf(checkin.likeNum + 1) + getContext().getString(R.string.checkin_card_like_num);
                        }
                        like.setText(likeStr);
                        databaseReference.child("checkin").child(mapTag).child(checkin.key).child("like").child(uid).setValue(true);
                        checkin.like.put(uid, true);
                        checkinMap.get(checkin.key).like.put(uid, true);
                        actionLog("like checkin", checkin.location, checkin.key);
                    }
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                        saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_black_1000));
                        saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).removeValue();
                        savedPostId.remove(checkin.key);
                        actionLog("cancel save checkin", checkin.location, checkin.key);
                    } else {
                        saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                        saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
                        databaseReference.child("users").child(uid).child("saved").child(mapTag).child(checkin.key).setValue(true);
                        savedPostId.put(checkin.key, true);
                        actionLog("save checkin", checkin.location, checkin.key);
                    }
                }
            });

            if (checkin.like.containsKey(uid) && checkin.like.get(uid)) {
                likeBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.md_red_500));
                likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_red_500_24dp, 0, 0, 0);
            }

            if (savedPostId.containsKey(checkin.key) && savedPostId.get(checkin.key)) {
                saveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.gps_marker_color));
                saveBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bookmark_blue_24dp, 0, 0, 0);
            }
        }
    }

    private void setComment(final View view, final Checkin checkin) {
        View commentDivider = view.findViewById(R.id.comment_divider);
        RecyclerView commentList = view.findViewById(R.id.lv_comment);
        RelativeLayout commentEdit = view.findViewById(R.id.comment_edit);
        TextView commentUsername = view.findViewById(R.id.tv_comment_username);
        final EditText commentMsg = view.findViewById(R.id.et_comment_msg);
        ImageView sendBtn = view.findViewById(R.id.btn_comment_send);

        // set comment list
        if (checkin.comment.size() > 0) {
            commentDivider.setVisibility(View.VISIBLE);
            commentList.setVisibility(View.VISIBLE);
            ArrayList<Comment> comments = new ArrayList<>(checkin.comment.values());
            CommentItemAdapter commentItemAdapter = new CommentItemAdapter(getContext(), comments);
            commentList.setAdapter(commentItemAdapter);
            commentList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true));
        }

        // set comment edit
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            commentEdit.setVisibility(View.VISIBLE);
            commentUsername.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            sendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // send comment
                    String msg = commentMsg.getText().toString().trim();
                    if (msg.equals("")) return;

                    Comment comment = new Comment(msg,
                            FirebaseAuth.getInstance().getCurrentUser().getUid(),
                            FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                            System.currentTimeMillis() / 1000);
                    String pushKey = FirebaseDatabase.getInstance().getReference()
                            .child("checkin").child(mapTag).child(checkin.key).child("comment")
                            .push().getKey();

                    Map<String, Object> commentValue = comment.toMap();
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/checkin/" + mapTag + "/" + checkin.key + "/comment/" + pushKey, commentValue);
                    FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates,
                            new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {

                                }
                            });
                }
            });
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // set dialog layout
        Objects.requireNonNull(getDialog().getWindow())
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT,     // width
                        WindowManager.LayoutParams.WRAP_CONTENT);    // height
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
