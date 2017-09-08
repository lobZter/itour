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

public class CheckinListFragment extends Fragment {

    private static final String TAG = "CheckinListFragment";
    private List<Checkin> checkins;
    private CheckinItemAdapter checkinItemAdapter;
    private ListView checkinList;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        databaseReference = FirebaseDatabase.getInstance().getReference();

        checkinList = (ListView) view.findViewById(R.id.checkin_list);

        // all checkins
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

}
