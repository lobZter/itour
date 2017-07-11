package nctu.cs.cgv.itour.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import nctu.cs.cgv.itour.R;

/**
 * Created by lobZter on 2017/7/11.
 */

public class CheckinDialogFragment extends DialogFragment {

    private String title;
    private String description;
    private String filename;
    private String type;

    public CheckinDialogFragment() {
    }

    public static CheckinDialogFragment newInstance(String title, String description, String filename, String type) {
        CheckinDialogFragment checkinDialogFragment = new CheckinDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("description", description);
        args.putString("filename", filename);
        args.putString("type", type);
        checkinDialogFragment.setArguments(args);
        return checkinDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getArguments().getString("title", "nctu");
        description = getArguments().getString("description", "...");
        filename = getArguments().getString("filename", "");
        type = getArguments().getString("type", "audio");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checkin_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvTitle = (TextView) view.findViewById(R.id.tv_title);
        TextView tvDescription = (TextView) view.findViewById(R.id.tv_description);
        tvTitle.setText(title);
        tvDescription.setText(description);

        ImageButton closeBtn = (ImageButton) view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckinDialogFragment.this.dismiss();
            }
        });

    }

}
