package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.PlanItem;
import nctu.cs.cgv.itour.PlanItemAdapter;
import nctu.cs.cgv.itour.R;

public class PlanFragment extends Fragment {

    public static PlanFragment newInstance() {
        PlanFragment fragment = new PlanFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ArrayList<PlanItem> planItems = new ArrayList<>();
        PlanItemAdapter planItemAdapter = new PlanItemAdapter(getContext(), planItems);
        ListView planList = (ListView) view.findViewById(R.id.plan_list);
        planList.setAdapter(planItemAdapter);

        // testing data
        planItemAdapter.add(new PlanItem("工程三館", 30));
        planItemAdapter.add(new PlanItem("第二餐廳", 30));
        planItemAdapter.add(new PlanItem("浩然圖書館", 30));
    }
}