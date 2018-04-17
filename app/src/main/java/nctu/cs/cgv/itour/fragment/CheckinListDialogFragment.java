package nctu.cs.cgv.itour.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.custom.GridPhotoAdapter;
import nctu.cs.cgv.itour.object.Checkin;
import nctu.cs.cgv.itour.object.CheckinNode;

@SuppressLint("ValidFragment")
public class CheckinListDialogFragment extends DialogFragment {

    private static final String TAG = "CheckinDialogFragment";

    private ArrayList<Checkin> checkinList;

    public static CheckinListDialogFragment newInstance(CheckinNode checkinNode) {
        CheckinListDialogFragment checkinListDialogFragment = new CheckinListDialogFragment(checkinNode);
        return checkinListDialogFragment;
    }

    @SuppressLint("ValidFragment")
    public CheckinListDialogFragment(CheckinNode checkinNode) {
        this.checkinList = new ArrayList<>(checkinNode.checkinList);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_list_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridPhotoAdapter gridPhotoAdapter = new GridPhotoAdapter(getContext(), checkinList);

        GridView gridView = view.findViewById(R.id.grid_view);
//        gridView.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, 3));
        gridView.setNumColumns(3);
        gridView.setAdapter(gridPhotoAdapter);
        gridView.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                String key = checkinList.get(position).key;
                String location = checkinList.get(position).location;
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                CheckinDialogFragment checkinDialogFragment = CheckinDialogFragment.newInstance(key);
                checkinDialogFragment.show(fragmentManager, "fragment_checkin_dialog");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // set dialog layout
        getDialog().getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }
}
