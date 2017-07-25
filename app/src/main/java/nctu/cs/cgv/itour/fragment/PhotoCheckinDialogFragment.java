package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nctu.cs.cgv.itour.R;

/**
 * Created by lobZter on 2017/7/11.
 */

public class PhotoCheckinDialogFragment extends DialogFragment {

    public PhotoCheckinDialogFragment() {
    }

    public static PhotoCheckinDialogFragment newInstance() {
        PhotoCheckinDialogFragment photoCheckinDialogFragment = new PhotoCheckinDialogFragment();
        return photoCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
