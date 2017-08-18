package nctu.cs.cgv.itour;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nctu.cs.cgv.itour.object.Checkin;

/**
 * Created by lobZter on 2017/8/18.
 */

public class CheckinItemAdapter extends ArrayAdapter<Checkin> {

    public CheckinItemAdapter(Context context, List<Checkin> checkins) {
        super(context, 0, checkins);
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Checkin checkin = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_checkin, parent, false);
        }

        TextView username = (TextView) convertView.findViewById(R.id.username);
        TextView location = (TextView) convertView.findViewById(R.id.location);
        TextView description = (TextView) convertView.findViewById(R.id.description);
        username.setText(checkin.username);
        location.setText(checkin.location);
        description.setText(checkin.description);

        return convertView;
    }
}
