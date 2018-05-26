package com.daniel.awesomemusicplayer.tracks;

import android.net.Uri;

import com.daniel.awesomemusicplayer.util.Utils;

import java.io.Serializable;

public class Track implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String title;
    private String artist;
    private long duration;
    private boolean selected;
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
        return "[" + id + "] " + title + " by " + artist + " (" + Utils.formatMillis(duration) + ")";
    }

}
