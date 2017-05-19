package com.monsterbutt.homeview.player;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;

import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_APPLICATION;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_AUDIO;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_TEXT;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_VIDEO;

public class TrackSelector extends DefaultTrackSelector {

    public final static int TrackTypeOff = -1;

    private TrackGroupArray[] mTracks;
    private RendererCapabilities[] mRenderers;
    private TrackSelection[] mSelections;


    public void setSelectionOverride(int type, String trackId) {


        if (trackId == null || trackId.isEmpty()) {

            for (int rendererIndex = 0; rendererIndex < mRenderers.length; ++rendererIndex) {

                if (type == mRenderers[rendererIndex].getTrackType())
                    setRendererDisabled(rendererIndex, true);
            }
        }
        else {

            TrackGroupArray trackGroupArray = null;
            SelectionOverride override = null;
            Format trackFormat = null;
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
                            if (0 < (RendererCapabilities.FORMAT_HANDLED &
                                    mRenderers[rendererIndex].supportsFormat(trackFormat))) {
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

    public int getSelectedTrackId(int type) {

        if (mSelections != null) {
            for (TrackSelection sel : mSelections) {
                if (sel != null && sel.length() > 0) {
                    Format format = sel.getSelectedFormat();
                    if ((type == C.TRACK_TYPE_AUDIO && format.sampleMimeType.startsWith(BASE_TYPE_AUDIO))
                     || (type == C.TRACK_TYPE_TEXT && (format.sampleMimeType.startsWith(BASE_TYPE_APPLICATION) || format.sampleMimeType.startsWith(BASE_TYPE_TEXT)))
                     || (type == C.TRACK_TYPE_VIDEO && format.sampleMimeType.startsWith(BASE_TYPE_VIDEO)))
                        return Integer.parseInt(format.id);
                }
            }
        }
        return -1;
    }

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities,
                                            TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports)
            throws ExoPlaybackException {

        mRenderers = rendererCapabilities;
        mTracks = rendererTrackGroupArrays;
        mSelections = super.selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);
        return mSelections;
    }
}
