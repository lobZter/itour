package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.MyViewPager;
import nctu.cs.cgv.itour.object.Checkin;

import static nctu.cs.cgv.itour.Utility.dpToPx;

public class PersonalFragment extends Fragment {

    private static final String TAG = "PersonalFragment";
    private ActionBar actionBar;
    private MyViewPager viewPager;
    private List<Fragment> fragmentList;
    private PostedCheckinFragment postedCheckinFragment;
    private SavedCheckinFragment savedCheckinFragment;

    public static PersonalFragment newInstance() {
        return new PersonalFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        actionBar = ((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar();

        postedCheckinFragment = PostedCheckinFragment.newInstance();
        savedCheckinFragment = SavedCheckinFragment.newInstance();
        fragmentList = new ArrayList<>();
        fragmentList.add(postedCheckinFragment);
        fragmentList.add(savedCheckinFragment);

        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {

            String tabTitles[] = new String[]{"個人發文", "收藏打卡"};

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

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (getUserVisibleHint()) {
            if (actionBar != null) {
                actionBar.setElevation(0);
                actionBar.setSubtitle(getString(R.string.subtitle_personal));
            }
            postedCheckinFragment.refresh();
            savedCheckinFragment.refresh();
        } else {
            if (actionBar != null) {
                actionBar.setElevation(dpToPx(getContext(), 4));
            }
        }
    }

    public void notifySavedCheckinChanged() {
        savedCheckinFragment.checkinItemAdapter.notifyDataSetChanged();
    }

    public void notifyPostedCheckinChanged() {
        postedCheckinFragment.checkinItemAdapter.notifyDataSetChanged();
    }
}
