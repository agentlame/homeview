package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import com.monsterbutt.Homeview;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.presenters.SettingCard;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SettingsManager {

    private static final SettingsManager mMgr = new SettingsManager();

    public class SettingsSection {

        public final String title;
        public final List<SettingValue> settings;

        SettingsSection(String title, List<SettingValue> list) {

            this.title = title;
            settings = list;
        }
    }

    public static SettingsManager getInstance() {
        return mMgr;
    }

    private List<SettingsSection> mSections = new ArrayList<>();
    private Map<String, SettingValue> mSettings = new HashMap<>();

    private static final String PreferenceScreen = "PreferenceScreen";
    private static final String PreferenceCategory = "PreferenceCategory";

    private String mTitle = "";

    private SettingsManager() {

        Context context = Homeview.getAppContext();
        if (context == null)
            return;
        Resources res = context.getResources();
        XmlResourceParser xml = res.getXml(R.xml.preferences);
        try {
            xml.next();
            int eventType = xml.getEventType();
            List<SettingValue> row = null;
            String header = "";
            int id = SettingCard.BASE_RESULT + 1;
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if(eventType == XmlPullParser.START_DOCUMENT);
                else if(eventType == XmlPullParser.START_TAG) {

                    String name = xml.getName();
                    switch(name) {
                        case PreferenceScreen:
                            mTitle = SettingValue.getStringAttribute(context, xml, SettingValue.Title);
                            break;
                        case PreferenceCategory:
                            header = SettingValue.getStringAttribute(context, xml, SettingValue.Title);
                            row = new ArrayList<>();
                            break;
                        case SettingLaunch.NODE_NAME: {

                            SettingValue sett = new SettingLaunch(context, xml, id++);
                            if (row != null)
                                row.add(sett);
                            mSettings.put(sett.key(), sett);
                            break;
                        }
                        case SettingBoolean.NODE_NAME: {

                            SettingValue sett = new SettingBoolean(context, xml);
                            if (row != null)
                                row.add(sett);
                            mSettings.put(sett.key(), sett);
                            break;
                        }
                        case SettingText.NODE_NAME: {

                            SettingValue sett = new SettingText(context, header, xml);
                            if (row != null)
                                row.add(sett);
                            mSettings.put(sett.key(), sett);
                            break;
                        }
                        case SettingArray.NODE_NAME: {

                            SettingValue sett = new SettingArray(context, header, xml);
                            if (row != null)
                                row.add(sett);
                            mSettings.put(sett.key(), sett);
                            break;
                        }
                        default:
                            break;
                    }
                }
                else if(eventType == XmlPullParser.END_TAG) {

                    if (xml.getName().equals(PreferenceCategory))
                        mSections.add(new SettingsSection(header, row));

                }
                //else if(eventType == XmlPullParser.TEXT)
                eventType = xml.next();
            }
        }
        catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public String title() { return mTitle; }

    public SettingValue getSetting(String key) { return mSettings.get(key); }

    public boolean getBoolean(String key) {
        SettingValue val = getSetting(key);
        return val instanceof SettingBoolean && ((SettingBoolean) val).value();
    }

    public String getString(String key) {

        SettingValue val = getSetting(key);
        if (val instanceof SettingText)
            return ((SettingText)val).value();
        else if (val instanceof SettingArray)
            return ((SettingArray)val).currentValue();
        return "";
    }


    public long getLong(String key) {
        SettingValue val = getSetting(key);
        if (val instanceof SettingText)
            return Long.valueOf(((SettingText)val).value());
        else if (val instanceof SettingArray)
            return Long.valueOf(((SettingArray)val).currentValue());
        return 0;
    }

    public List<SettingsSection> getSettingsLayout() { return mSections; }

    void reloadSetting(Context context, String key) {
        SettingValue setting = mSettings.get(key);
        if (setting != null)
            mSettings.put(setting.key(), setting.reload(context));
    }


}
