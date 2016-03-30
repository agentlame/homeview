package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


public class SettingLaunch extends SettingValue {

    private static final String Node_Intent = "intent";
    //private static final String TargetPackage = "targetPackage";
    private static final String TargetClass = "targetClass";

    private final int mResult;
    private final String mContent;
    private  String mClassName = "";


    public static final String NODE_NAME = "Preference";

    public SettingLaunch(Context context, XmlResourceParser xml, int resultCode) {

        super(context, xml);
        mContent = "";
        mResult = resultCode;

        int eventType = 0;
        try {
            eventType = xml.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) ;
                else if (eventType == XmlPullParser.START_TAG) {

                    if (xml.getName().equals(Node_Intent)) {
                        mClassName = xml.getAttributeValue(namespace, TargetClass);
                        break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {

                    if (xml.getName().equals(Node_Intent))
                        mClassName = xml.getAttributeValue(namespace, TargetClass);
                    break;
                }
                eventType = xml.next();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SettingLaunch(String name, String detail, int drawableId, String className, int result) {

        super(name, drawableId);
        mContent = detail;
        this.mResult = result;
        this.mClassName = className;
    }

    public int result() { return mResult; }
    public String content() { return mContent; }
    public String className() { return  mClassName; }
    @Override
    public SettingValue reload(Context context) { return this; }
}
