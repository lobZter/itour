package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.PostedCheckinItemAdapter;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.adminUid;
import static nctu.cs.cgv.itour.activity.MainActivity.busCheckinMapForAdmin;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

public class PostedCheckinFragment extends Fragment {

    private static final String TAG = "PostedCheckinFragment";
    private PostedCheckinItemAdapter checkinItemAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static PostedCheckinFragment newInstance() {
        return new PostedCheckinFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkinItemAdapter = new PostedCheckinItemAdapter(getActivity(), new ArrayList<Checkin>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_view_swipe_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.gps_marker_color);

        ListView checkinList = (ListView) view.findViewById(R.id.list_view);
        checkinList.setAdapter(checkinItemAdapter);
    }

    public void refresh() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkinItemAdapter.clear();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid.equals(adminUid)) {
                for (final Checkin checkin : busCheckinMapForAdmin.values()) {
                    checkinItemAdapter.add(checkin);
                }
            } else {
                for (final Checkin checkin : checkinMap.values()) {
                    if (uid.equals(checkin.uid)) {
                        checkinItemAdapter.add(checkin);
                    }
                }
            }
        }
    }

    public void addPostedCheckin(Checkin checkin) {
        checkinItemAdapter.add(checkin);
    }
}