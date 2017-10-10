package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.custom.PlanItem;
import nctu.cs.cgv.itour.custom.PlanItemAdapter;
import nctu.cs.cgv.itour.R;

public class PlanFragment extends Fragment {

    private ActionBar actionBar;

    public static PlanFragment newInstance() {
        PlanFragment fragment = new PlanFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_view, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ArrayList<PlanItem> planItems = new ArrayList<>();
        PlanItemAdapter planItemAdapter = new PlanItemAdapter(getContext(), planItems);
        ListView planList = (ListView) view.findViewById(R.id.list_view);
        planList.setAdapter(planItemAdapter);

        // testing data
        planItemAdapter.add(new PlanItem("工程三館", 30));
        planItemAdapter.add(new PlanItem("第二餐廳", 30));
        planItemAdapter.add(new PlanItem("浩然圖書館", 30));
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (actionBar != null) {
            if (getUserVisibleHint()) {
                actionBar.setSubtitle(getString(R.string.subtitle_plan));
            }
        }
    }
}