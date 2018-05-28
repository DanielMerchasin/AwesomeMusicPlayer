package com.daniel.awesomemusicplayer.tracks;

import com.daniel.awesomemusicplayer.util.Utils;

import java.io.Serializable;

/**
 * Track - data model representing a track (song)
 * Contains dynamic boolean variables that represent the state of the track (selected/playing)
 */
public class Track {

    /** Track ID */
    private long id;

    /** Track title (name) */
    private String title;

    /** Name of the artist */
    private String artist;

    /** Duration of the track in milliseconds*/
    private long duration;

    /** Is the track selected? */
    private boolean selected;

    /** Is the track playing? */
    private boolean playing;

    /** Path to the album art image */
    private String albumArtURI;

    public Track() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getAlbumArtURI() {
        return albumArtURI;
    }

    public void setAlbumArtURI(String albumArtURI) {
        this.albumArtURI = albumArtURI;
    }

    public String getFullTitle() {
        return artist + " - " + title;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        return obj instanceof Track && this.id == ((Track) obj).id;
    }

    @Override
    public int hashCode() {
        return (int)(id ^ (id>>>32));
    }

    @Override
    public String toString() {
        return "[" + id + "] " + title + " by " + artist + " (" + Utils.formatMillis(duration) + ")"
                + (selected ? " SELECTED" : "") + (playing ? " PLAYING" : "");
    }

}
