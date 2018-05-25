package com.daniel.awesomemusicplayer.service;


import com.daniel.awesomemusicplayer.tracks.RepeatMode;

public interface MusicServiceCallback {
    void onTrackStarted(int trackIndex);
    void onTrackPaused();
    void onTrackResumed();
    void onTrackStopped();
    void onRepeatModeChanged(RepeatMode repeatMode);
    void onShuffleModeChanged(boolean shuffleEnabled);
    void onPositionChanged(int position, int trackTime);
}
