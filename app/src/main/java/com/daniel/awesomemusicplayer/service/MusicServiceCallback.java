package com.daniel.awesomemusicplayer.service;


import com.daniel.awesomemusicplayer.tracks.RepeatMode;

/**
 * Interface for communication from MusicPlayerService back to MainActivity
 */
public interface MusicServiceCallback {

    /**
     * This method is called after a new track has been prepared and it has started playing
     * @param trackIndex index of the playing track in the playlist
     */
    void onTrackStarted(int trackIndex);

    /**
     * This method is called when the track is paused
     */
    void onTrackPaused();

    /**
     * This method is called when the track is resumed
     */
    void onTrackResumed();

    /**
     * This method is called when the track is stopped
     */
    void onTrackStopped();

    /**
     * This method is called when the repeat mode is changed
     * @param repeatMode the new active repeat mode
     */
    void onRepeatModeChanged(RepeatMode repeatMode);

    /**
     * This method is called when the shuffle mode is changed
     * @param shuffleEnabled true if shuffle mode is enabled, otherwise false
     */
    void onShuffleModeChanged(boolean shuffleEnabled);

    /**
     * This method is called when the user seeks to a new position in the track
     * @param trackTime the new position
     */
    void onPositionChanged(int trackTime);

}
