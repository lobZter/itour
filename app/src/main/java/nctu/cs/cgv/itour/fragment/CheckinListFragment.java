package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.custom.CheckinItemAdapter;
import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.activity.MainActivity;
import nctu.cs.cgv.itour.object.Checkin;

public class CheckinListFragment extends Fragment {

    private static final String TAG = "CheckinListFragment";

    private CheckinItemAdapter checkinItemAdapter;

    public static CheckinListFragment newInstance() {
        return new CheckinListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkinItemAdapter = new CheckinItemAdapter(getActivity(), new ArrayList<Checkin>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_view_no_divider, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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
}
