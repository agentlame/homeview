package com.monsterbutt.homeview.ui.grid;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.monsterbutt.homeview.R;

import java.util.List;

class SectionFilterArrayAdapter extends ArrayAdapter<SectionFilter> {

    private final List<SectionFilter> values;
    private final Context context;
    private SectionFilter selected;

    SectionFilterArrayAdapter(Context context, List<SectionFilter> values, SectionFilter selected) {
        super(context, R.layout.lb_aboutitem, values);
        this.context = context;
        this.values = values;
        this.selected = selected;
    }

    public SectionFilter selected() { return selected; }

    public SectionFilter selected(int selected) {
        this.selected = values.get(selected);
        return selected();
    }

    @NonNull
    @Override
    public View getView(int position, View row, @NonNull ViewGroup parent) {

        View rowView = row;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            rowView = inflater.inflate(R.layout.lb_filterchoice, parent, false);
        }
        final SectionFilter item = values.get(position);
        ImageView image = rowView.findViewById(R.id.directionImage);
        image.setVisibility(item instanceof SectionSort && !((SectionSort)item).isAscending ? View.VISIBLE: View.INVISIBLE);
        CheckBox name = rowView.findViewById(R.id.name);
        name.setText(item.name);
        name.setChecked(item == selected);
        return rowView;
    }
}
