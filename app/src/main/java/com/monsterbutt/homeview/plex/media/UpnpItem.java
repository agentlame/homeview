package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.widget.ListRow;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.item.*;

import java.util.List;

public class UpnpItem  extends PlexVideoItem implements Parcelable {

    final String mTitle;
    final String mKey;
    String mPosterURL = "";
    String mFilePath = "";
    long mDuration = 0;
    boolean mIsMovie;

    public UpnpItem(Item item) {

        super();

        mIsMovie = item instanceof org.fourthline.cling.support.model.item.Movie;
        mTitle = item.getTitle();
        mKey = item.getRefID();
        List<DIDLObject.Property> properties = item.getProperties();
        if (properties != null && !properties.isEmpty()) {

            for (DIDLObject.Property property : properties) {

                if (property.getDescriptorName().equals("albumArtURI")) {
                    mPosterURL = property.getValue().toString();

                    int scale = mPosterURL.indexOf("?scale=");
                    if (-1 < scale)
                        mPosterURL = mPosterURL.substring(0, scale) + "scale=480x480";
                }
            }
        }

        List<Res> resources = item.getResources();
        if (resources != null && !resources.isEmpty()) {

            for (Res resource : resources) {
                if (resource.getProtocolInfo().getContentFormat().startsWith("video/"))
                    mFilePath = resource.getValue();
                if (mDuration == 0 && !TextUtils.isEmpty(resource.getDuration())) {

                    String[] times = resource.getDuration().split(":");
                    if (times.length > 2) {
                        // hh:mm::ss.mmm
                        String milli = "0";
                        int dec = times[2].indexOf(".");
                        if (dec > -1) {
                            milli = times[2].substring(dec+1);
                            times[2] = times[2].substring(0, dec);
                        }
                        mDuration = Long.valueOf(milli) + (1000 * Long.valueOf(times[2]))
                                + (60 * 1000 * Long.valueOf(times[1])) + (60 * 1000 * Long.valueOf(times[0]));
                    }
                }
            }
        }
    }

    protected UpnpItem(Parcel in) {
        super();
        mTitle = in.readString();
        mKey = in.readString();
        mPosterURL = in.readString();
        mFilePath = in.readString();
        mDuration = in.readLong();
        mIsMovie = in.readInt() > 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mKey);
        dest.writeString(mPosterURL);
        dest.writeString(mFilePath);
        dest.writeLong(mDuration);
        dest.writeInt(mIsMovie ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UpnpItem> CREATOR = new Creator<UpnpItem>() {
        @Override
        public UpnpItem createFromParcel(Parcel in) {
            return new UpnpItem(in);
        }

        @Override
        public UpnpItem[] newArray(int size) {
            return new UpnpItem[size];
        }
    };

    @Override
    public String getKey() {
        return mKey;
    }

    @Override
    public String getPlaybackTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getPlaybackSubtitle(Context context) {
        return getCardContent(context);
    }

    @Override
    public String getCardImageURL() {
        return mPosterURL;
    }

    @Override
    public String getPlaybackImageURL() {
        return getCardImageURL();
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getCardContent(Context context) {
        return "";
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getWideCardContent(Context context) {
        return getPlaybackSubtitle(context);
    }

    @Override
    public String getSectionId() {
        return "0";
    }

    @Override
    public int getUnwatchedCount() {
        return 0;
    }

    @Override
    public WatchedState getWatchedState() {

        return WatchedState.Watched;
    }

    @Override
    public long getDurationMs() {
        return mDuration;
    }

    @Override
    public long getViewedOffset() {
        return 0;
    }

    @Override
    public int getViewedProgress() {
        return 0;
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        if (!mIsMovie) {

            Toast.makeText(fragment.getActivity(), fragment.getString(R.string.upnpbaditem), Toast.LENGTH_LONG).show();
            return false;
        }

        if (TextUtils.isEmpty(mFilePath)) {
            Toast.makeText(fragment.getActivity(), fragment.getString(R.string.upnpbadpath), Toast.LENGTH_LONG).show();
            return false;
        }

        Intent intent = new Intent(fragment.getActivity(), PlaybackActivity.class);
        intent.putExtra(PlaybackActivity.VIDEO, this);
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    PlaybackActivity.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);
        return true;
    }

    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return onClicked(fragment, extras, transitionView);
    }

    @Override
    public boolean shouldDiscoverQueue() {
        return false;
    }

    @Override
    public boolean shouldUpdateStatusOnPlayback() {
        return false;
    }

    @Override
    public MediaTrackSelector fillTrackSelector(Context context, String baseLanguageCode, MediaCodecCapabilities capabilities) {

        return null;
    }

    @Override
    public ListRow getCodecsRow(Context context, PlexServer server, MediaTrackSelector selector) {
        return null;
    }

    @Override
    public PlexItemRow getChildren(Context context, PlexServer server, CardPresenter.CardPresenterLongClickListener listener) {
        return null;
    }

    @Override
    public String getVideoPath(PlexServer server) {
        return mFilePath;
    }

    @Override
    public String getStudio() {
        return "";
    }

    @Override
    public long getRatingKey() {
        return 0;
    }

    @Override
    public boolean selectedHasMissingData() {
        return false;
    }

    @Override
    public boolean hasSourceStats() {
        return false;
    }
}


