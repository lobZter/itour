package nctu.cs.cgv.itour.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Objects;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.custom.ItemClickSupport;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.MyApplication.mapTag;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

public class PostedCheckinFragment extends Fragment {

    private static final String TAG = "PostedCheckinFragment";
    private SwipeRefreshLayout swipeRefreshLayout;
    public CheckinItemAdapter checkinItemAdapter;

    public static PostedCheckinFragment newInstance() {
        return new PostedCheckinFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkinItemAdapter = new CheckinItemAdapter(getActivity(), new ArrayList<Checkin>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        Checkin checkin = checkinItemAdapter.getItem(position);
                        CheckinDialogFragment checkinDialogFragment = CheckinDialogFragment.newInstance(checkin.key);
                        checkinDialogFragment.show(Objects.requireNonNull(getFragmentManager()), "fragment_checkin_dialog");
                    }
                }
        );

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener(
                new ItemClickSupport.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                        final Checkin checkin = checkinItemAdapter.getItem(position);
                        new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                                .setTitle(R.string.dialog_delete_title)
                                .setMessage(R.string.dialog_delete_message)
                                .setPositiveButton(R.string.dialog_positive_btn, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FirebaseDatabase.getInstance().getReference().child("checkin").child(mapTag).child(checkin.key).removeValue();
                                        actionLog("remove checkin\n" + checkin.toMap().toString(), checkin.location, checkin.key);
                                        checkinMap.remove(checkin.key);
                                        checkinItemAdapter.remove(position);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_negative_btn, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();

                        return true;
                    }
                }
        );
    }

    public void refresh() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            checkinItemAdapter.clear();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            for (final Checkin checkin : checkinMap.values()) {
                if (uid.equals(checkin.uid)) {
                    checkinItemAdapter.insert(checkin, 0);
                }
            }
        }
    }
}