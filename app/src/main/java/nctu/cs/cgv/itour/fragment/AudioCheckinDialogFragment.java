package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import nctu.cs.cgv.itour.R;

public class AudioCheckinDialogFragment extends DialogFragment {

    public AudioCheckinDialogFragment() {
    }

    public static AudioCheckinDialogFragment newInstance() {
        AudioCheckinDialogFragment audioCheckinDialogFragment = new AudioCheckinDialogFragment();
        return audioCheckinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
