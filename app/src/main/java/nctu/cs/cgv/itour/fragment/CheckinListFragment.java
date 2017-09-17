package nctu.cs.cgv.itour.fragment;

import android.graphics.Color;
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

public class CheckinListFragment extends Fragment {

    private static final String TAG = "CheckinListFragment";
    private List<Checkin> checkins;
    private CheckinItemAdapter checkinItemAdapter;
    private SwipeMenuListView checkinList;
    private DatabaseReference databaseReference;

    public CheckinListFragment() {
        // Required empty public constructor
    }

    public static CheckinListFragment newInstance() {
        CheckinListFragment fragment = new CheckinListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_swipe_menu_list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        checkinList = (SwipeMenuListView) view.findViewById(R.id.list_view);
        checkinList.setMenuCreator(new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem mapItem = new SwipeMenuItem(getContext());
                mapItem.setBackground(R.color.gps_marker_color);
                mapItem.setWidth(dpToPx(getContext(), 72));
                mapItem.setIcon(R.drawable.ic_gps_fixed_white_24dp);
                mapItem.setTitle("locate");
                mapItem.setTitleColor(Color.WHITE);
                mapItem.setTitleSize(12);
                menu.addMenuItem(mapItem);

                SwipeMenuItem viewItem = new SwipeMenuItem(getContext());
                viewItem.setBackground(R.color.md_blue_grey_500);
                viewItem.setWidth(dpToPx(getContext(), 72));
                viewItem.setIcon(R.drawable.ic_picture_in_picture_white_24dp);
                viewItem.setTitle("view");
                viewItem.setTitleColor(Color.WHITE);
                viewItem.setTitleSize(12);
                menu.addMenuItem(viewItem);

            }
        });
        checkinList.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        break;
                    case 1:
                        break;
                }
                return false; // false : close the menu; true : not close the menu
            }
        });
        checkinList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        // all mergedCheckinNode
        Query query = databaseReference.child("checkin").child(mapTag);
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

    public void addCheckin(final Checkin checkin) {
        checkinItemAdapter.add(checkin);
    }

}
