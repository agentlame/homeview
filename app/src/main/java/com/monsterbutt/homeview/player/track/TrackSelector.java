package com.monsterbutt.homeview.player.track;

import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.monsterbutt.homeview.plex.media.Stream;

public class TrackSelector extends DefaultTrackSelector {

    public final static int TrackTypeOff = -1;

    private TrackGroupArray[] mTracks;
    private RendererCapabilities[] mRenderers;


    void setSelectionOverride(int type, Stream.StreamChoice choice) {


        if (choice == null || choice instanceof Stream.StreamChoiceDisable) {

            for (int rendererIndex = 0; rendererIndex < mRenderers.length; ++rendererIndex) {

                if (type == mRenderers[rendererIndex].getTrackType())
                    setRendererDisabled(rendererIndex, true);
            }
        }
        else {

            TrackGroupArray trackGroupArray = null;
            SelectionOverride override = null;
            Format trackFormat = null;
            String trackId = Integer.toString(Integer.parseInt(choice.stream.getIndex()) + 1);
            for (TrackGroupArray groupArray : mTracks) {
                for (int groupIndex = 0; groupIndex < groupArray.length; ++groupIndex) {

                    TrackGroup group = groupArray.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < group.length; ++trackIndex) {

                        Format format = group.getFormat(trackIndex);
                        if (format.id.equalsIgnoreCase(trackId)) {

                            trackGroupArray = groupArray;
                            trackFormat = format;
                            override = new SelectionOverride(new FixedTrackSelection.Factory(0, null), groupIndex, trackIndex);
                            break;
                        }
                    }
                }
            }
            if (trackGroupArray != null) {

                for (int rendererIndex = 0; rendererIndex < mRenderers.length; ++rendererIndex) {

                    if (type == mRenderers[rendererIndex].getTrackType()) {

                        try {
                            int mask = mRenderers[rendererIndex].supportsFormat(trackFormat);
                            if (RendererCapabilities.FORMAT_HANDLED == (RendererCapabilities.FORMAT_SUPPORT_MASK & mask)) {
                                if (getRendererDisabled(rendererIndex))
                                    setRendererDisabled(rendererIndex, false);
                                setSelectionOverride(rendererIndex, trackGroupArray, override);
                                break;
                            }
                        } catch (ExoPlaybackException e) {
                            Log.d("HomeViewTrackSelector", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities,
                                            TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports)
            throws ExoPlaybackException {

        mRenderers = rendererCapabilities;
        mTracks = rendererTrackGroupArrays;
        return super.selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);
    }
}
