package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.custom.PlanItem;
import nctu.cs.cgv.itour.custom.NewsItemAdapter;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.Notification;

import static nctu.cs.cgv.itour.Utility.dpToPx;

public class NewsFragment extends Fragment {

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

        // testing data
        newsItemAdapter.add(new Notification("", "", "TEST", "= =", "", "", "", 0));
        newsItemAdapter.add(new Notification("", "", "TEST", "= =", "", "", "", 0));
        newsItemAdapter.add(new Notification("", "", "TEST", "= =", "", "", "", 0));
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