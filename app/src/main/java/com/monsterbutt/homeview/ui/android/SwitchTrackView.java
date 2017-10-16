package com.monsterbutt.homeview.ui.android;


import android.app.Activity;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.PlexItemRow;

public class SwitchTrackView extends SelectViewRow {

  final private int type;
  final private MediaTrackSelector tracks;
  final private TrackSelector trackSelector;

  public static SwitchTrackView getTracksView(Activity activity, int streamType,
                                              MediaTrackSelector tracks, TrackSelector trackSelector,
                                              PlexServer server, SelectView.SelectViewCaller caller) {

    SwitchTrackView selectView = new SwitchTrackView(activity, streamType, tracks, trackSelector);

    MediaTrackSelector.StreamChoiceArrayAdapter choices = tracks.getTracks(activity, streamType);
    int selected = 0;
      for (int i = 0; i < choices.getCount(); ++i) {
      Stream.StreamChoice choice = choices.getItem(i);
      if (choice != null && choice.isCurrentSelection())
        selected = i;
    }
    String header = activity.getString(streamType == Stream.Audio_Stream ?
     R.string.exo_controls_audio_description : R.string.exo_controls_subtitles_description);
    selectView.setRow(PlexItemRow.buildCodecItemsRow(activity, server, header,
                                            choices, streamType), selected, caller);
    return selectView;
  }

  private SwitchTrackView(Activity activity, int streamType,
                          MediaTrackSelector tracks, TrackSelector trackSelector) {

    super(activity);
    type = streamType;
    this.tracks = tracks;
    this.trackSelector = trackSelector;
  }

  protected  int getHeight() { return 600; }

  protected String getTag() { return type == Stream.Audio_Stream ? "audio" : "subtitle"; }

  protected void cardClicked(PosterCard card) {

    CodecCard codec = (CodecCard) card;
    Stream.StreamChoice stream = codec.getStream();
    if (stream == null)
      return;

    if (stream instanceof Stream.StreamChoiceDisable)
      tracks.disableTrackType(trackSelector, type);
    else
      tracks.setSelectedTrack(trackSelector,stream);
  }
}
