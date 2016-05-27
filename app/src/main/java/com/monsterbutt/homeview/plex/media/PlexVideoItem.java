package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.data.VideoContract;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.presenters.CodecPresenter;
import com.monsterbutt.homeview.provider.MediaContentProvider;
import com.monsterbutt.homeview.provider.SearchImagesProvider;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.*;
import us.nineworlds.plex.rest.model.impl.Video;


public abstract class PlexVideoItem extends PlexLibraryItem implements Parcelable {

    static private final long MillisecondInMinutes = 1000*60;
    static public String RatingKeyToKey(String key) { return "/library/metadata/" + key; }

    public static long NEXTUP_DISABLED = -1;
    private static final long MIN_MINUTES_FOR_NEXTUP = 5 * 1000;

    protected PlexVideoItem(Parcel in) {

        mVideo = new Video(in);
        mShouldPlayFirst = in.readInt() == 1;
        mWatchedState = PlexVideoItem.getWatchedState(mVideo);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mVideo.writeToParcel(dest, flags);
        dest.writeInt( mShouldPlayFirst ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    static public PlexVideoItem getItem(Video video) {

        if (video.getType().equalsIgnoreCase(Episode.TYPE))
            return new Episode(video);
        else if (video.getType().equalsIgnoreCase(Movie.TYPE))
            return new Movie(video);
        else if (video.getType().equalsIgnoreCase(Clip.TYPE))
            return new Clip(video);
        return null;
    }

    protected final Video mVideo;
    private  boolean mShouldPlayFirst = false;
    private WatchedState mWatchedState;

    protected PlexVideoItem() {
        mVideo = null;
        mWatchedState = WatchedState.Watched;
    }

    protected PlexVideoItem(Video video) {

        mVideo = video;
        mWatchedState = PlexVideoItem.getWatchedState(mVideo);
    }

    @Override
    public void toggleWatched() {

        if (mWatchedState != WatchedState.Watched)
            mWatchedState = WatchedState.Watched;
        else
            mWatchedState = WatchedState.Unwatched;
        mVideo.setViewCount(getWatchedState() != WatchedState.Watched ? 1 : 0);
        mVideo.setViewOffset(0);
    }

    public static WatchedState getWatchedState(Video video) {

        WatchedState state;
        int viewCount = video.getViewCount();
        if (viewCount > 0) {

            state = (video.getViewOffset() > 0) ?
                    WatchedState.WatchedAndPartial :
                    WatchedState.Watched;
        }
        else {

            state = (video.getViewOffset() > 0) ?
                    WatchedState.PartialWatched :
                    WatchedState.Unwatched;
        }
        return state;
    }

    public boolean shouldPlaybackFirst() { return mShouldPlayFirst; }
    public void setShouldPlayFirst(boolean value) { mShouldPlayFirst = value; }

    @Override
    public String getKey() {
        return mVideo.getKey();
    }

    @Override
    public long getRatingKey() {
        return mVideo.getRatingKey();
    }

    @Override
    public String getSectionId() {
        return mVideo.getLibrarySectionID();
    }

    @Override
    public String getSectionTitle() {
        return mVideo.getLibrarySectionTitle();
    }

    @Override
    public String getType() {
        return mVideo.getType();
    }

    @Override
    public String getTitle() {
        return mVideo.getTitle();
    }

    @Override
    public String getSortTitle() {
        return (mVideo.getTitleSort() == null || mVideo.getTitleSort().isEmpty()
                        ? mVideo.getTitle() : mVideo.getTitleSort());
    }

    @Override
    public String getThumbnailKey() {
        return mVideo.getThumbNailImageKey();
    }

    @Override
    public String getArt() {
        return mVideo.getBackgroundImageKey();
    }

    @Override
    public long getAddedAt() {
        return mVideo.getTimeAdded();
    }

    @Override
    public long getUpdatedAt() {
        return mVideo.getTimeUpdated();
    }

    @Override
    public String getSummary() {
        return mVideo.getSummary();
    }

    @Override
    public String getCardTitle(Context context) {
        return mVideo.getTitle();
    }

    @Override
    public String getCardContent(Context context) {
        return "";
    }

    @Override
    public String getCardImageURL() {
        return mVideo.getThumbNailImageKey();
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getWideCardContent(Context context) {
        return getCardContent(context);
    }

    public String getWideCardImageURL() {
        return getCardImageURL();
    }

    public String getContentRating() {
        return mVideo.getContentRating();
    }

    @Override
    public String getRating() {
        return Double.toString(mVideo.getRating());
    }

    public String getYear() {
        return mVideo.getYear();
    }

    @Override
    public String getOriginalAvailableDate() {
        return mVideo.getOriginallyAvailableDate();
    }

    @Override
    public long getLastViewedAt() { return mVideo.getLastViewedAt(); }

    public long getDurationMs() {
        return mVideo.getDuration();
    }
    @Override
    public long getDuration() { return getDurationMs(); }

    public List<Role> getActors() {
        return mVideo.getActors();
    }

    public List<Director> getDirectors() {
        return mVideo.getDirectors();
    }

    public List<Writer> getWriters() {
        return mVideo.getWriters();
    }

    public List<Genre> getGenres() {
        return mVideo.getGenres();
    }

    public List<Country> getCountries() {
        return mVideo.getCountries();
    }

    public boolean hasSourceStats() {
        return true;
    }

    public List<Media> getMedia() {
        return mVideo.getMedias();
    }

    public WatchedState getWatchedState() {
        return mWatchedState;
    }

    public long getDurationInMin() {
       return mVideo.getDuration() / MillisecondInMinutes;
    }
    public String getStudio() { return mVideo.getStudio(); }

    public abstract String getPlaybackTitle(Context context);
    public abstract String getPlaybackSubtitle(Context context);
    public abstract String getPlaybackImageURL();

    @Override
    public String getBackgroundImageURL() {

        if (mVideo == null)
            return "";
        String art = mVideo.getBackgroundImageKey();
        if (art != null && !art.isEmpty())
            return art;
        return mVideo.getGrandparentArtKey();
    }

    @Override
    public long getViewedOffset() {
        return mVideo.getViewOffset();
    }

    @Override
    public void setStatus(WatchedStatusHandler.UpdateStatus status) {

        if (mVideo == null)
            return;
        mVideo.setViewOffset(status.viewedOffset);
        mWatchedState = status.state;
    }

    @Override
    public int getViewedProgress() {

        return ((int) (100 * ((double) mVideo.getViewOffset() / (double) mVideo.getDuration())));
    }

    @Override
    public boolean useItemBackgroundArt() { return true; }

    @Override
    public String getHeaderForChildren(Context context) {
        return context != null ? context.getString(R.string.chapters) : "Chapters";
    }

    @Override
    public List<PlexLibraryItem> getChildrenItems() {

        List<PlexLibraryItem> list = new ArrayList<>();
        if (mVideo.getChapters() != null) {
            for (us.nineworlds.plex.rest.model.impl.Chapter chapter : mVideo.getChapters())
                list.add(new Chapter(this, getKey(), chapter));
        }
        return list;
    }

    @Override
    public List<PlexLibraryItem> getExtraItems() {

        List<PlexLibraryItem> ret = new ArrayList<>();
        if (mVideo.getExtras() != null && !mVideo.getExtras().isEmpty()
                && mVideo.getExtras().get(0).getVideos() != null) {

            for (Video video : mVideo.getExtras().get(0).getVideos())
                ret.add(PlexVideoItem.getItem(video));
        }
        return ret;
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), DetailsActivity.class);
        intent.putExtra(DetailsActivity.ITEM, this);
        intent.putExtra(DetailsActivity.BACKGROUND, getBackgroundImageURL());
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);

        return true;
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), PlaybackActivity.class);
        intent.putExtra(PlaybackActivity.KEY, getKey());
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

    @Override
    public void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride, boolean isStartOverride) {

        Media media = mVideo.getMedias().get(0);
        boolean isMovie = this instanceof Movie;
        row.add(VideoContract.VideoEntry._ID, getRatingKey());
        row.add(VideoContract.VideoEntry.COLUMN_CATEGORY, isMovie ? Movie.TYPE : Episode.TYPE);

        String title = getTitle();
        String title2 = getSummary();
        String thumbKey = getThumbnailKey();
        String year = getYear();
        String key = getKey();
        String mimetype = "video/" + media.getContainer();
        if (!TextUtils.isEmpty(yearOverride))
            year = yearOverride;
        if (!TextUtils.isEmpty(keyOverride))
            key = keyOverride;
        if (!isMovie) {

            Episode ep = (Episode) this;
            if (!TextUtils.isEmpty(yearOverride)) {

                title2 = ep.getTitle();
                title = ep.getShowName();
            }
            else {
                thumbKey = ep.getShowThumbKey();
                if (thumbKey == null)
                    thumbKey = ep.getSeasonThumbKey();
            }
        }

        row.add(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, mimetype);
        row.add(VideoContract.VideoEntry.COLUMN_NAME, title);
        row.add(VideoContract.VideoEntry.COLUMN_DESC, title2);
        row.add(VideoContract.VideoEntry.COLUMN_CARD_IMG, SearchImagesProvider.CONTENT_URI + thumbKey);
        row.add(VideoContract.VideoEntry.COLUMN_SUBTITLE, getCardContent(context));
        row.add(VideoContract.VideoEntry.COLUMN_STUDIO, getStudio());
        row.add(VideoContract.VideoEntry.COLUMN_VIDEO_HEIGHT, media.getHeight());
        row.add(VideoContract.VideoEntry.COLUMN_VIDEO_WIDTH, media.getWidth());
        row.add(VideoContract.VideoEntry.COLUMN_VIDEO_URL, getKey());
        row.add(VideoContract.VideoEntry.COLUMN_IS_LIVE, "false");
        String audio;
        switch (media.getAudioChannels()) {
            case "6":
                audio = "5.1";
                break;
            case "7":
                audio = "6.1";
                break;
            case "8":
                audio = "7.1";
                break;
            case "3":
                audio = "2.1";
                break;
            default:
                audio = Integer.valueOf(media.getAudioChannels()) + ".0";
                break;
        }
        row.add(VideoContract.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG, audio);
        row.add(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, year);
        row.add(VideoContract.VideoEntry.COLUMN_DURATION, getDurationMs());
        row.add(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, SearchImagesProvider.CONTENT_URI + getBackgroundImageURL());
        row.add(VideoContract.VideoEntry.COLUMN_SHOULDSTART, isStartOverride ? 1 : 0);
        row.add(VideoContract.VideoEntry.COLUMN_KEY, getKey());
        row.add(VideoContract.VideoEntry.COLUMN_SERVERPATH, getMedia().get(0).getVideoPart().get(0).getKey());
        row.add(VideoContract.VideoEntry.COLUMN_FILEPATH, getMedia().get(0).getVideoPart().get(0).getFilename());
        row.add(VideoContract.VideoEntry.COLUMN_WATCHEDOFFSET, mVideo.getViewOffset());
        row.add(VideoContract.VideoEntry.COLUMN_WATCHED, getWatchedState() == WatchedState.Watched ? 1 : 0);
        row.add(VideoContract.VideoEntry.COLUMN_FRAMERATE, getMedia().get(0).getVideoFrameRate());

        if (!TextUtils.isEmpty(keyOverride))
            row.add(VideoContract.VideoEntry.COLUMN_DATA_ID, MediaContentProvider.ID_DETAIL);
        else
            row.add(VideoContract.VideoEntry.COLUMN_DATA_ID, MediaContentProvider.ID_PLAYBACK);
        row.add(VideoContract.VideoEntry.COLUMN_EXTRA, key);
    }

    public MediaTrackSelector fillTrackSelector(Context context, String baseLanguageCode, MediaCodecCapabilities capabilities) {

        Part part = getMedia().get(0).getVideoPart().get(0);
        if (part.getStreams() != null)
            return new MediaTrackSelector(context, part.getStreams(), baseLanguageCode, capabilities);
        return null;
    }

    private boolean addTrackTypeToCodec(Context context, ArrayObjectAdapter adapter, int trackType, MediaTrackSelector selector) {

        Stream stream = selector.getSelectedTrack(trackType);
        if (stream != null)
            adapter.add(new CodecCard(context, stream, trackType, selector.getCount(trackType)));
        return (stream != null);
    }

    public ListRow getCodecsRow(Context context, PlexServer server, MediaTrackSelector selector) {

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CodecPresenter(server));
        adapter.add(new CodecCard(context, getMedia().get(0)));
        addTrackTypeToCodec(context, adapter, Stream.Video_Stream, selector);
        addTrackTypeToCodec(context, adapter, Stream.Audio_Stream, selector);
        addTrackTypeToCodec(context, adapter, Stream.Subtitle_Stream, selector);

        return new ListRow(null, adapter);
    }

    public boolean hasChapters() {

        return (mVideo != null && mVideo.getChapters() != null && !mVideo.getChapters().isEmpty());
    }

    public static long START_CHAPTER_THRESHOLD = 5000;
    public static long BAD_CHAPTER_START = -1;
    public long getPreviousChapterStart(long position) {

        long ret = BAD_CHAPTER_START;
        if (hasChapters()) {

            us.nineworlds.plex.rest.model.impl.Chapter lastChapter = null;
            for (us.nineworlds.plex.rest.model.impl.Chapter chapter : mVideo.getChapters()) {

                if (chapter.getStartTimeOffset() + START_CHAPTER_THRESHOLD > position) {

                    if (lastChapter != null)
                        ret = lastChapter.getStartTimeOffset();
                    break;
                }
                lastChapter = chapter;
            }

            if (ret == 0 && position < START_CHAPTER_THRESHOLD)
                ret = BAD_CHAPTER_START;
        }
        return ret;
    }
    public long getNextChapterStart(long position) {

        long ret = BAD_CHAPTER_START;
        if (hasChapters()) {

            for (us.nineworlds.plex.rest.model.impl.Chapter chapter : mVideo.getChapters()) {

                if (chapter.getStartTimeOffset() > position) {

                    ret = chapter.getStartTimeOffset();
                    break;
                }
            }
        }
        return ret;
    }

    public com.monsterbutt.homeview.model.Video toVideo(Context context) {

        return new com.monsterbutt.homeview.model.Video.VideoBuilder()
                .title(getPlaybackTitle(context))
                .description(getPlaybackSubtitle(context))
                .cardImageUrl(getPlaybackImageURL())
                .duration(getDurationMs())
                .studio(getStudio())
                .id(getRatingKey())
                .subtitle(getCardContent(context))
                .build();
    }

    public long getNextUpThresholdTrigger(Context context) {

        long threshold = Long.valueOf(SettingsManager.getInstance(context).
                getString(this instanceof Episode ? "preferences_playback_nextup_episode"
                        : "preferences_playback_nextup_movies").trim());
        long ret = NEXTUP_DISABLED;
        long duration = getDurationMs();

        if (duration > MIN_MINUTES_FOR_NEXTUP) {

            threshold *= 1000; // make milliseconds
            ret = duration - threshold;
            if (ret < 0)
                ret = NEXTUP_DISABLED;
        }
        return ret;
    }

    @Override
    public String getDetailTitle(Context context) { return getTitle(); }

    @Override
    public String getDetailSubtitle(Context context) { return ""; }

    @Override
    public String getDetailContent(Context context) { return ""; }

    @Override
    public String getDetailYear(Context context) { return getYear(); }

    @Override
    public String getDetailDuration(Context context) {
        long mins = getDurationInMin();
        long hr = mins / 60;
        String ret = String.format("%d %s", mins%60, context.getString(R.string.minutes_abbrev));
        if (hr > 0)
            ret = String.format("%d %s %s", hr, context.getString(R.string.hours_abbrev), ret);
        return ret;
    }

    @Override
    public String getDetailStudioPath(PlexServer server) { return server.makeServerURLForCodec("studio", getStudio()); }

    @Override
    public String getDetailRatingPath(PlexServer server) { return server.makeServerURLForCodec("contentRating", mVideo.getContentRating()); }

    @Override
    public String getDetailGenre(Context context) {

        String ret = "";
        if (mVideo != null && mVideo.getGenres() != null) {
            for (Genre genre : mVideo.getGenres()) {
                if (!TextUtils.isEmpty(ret))
                    ret += ", ";
                ret += genre.getTag();
            }
        }
        return ret;
    }

    @Override
    public List<Hub> getRelated() {
        return mVideo.getRelated() != null && !mVideo.getRelated().isEmpty() ?
                mVideo.getRelated().get(0).getHubs() : null;
    }

    public String getPathKey() {

        return getMedia().get(0).getVideoPart().get(0).getKey();
    }

    public boolean trackIsDtsHd(String trackId) {

        // plex track ids are 0 based, media trackids are 1 based
        if (TextUtils.isEmpty(trackId) || Integer.valueOf(trackId) <= 0)
            return false;
        int trackIndex = Integer.valueOf(trackId) - 1;

        boolean ret = false;
        Media media = getMedia() != null ? getMedia().get(0) : null;
        if (media != null && media.getVideoPart() != null && media.getVideoPart().get(0).getStreams() != null) {

            List<us.nineworlds.plex.rest.model.impl.Stream> streams = media.getVideoPart().get(0).getStreams();
            for(us.nineworlds.plex.rest.model.impl.Stream stream : streams) {

                if (stream.getStreamType() == Stream.Audio_Stream && trackIndex == Integer.valueOf(stream.getIndex())) {

                    ret = Stream.profileIsDtsHdVariant(stream.getProfile());
                    break;
                }
            }
        }
        return ret;
    }

    public boolean shouldDiscoverQueue() { return true; }
    public boolean shouldUpdateStatusOnPlayback() { return true; }

    public String getVideoPath(PlexServer server) {

        String serverURL = server.getServerURL();
        String url = getPathKey();
        String ret = "";
        if (!TextUtils.isEmpty(url) && serverURL != null) {
            ret = serverURL;
            if (url.startsWith("/") && ret.endsWith("/"))
                ret += url.substring(1);
            else
                ret += url;
        }
        return ret;
    }

    public boolean selectedHasMissingData() {
        return getMedia().get(0).getVideoPart().get(0).getStreams() == null
                || !hasChapters(); // force chapter check in here, even if they don't exist
    }
}
