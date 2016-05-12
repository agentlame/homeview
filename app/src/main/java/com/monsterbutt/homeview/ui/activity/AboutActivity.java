package com.monsterbutt.homeview.ui.activity;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AboutActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {

            setContentView(R.layout.activity_about);
            ListView listView = (ListView) findViewById(android.R.id.list);
            View headerView = getLayoutInflater().inflate(R.layout.lb_list_header, listView, false);
            getListView().addHeaderView(headerView, null, false);
            setListAdapter(new AboutItemArrayAdapter(this, parseAbout()));
        }
    }


    private class AboutItem {

        public final static String TAG = "Item";

        private String title;
        final private String blurb;
        final private String path;
         private String text;

        public AboutItem(XmlResourceParser xml) {

            title = xml.getAttributeValue(null, "title");
            blurb = xml.getAttributeValue(null, "blurb");
            path = xml.getAttributeValue(null, "path");
            try {
                xml.next();
                text = xml.getText();
            }
            catch (XmlPullParserException|IOException e) {
                e.printStackTrace();
            }
        }

        public String toString() {

            String ret = title + "\n";
            if (!TextUtils.isEmpty(path))
                ret += path + "\n";
            if (!TextUtils.isEmpty(blurb))
                ret += "\n" + blurb + "\n";
            if (!TextUtils.isEmpty(text))
                ret += "\n" + text + "\n";

            return ret;
        }
    }

    private List<AboutItem> parseAbout() {

        List<AboutItem> items = new ArrayList<>();
        Resources res = getResources();
        XmlResourceParser xml = res.getXml(R.xml.about);
        try {
            xml.next();
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                /*if(eventType == XmlPullParser.START_DOCUMENT);
                else*/
                if(eventType == XmlPullParser.START_TAG) {

                    switch(xml.getName()) {
                        case AboutItem.TAG:
                            AboutItem item = new AboutItem(xml);
                            items.add(item);
                            break;
                        default:
                            break;
                    }
                }
                eventType = xml.next();
            }
        }
        catch (IOException|XmlPullParserException e) {
            e.printStackTrace();
        }

        try {
            if (!items.isEmpty() && items.get(0).title.equals("Homeview"))
                items.get(0).title += String.format(" (%s)", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception e) {}

        return items;
    }

    public class AboutItemArrayAdapter extends ArrayAdapter<AboutItem> {

        private final Context context;
        private final List<AboutItem> values;

        public AboutItemArrayAdapter(Context context, List<AboutItem> values) {
            super(context, R.layout.lb_aboutitem, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View row, ViewGroup parent) {

            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView =inflater.inflate(R.layout.lb_aboutitem, parent, false);
            }
            final AboutItem item = values.get(position);
            setTextValue(rowView, R.id.title, item.title);
            setTextValue(rowView, R.id.blurb, item.blurb);
            setTextValue(rowView, R.id.path,  item.path);
            setTextValue(rowView, R.id.text,  item.text);

            return rowView;
        }

        private void setTextValue(View rowView, int id, String text) {

            TextView view = (TextView) rowView.findViewById(id);
            view.setText(text);
        }
    }
}
