package com.monsterbutt.homeview.player;


import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.player.track.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;

public class HomeViewExoPlayerAdapter extends ExoPlayerAdapter {

  public static final String TAG = "HV_HVExoPlayerAdapter";

  private TrackSelector mTrackSelector = null;
  private MediaTrackSelector mTracks = null;
  private TrackGroupArray lastSeenTrackGroupArray = null;
  private boolean mUseDefaultTracks = true;
  private final SeekOccuredNotifier mSeekOccuredNotifier;

  public static HomeViewExoPlayerAdapter getAdapter(Context context, SubtitleView view,
                                                    TrackSelector trackSelector,
                                                    SeekOccuredNotifier seekOccuredNotifier) {

    if (view != null) {
      view.setStyle(new CaptionStyleCompat(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT,
       CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null));
    }
    trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context));
    return new HomeViewExoPlayerAdapter(context, new HomeViewRendererFactory(context), trackSelector,
     view, seekOccuredNotifier);
  }

  private HomeViewRendererFactory mFactory;

  private HomeViewExoPlayerAdapter(Context context, HomeViewRendererFactory factory,
                                   TrackSelector trackSelector, SubtitleView view,
                                   SeekOccuredNotifier seekOccuredNotifier) {
    super(context, factory, trackSelector, view);
    mSeekOccuredNotifier = seekOccuredNotifier;
    mFactory = factory;
    mTrackSelector = trackSelector;
  }

  public synchronized void setDataSource(PlexVideoItem video, MediaTrackSelector tracks,
                                  boolean useDefaultTracks, PlexServer server) {
    mFactory.mDeviceAudioRenderer.prepareVideo(video);
    mTracks = tracks;
    mUseDefaultTracks = useDefaultTracks;
    super.setDataSource(Uri.parse(video.getVideoPath(server)));
  }

  public synchronized void checkInitialTrack(int streamId) {
    if (mTracks != null) {
      Stream stream = mTracks.getSelectedTrack(streamId);
      if (stream != null)
        mTracks.setSelectedTrack(mTrackSelector, new Stream.StreamChoice(getContext(), true, stream));
      else
        mTracks.disableTrackType(mTrackSelector, streamId);
    }
  }

  @Override
  public MediaSource onCreateMediaSource(Uri uri) {
    return new ExtractorMediaSource(uri,
     new DataSourceFactory(getContext()),
     new DefaultExtractorsFactory(),
     null,
     null);
  }

  private class DataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final String userAgent;

    DataSourceFactory(Context context) {
      this.context = context;
      this.userAgent = Util.getUserAgent(context, "HomeView");
    }

    @Override
    public DefaultDataSource createDataSource() {
      return new DefaultDataSource(context, null, userAgent, true);
    }
  }

  private static class HomeViewRendererFactory extends DefaultRenderersFactory {

    DeviceAudioTrackRenderer mDeviceAudioRenderer;

    HomeViewRendererFactory(Context context) {
      super(context, null, EXTENSION_RENDERER_MODE_ON);
    }

    @Override
    protected void buildAudioRenderers(Context context,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       AudioProcessor[] audioProcessors, Handler eventHandler,
                                       AudioRendererEventListener eventListener, @ExtensionRendererMode int extensionRendererMode,
                                       ArrayList<Renderer> out) {

      AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(context);
      boolean enableFloatOutput = audioCapabilities != null && audioCapabilities.supportsEncoding(C.ENCODING_PCM_FLOAT);

      mDeviceAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
       drmSessionManager, true, eventHandler, eventListener,
       MediaCodecCapabilities.getInstance(context), audioCapabilities, enableFloatOutput, audioProcessors);
      out.add(mDeviceAudioRenderer);
      if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
        return;
      }
      int extensionRendererIndex = out.size();
      if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
        extensionRendererIndex--;
      }
      try {
        Class<?> clazz =
         Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer");
        Constructor<?> constructor = clazz.getConstructor(Handler.class,
         AudioRendererEventListener.class, AudioSink.class, boolean.class);
        Renderer renderer = (Renderer) constructor.newInstance(eventHandler, eventListener,
         new DefaultAudioSink(null, audioProcessors), enableFloatOutput);
        out.add(extensionRendererIndex, renderer);
        Log.i(TAG, "Loaded FfmpegAudioRenderer.");
      } catch (ClassNotFoundException e) {
        // Expected if the app was built without the extension.
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    if (!mUseDefaultTracks) {
      mUseDefaultTracks = true;
      checkInitialTrack(Stream.Audio_Stream);
      checkInitialTrack(Stream.Subtitle_Stream);
    }

    if (trackGroups != lastSeenTrackGroupArray) {
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
         == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          getCallback().onError(this, TYPE_SOURCE, getContext().getString(R.string.error_unsupported_video));
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
         == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          getCallback().onError(this, TYPE_SOURCE, getContext().getString(R.string.error_unsupported_audio));
        }
      }
      lastSeenTrackGroupArray = trackGroups;
    }
  }

  @Override
  public void onPositionDiscontinuity(int reason) {
    mSeekOccuredNotifier.seekOccured(reason);
  }

}
