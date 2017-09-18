package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.MyViewPager;
import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.Utility.dpToPx;

public class PersonalFragment extends Fragment {

    private static final String TAG = "PersonalFragment";
    private ActionBar actionBar;
    private MyViewPager viewPager;
    private TabLayout tabLayout;
    private List<Fragment> fragmentList;
    private String tabTitles[];

    public static PersonalFragment newInstance() {
        PersonalFragment fragment = new PersonalFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        tabTitles = new String[]{"Posted", "Saved"};

        fragmentList = new ArrayList<>();
        fragmentList.add(PostedCheckinFragment.newInstance());
        fragmentList.add(SavedCheckinFragment.newInstance());

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
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (actionBar != null) {
            if (getUserVisibleHint()) {
                actionBar.setElevation(0);
                actionBar.setSubtitle("Personal");
            } else {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }
}
