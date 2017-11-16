package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcel;

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


    static final String NODE_NAME = "Preference";

    private SettingLaunch(Parcel in) {
        super(in);
        mResult = in.readInt();
        mContent = in.readString();
    }

    SettingLaunch(Context context, XmlResourceParser xml, int resultCode) {

        super(context, xml);
        mContent = "";
        mResult = resultCode;

        try {
            int eventType = xml.getEventType();

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

        } catch (XmlPullParserException | IOException e) {
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


    public static final Creator<SettingLaunch> CREATOR = new Creator<SettingLaunch>() {
        @Override
        public SettingLaunch createFromParcel(Parcel in) {
            return new SettingLaunch(in);
        }

        @Override
        public SettingLaunch[] newArray(int size) {
            return new SettingLaunch[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mResult);
        dest.writeString(mContent);
    }
}
