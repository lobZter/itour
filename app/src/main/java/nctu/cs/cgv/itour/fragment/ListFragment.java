package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

/**
 * Created by lobst3rd on 2017/8/18.
 */

public class ListFragment extends Fragment {

    private static final String TAG = "ListFragment";
    private List<Checkin> checkins;
    private List<Checkin> myCheckins;
    private CheckinItemAdapter checkinItemAdapter;
    private boolean isMyCheckins = false;

    public static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final ListView checkinList = (ListView) view.findViewById(R.id.checkin_list);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("checkin").child(mapTag);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                checkins = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Checkin checkin = issue.getValue(Checkin.class);
                        checkins.add(checkin);
                    }
                }
                Log.d(TAG, "" + checkins.size());
                checkinItemAdapter = new CheckinItemAdapter(getContext(), new ArrayList<>(checkins));
                checkinList.setAdapter(checkinItemAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });

        query = databaseReference.child("checkin").child(mapTag).orderByChild("uid").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myCheckins = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        Checkin checkin = issue.getValue(Checkin.class);
                        myCheckins.add(checkin);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "updateCheckin(): onCancelled", databaseError.toException());
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_filter, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_filter:
                checkinItemAdapter.clear();
                if (isMyCheckins) {
                    Log.d(TAG, "" + checkins.size());
                    checkinItemAdapter.addAll(checkins);
                } else {
                    checkinItemAdapter.addAll(myCheckins);
                }
                isMyCheckins = !isMyCheckins;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
//
//    Filter filter = new Filter() {
//
//        @Override
//        protected void publishResults(CharSequence constraint, FilterResults results) {
//            checkins = (List<Checkin>) results.values; // has the filtered values
//            checkinItemAdapter.notifyDataSetChanged();  // notifies the data with new filtered values
//        }
//
//        @Override
//        protected FilterResults performFiltering(CharSequence constraint) {
//            FilterResults results = new FilterResults(); // Holds the results of a filtering operation in values
//            List<Checkin> FilteredArrList = new ArrayList<Checkin>();
//
//            if (originalValues == null) {
//                originalValues = new ArrayList<Checkin>(checkins); // saves the original data in mOriginalValues
//            }
//
//            if (constraint == null || constraint.length() == 0) {
//                // set the Original result to return
//                results.count = originalValues.size();
//                results.values = originalValues;
//            } else {
//                constraint = constraint.toString().toLowerCase();
//                for (int i = 0; i < originalValues.size(); i++) {
//                    Checkin data = originalValues.get(i);
//                    if (data.uid.equals(constraint.toString())) {
//                        FilteredArrList.add(data);
//                    }
//                }
//                // set the Filtered result to return
//                results.count = FilteredArrList.size();
//                results.values = FilteredArrList;
//            }
//            return results;
//        }
//    };
}