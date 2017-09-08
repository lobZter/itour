package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

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

public class SavedCheckinFragment extends Fragment {

    private static final String TAG = "SavedCheckinFragment";
    private List<Checkin> checkins;
    private CheckinItemAdapter checkinItemAdapter;
    private ListView checkinList;
    private DatabaseReference databaseReference;

    public SavedCheckinFragment() {
        // Required empty public constructor
    }

    public static SavedCheckinFragment newInstance() {
        SavedCheckinFragment fragment = new SavedCheckinFragment();
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
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

//        checkins = new ArrayList<>();
        checkinItemAdapter = new CheckinItemAdapter(getContext(), new ArrayList<Checkin>(), getActivity().getSupportFragmentManager());
        checkinList = (ListView) view.findViewById(R.id.list_view);
        checkinList.setAdapter(checkinItemAdapter);

        final List<String> checkinIds = new ArrayList<>();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query saveQuery = databaseReference.child("user").child(uid).child("saved");
        saveQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        if (Boolean.valueOf(issue.getValue().toString())) {
                            checkinIds.add(issue.getKey());
                        }
                    }
                }

                // query every checkin
                for (final String postId : checkinIds) {
                    Query query = databaseReference.child("checkin").child(mapTag).child(postId);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Checkin checkin = dataSnapshot.getValue(Checkin.class);
                                checkin.key = postId;
                                checkinItemAdapter.add(checkin);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "for (String postId: checkinIds): onCancelled", databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "checkinIds: onCancelled", databaseError.toException());
            }
        });
    }
}
