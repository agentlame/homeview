package com.monsterbutt.homeview.ui.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AboutActivity extends Activity {


    private static final int CANCEL   = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {

            List<AboutItem> items = parseAbout();
            setContentView(R.layout.activity_about);
            TextView text = (TextView) findViewById(R.id.about_text);
            String str = "";
            for(AboutItem item : items)
                str += item.toString() + "\n\n";

            text.setText(str);
        }
    }


    private class AboutItem {

        public final static String TAG = "Item";

        final private String title;
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
            catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
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
                if(eventType == XmlPullParser.START_DOCUMENT);
                else if(eventType == XmlPullParser.START_TAG) {

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
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return items;
    }
}
