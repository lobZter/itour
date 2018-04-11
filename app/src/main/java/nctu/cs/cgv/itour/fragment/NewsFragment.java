package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;

import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.custom.ItemClickSupport;
import nctu.cs.cgv.itour.custom.PlanItem;
import nctu.cs.cgv.itour.custom.NewsItemAdapter;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.Notification;

import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.dpToPx;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;

public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";
    private ActionBar actionBar;
    private NewsItemAdapter newsItemAdapter;

    public static NewsFragment newInstance() {
        NewsFragment fragment = new NewsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        newsItemAdapter = new NewsItemAdapter(getContext(), new ArrayList<Notification>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        RecyclerView newsList = view.findViewById(R.id.recycle_view);
        newsList.setAdapter(newsItemAdapter);
        newsList.setLayoutManager(new LinearLayoutManager(getContext()));

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = databaseReference.child("notification").child(mapTag);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                nctu.cs.cgv.itour.object.Notification notification = dataSnapshot.getValue(nctu.cs.cgv.itour.object.Notification.class);
                if (notification == null) return;
                newsItemAdapter.add(notification);
                Log.d("NewsFragment", notification.title);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ItemClickSupport.addTo(newsList).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Notification notification = newsItemAdapter.getItem(position);
                        NewsFragment.this.notify();
                        float[] imgPx = gpsToImgPx(Float.valueOf(notification.lat), Float.valueOf(notification.lng));
                        ((MainActivity) getActivity()).onLocateClick(imgPx[0], imgPx[1], notification.postId);
                        actionLog("click news", notification.msg, notification.postId);
                    }
                }
        );
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getUserVisibleHint()) {
            if (actionBar != null) {
                actionBar.setElevation(0);
                actionBar.setSubtitle(getString(R.string.subtitle_plan));
            }
        } else {
            if (actionBar != null) {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }
}