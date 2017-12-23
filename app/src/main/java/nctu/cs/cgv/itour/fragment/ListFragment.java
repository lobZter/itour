package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.custom.MyViewPager;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.Utility.dpToPx;
import static nctu.cs.cgv.itour.activity.MainActivity.checkinMap;

/**
 * Created by lobst3rd on 2017/8/18.
 */

public class ListFragment extends Fragment {

    private static final String TAG = "ListFragment";
    private CheckinItemAdapter checkinItemAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ActionBar actionBar;

    public static ListFragment newInstance() {
        return new ListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        checkinItemAdapter = new CheckinItemAdapter(getActivity(), new ArrayList<Checkin>());
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

    public void addCheckin(final Checkin checkin) {
        checkinItemAdapter.add(checkin);
    }

    public void removeCheckin(final Checkin checkin) {

    }

    public void addCheckins() {
        checkinItemAdapter.addAll(((MainActivity) getActivity()).checkinMap.values());
    }

    public void refresh() {
        checkinItemAdapter.clear();
        for (final Checkin checkin : checkinMap.values()) {
            checkinItemAdapter.insert(checkin, 0);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getUserVisibleHint()) {
            if (actionBar != null) {
                actionBar.setElevation(0);
                actionBar.setSubtitle(getString(R.string.subtitle_list));
            }
            refresh();
        } else {
            if (actionBar != null) {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.checkin_filter_menu, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.popular:
//                Toast.makeText(getContext(), "not available", Toast.LENGTH_SHORT).show();
//                return true;
//            case R.id.time:
//                Toast.makeText(getContext(), "not available", Toast.LENGTH_SHORT).show();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
}