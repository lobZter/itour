package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.MyViewPager;
import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.Utility.dpToPx;

/**
 * Created by lobst3rd on 2017/8/18.
 */

public class ListFragment extends Fragment {

    private static final String TAG = "ListFragment";
    private ActionBar actionBar;
    private MyViewPager viewPager;
    private TabLayout tabLayout;
    private List<Fragment> fragmentList;
    private String tabTitles[];

    public static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        tabTitles = new String[]{"Checkin", "Spot"};

        fragmentList = new ArrayList<>();
        fragmentList.add(CheckinListFragment.newInstance());
        fragmentList.add(SpotListFragment.newInstance());

        viewPager = (MyViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                // Generate title based on item position
                return tabTitles[position];
            }
        });
        viewPager.setPagingEnabled(false);
        viewPager.setOffscreenPageLimit(2);

        tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (actionBar != null) {
            if (getUserVisibleHint()) {
                actionBar.setElevation(0);
                actionBar.setSubtitle("List");
            } else {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.menu_filter, menu);
        inflater.inflate(R.menu.checkin_filter_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.btn_filter:
//                Log.d(TAG, "onOptionsItemSelected():" + isMyCheckins);
//                if (isMyCheckins) {
//                    checkinList.setVisibility(View.VISIBLE);
//                    myCheckinList.setVisibility(View.GONE);
//                } else {
//                    checkinList.setVisibility(View.GONE);
//                    myCheckinList.setVisibility(View.VISIBLE);
//                }
//                isMyCheckins = !isMyCheckins;
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}