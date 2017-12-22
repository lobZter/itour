package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.SpotNode;

import static nctu.cs.cgv.itour.MyApplication.spotList;
import static nctu.cs.cgv.itour.Utility.actionLog;
import static nctu.cs.cgv.itour.Utility.gpsToImgPx;

public class SpotListFragment extends Fragment {

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
        return inflater.inflate(R.layout.fragment_list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ArrayList<String> list = new ArrayList<>();
        list.addAll(spotList.getSpotsName());

        final ArrayAdapter<String> spotArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, list);
        final ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(spotArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                SpotNode spotNode = spotList.nodeMap.get(spotArrayAdapter.getItem(position));
                actionLog("spotlist: " + spotNode.name);
                ((MainActivity) getActivity()).onLocateClick(spotNode.x, spotNode.y, "");
            }
        });
    }

}