package com.monsterbutt.homeview;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;


public class RecommendationBuilder {

    String  mTitle = "";
    String  mDescription = "";
    String  mGroup = "";
    Bitmap  mLargeIcon = null;
    String  mBackgroundImage = "";
    int     mSmallIcon;
    int     mPriority;
    int     mProgress = 0;
    PendingIntent mIntent;

    Context mContext = null;

    public RecommendationBuilder setContext(Context context) {
        mContext = context;
        return  this;
    }

    public RecommendationBuilder setTitle(String title) {
        mTitle = title;
        return this;
    }

    public RecommendationBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    public RecommendationBuilder setLargeIcon(Bitmap largeIcon) {
        mLargeIcon = largeIcon;
        return this;
    }

    public RecommendationBuilder setBackground(String url) {
        mBackgroundImage = url;
        return this;
    }

    public RecommendationBuilder setSmallIcon(int smallIcon) {
        mSmallIcon = smallIcon;
        return this;
    }

    public RecommendationBuilder setProgress(int progress) {
        mProgress = progress;
        return this;
    }

    public RecommendationBuilder setPriority(int priority) {
        mPriority = priority;
        return this;
    }

    public RecommendationBuilder setIntent(PendingIntent intent) {
        mIntent = intent;
        return this;
    }

    public RecommendationBuilder setGroup(String group) {
        mGroup = group;
        return this;
    }

    public Notification build() throws IOException {

        Bundle extra = new Bundle();
        extra.putString(Notification.EXTRA_BACKGROUND_IMAGE_URI, Uri.parse(mBackgroundImage).toString());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setGroup(mGroup)
                .setContentTitle(mTitle)
                .setContentText(mDescription)
                .setPriority(mPriority)
                .setLocalOnly(true)
                .setOngoing(true)
                .setColor(mContext.getColor(R.color.BlueGrey_Primary))
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setLargeIcon(mLargeIcon)
                .setSmallIcon(mSmallIcon)
                .setContentIntent(mIntent)
                .setExtras(extra);

        if (mProgress > 0 && mProgress < 100)
            builder.setProgress(100, mProgress, false);

        return  new NotificationCompat.BigPictureStyle(builder).build();
    }
}