package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;

import static nctu.cs.cgv.itour.MyApplication.spotList;

public class SpotListFragment extends Fragment {

    public SpotListFragment() {
        // Required empty public constructor
    }

    public static SpotListFragment newInstance() {
        SpotListFragment fragment = new SpotListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ArrayList<String> list = new ArrayList<>();
        list.addAll(spotList.getSpotsName());

        ArrayAdapter<String> spotArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, list);
        ListView spotList = (ListView) view.findViewById(R.id.list_view);
        spotList.setAdapter(spotArrayAdapter);
    }

}