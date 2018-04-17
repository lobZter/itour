package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.fileDownloadURL;
import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.appLog;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Checkin checkin = checkinMap.get(postId);

        appLog("CheckinDialogFragment onViewCreated: " + postId);
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
            String likeStr = likeNum > 0 ? String.valueOf(likeNum) + getContext().getString(R.string.checkin_card_like_num) : "";
            like.setText(likeStr);


            setPhoto(view, checkin.photo);
            setActionBtn(view, checkin);
        } else {
            username.setText("");
            location.setText("");
            description.setText(getString(R.string.tv_checkin_remove));
            like.setText("");

            setPhoto(view, "");
        }
    }

    private void setPhoto(View view, final String filename) {

        final ImageView photo = (ImageView) view.findViewById(R.id.photo);

        if (filename.equals("")) {
            photo.setVisibility(View.GONE);
            return;
        }

        Glide.with(getContext())
                .load(fileDownloadURL + "?filename=" + filename)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_broken_image_black_48dp)
                        .centerCrop())
                .into(photo);
    }

    private void setActionBtn(View view, final Checkin checkin) {
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

    @Override
    public void onStart() {
        super.onStart();
        // set dialog layout
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }
}
