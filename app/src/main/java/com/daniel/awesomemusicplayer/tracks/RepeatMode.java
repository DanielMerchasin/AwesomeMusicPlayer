package com.daniel.awesomemusicplayer.tracks;

/**
 * RepeatMode - used to determine the playback behaviour after a track is completed
 * NONE = The next track is played
 * REPEAT_TRACK = The same track is looped
 * REPEAT_ALL = The entire playlist is looped
 */
public enum RepeatMode {
    NONE,
    REPEAT_TRACK,
    REPEAT_ALL
}
