package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Map;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;
import static nctu.cs.cgv.itour.activity.MainActivity.savedPostId;

public class SavedCheckinFragment extends Fragment {

    private static final String TAG = "SavedCheckinFragment";
    private CheckinItemAdapter checkinItemAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    public static SavedCheckinFragment newInstance() {
        return new SavedCheckinFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkinItemAdapter = new CheckinItemAdapter(getActivity(), new ArrayList<Checkin>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycle_view_swipe_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.gps_marker_color);

        RecyclerView recyclerView = view.findViewById(R.id.recycle_view);
        recyclerView.setAdapter(checkinItemAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }

    public void refresh() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkinItemAdapter.clear();
            for (final Map.Entry<String, Boolean> entry : savedPostId.entrySet()) {
                if (entry.getValue()) {
                    String postId = entry.getKey();
                    Checkin checkin = checkinMap.get(postId);
                    if (checkin != null) {
                        checkinItemAdapter.insert(checkin, 0);
                    } else {
                        // TODO delete this postId
                    }
                }
            }
        }
    }

    public void addSavedCheckin(Checkin checkin) {
        checkinItemAdapter.add(checkin);
    }
}
