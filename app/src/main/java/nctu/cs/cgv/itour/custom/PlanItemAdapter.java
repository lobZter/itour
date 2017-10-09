package nctu.cs.cgv.itour.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import nctu.cs.cgv.itour.R;

/**
 * Created by lobZter on 2017/7/10.
 */

public class PlanItemAdapter extends ArrayAdapter<PlanItem> {

    public PlanItemAdapter(Context context, ArrayList<PlanItem> planItems) {
        super(context, 0, planItems);
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        PlanItem planItem = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_plan, parent, false);
        }

        TextView title = (TextView) convertView.findViewById(R.id.tv_title);
        title.setText(planItem != null ? planItem.title : null);

        return convertView;
    }
}