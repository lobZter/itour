package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.CheckinItemAdapter;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.dpToPx;

public class PostedCheckinFragment extends Fragment {

    private static final String TAG = "PostedCheckinFragment";
    private List<Checkin> checkins;
    private CheckinItemAdapter checkinItemAdapter;
    private SwipeMenuListView checkinList;
    private DatabaseReference databaseReference;

    public PostedCheckinFragment() {
        // Required empty public constructor
    }

    public static PostedCheckinFragment newInstance() {
        PostedCheckinFragment fragment = new PostedCheckinFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posted_checkin, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        checkinList = (SwipeMenuListView) view.findViewById(R.id.list_view);

        checkinList.setMenuCreator(new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem deleteItem = new SwipeMenuItem(getContext());
                deleteItem.setBackground(R.color.md_grey_700);
                deleteItem.setWidth(dpToPx(getContext(), 90));
                deleteItem.setIcon(R.drawable.ic_delete_white_24dp);
                menu.addMenuItem(deleteItem);
            }
        });
        checkinList.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        // remove checkin
                        String key = checkins.get(position).key;
                        databaseReference.child("checkin").child(mapTag).child(key).removeValue();

                        for (Checkin checkin : checkins) {
                            if (key.equals(checkin.key)) {
                                checkins.remove(checkin);
                                break;
                            }
                        }
                        checkinItemAdapter.clear();
                        checkinItemAdapter.addAll(checkins);
                        checkins.remove(position);
                        checkinItemAdapter.clear();
                        checkinItemAdapter.addAll(checkins);
                        break;
                }
                return false; // false : close the menu; true : not close the menu
            }
        });
        checkinList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);


        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query query = databaseReference.child("checkin").child(mapTag).orderByChild("uid").equalTo(uid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                checkins = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Checkin checkin = issue.getValue(Checkin.class);
                        checkin.key = issue.getKey();
                        checkins.add(checkin);
                    }
                }
                checkinItemAdapter = new CheckinItemAdapter(getContext(), new ArrayList<>(checkins), getActivity().getSupportFragmentManager());
                checkinList.setAdapter(checkinItemAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });
    }

}