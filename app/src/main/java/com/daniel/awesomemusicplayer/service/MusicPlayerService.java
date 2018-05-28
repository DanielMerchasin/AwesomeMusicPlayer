package com.daniel.awesomemusicplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.daniel.awesomemusicplayer.App;
import com.daniel.awesomemusicplayer.MainActivity;
import com.daniel.awesomemusicplayer.R;
import com.daniel.awesomemusicplayer.tracks.RepeatMode;
import com.daniel.awesomemusicplayer.tracks.Track;
import com.daniel.awesomemusicplayer.util.Utils;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

/**
 * MusicPlayerService is in charge of playing the music using the android.media.MediaPlayer class,
 * handling track index movements and responding to user generated events.
 * After an event is handled, the service communicates back to MainActivity using the
 * MusicServiceCallback interface.
 * The rules of communication and binding are described in MainActivity.
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    /** Log tag */
    private static final String LOG_TAG         = "MusicPlayerService";

    /** Foreground notification ID */
    private static final int NOTIFICATION_ID    = 1234;

    /** MediaPlayer instance */
    private MediaPlayer mediaPlayer;

    /** Binder instance */
    private final IBinder musicServiceBinder = new MusicServiceBinder();

    /** Communication interface */
    private MusicServiceCallback callback;

    /** The track playlist */
    private ArrayList<Track> tracks;

    /** Index of the selected track */
    private int trackIndex;

    /** Full track title to be displayed */
    private String trackTitle;

    /** Shuffle mode */
    private boolean shuffle;

    /** Stack of track indexes saved when traversing the track list with shuffle mode enabled */
    private Stack<Integer> shuffleStack;

    /** Random number generator for shuffling */
    private Random random;

    /** Repeat mode */
    private RepeatMode repeatMode;

    /** Variable that determines if the media player is either playing or paused (not stopped) */
    private boolean playerReady;

    // --- Service lifecycle methods

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "In onCreate.");

        // Initialize default values
        trackIndex = 0;
        random = new Random();
        shuffle = false;
        repeatMode = RepeatMode.NONE;
        shuffleStack = new Stack<>();
        playerReady = false;

        // Initialize media player
        mediaPlayer = initMediaPlayer();

        Log.d(LOG_TAG, "Service created.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicServiceBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "In onDestroy.");
        mediaPlayer.release();
    }

    // --- Helper methods

    /**
     * Instantiates and initializes the MediaPlayer, sets the listeners
     * @return the initialized MediaPlayer
     */
    private MediaPlayer initMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        mp.setOnPreparedListener(this);
        mp.setOnErrorListener(this);
        mp.setOnCompletionListener(this);
        return mp;
    }

    /**
     * Plays the selected track from the start
     */
    public void playTrack() {
        // Reset the player
        mediaPlayer.reset();

        // Get the track title and update it's state to "playing"
        Track track = tracks.get(trackIndex);
        trackTitle = track.getArtist() + " - " + track.getTitle();
        track.setPlaying(true);

        // Get the track URI
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.getId());

        // Set the data source
        try {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Error: Failed to load track: " + trackTitle, Toast.LENGTH_LONG).show();
            playNext();
            return;
        }

        // Prepare the track asynchronously
        mediaPlayer.prepareAsync();
    }

    /**
     * Toggles between play and pause
     */
    public void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            // If the track is playing - pause it
            pause();
        } else if (!playerReady) {
            // If the track is not playing and the media player is stopped,
            // reset and prepare a new track
            playTrack();
        } else {
            // If the track is not playing and the media player is ready, resume it
            mediaPlayer.start();

            // Update the state
            tracks.get(trackIndex).setPlaying(true);

            // Callback
            if (callback != null)
                callback.onTrackResumed();

            // Start "Playing" foreground notification
            notifyAndStartForeground("Now Playing...", trackTitle);
        }
    }

    /**
     * Pauses the track
     */
    public void pause() {
        // Pause the track and update the state
        mediaPlayer.pause();
        tracks.get(trackIndex).setPlaying(false);

        // Callback
        if (callback != null)
            callback.onTrackPaused();

        // Start "Paused" foreground notification
        notifyAndStartForeground("Paused", trackTitle);
    }

    /**
     * Stops the media player
     */
    public void stop() {
        // Update the track state
        tracks.get(trackIndex).setPlaying(false);

        // Stop the media player
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();

        playerReady = false;

        // Callback
        if (callback != null)
            callback.onTrackStopped();

        // Stop the foreground notification
        stopForeground(true);
    }

    /**
     * Calculates the next track to select, while considering shuffle mode,
     * shuffle stack and repeat mode.
     */
    public void playNext() {
        // Deselect the last track
        Track lastTrack = tracks.get(trackIndex);
        lastTrack.setPlaying(false);
        lastTrack.setSelected(false);

        if (shuffle) {
            // Save last song in stack
            shuffleStack.push(trackIndex);

            // Find new track
            int newPosition;
            do {
                newPosition = random.nextInt(tracks.size());
            } while (trackIndex == newPosition);
            trackIndex = newPosition;
        } else {
            shuffleStack.clear();
            trackIndex++;
            if (trackIndex >= tracks.size()) {
                if (repeatMode == RepeatMode.REPEAT_ALL) {
                    trackIndex = 0;
                } else {
                    trackIndex--;
                    stop();
                    return;
                }
            }
        }

        // The new index is selected - play the track
        playTrack();
    }

    /**
     * Calculates the previous track to select, while considering shuffle mode and shuffle stack.
     */
    public void playPrevious() {
        // Deselect the last track
        Track lastTrack = tracks.get(trackIndex);
        lastTrack.setPlaying(false);
        lastTrack.setSelected(false);

        if (shuffle && !shuffleStack.isEmpty()) {
            trackIndex = shuffleStack.pop();
        } else {
            trackIndex--;
            if (trackIndex < 0)
                trackIndex = tracks.size() - 1;
        }

        // The new index is selected - play the track
        playTrack();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public boolean isReady() { return playerReady; }

    public ArrayList<Track> getTracks() { return tracks; }

    /**
     * Seeks to the requested position in the track
     * @param position the requested position
     */
    public void seekTo(int position) {
        if (playerReady) {
            int trackTime = (int) (mediaPlayer.getDuration() / 100.0f * position);
            mediaPlayer.seekTo(trackTime);
            if (callback != null)
                callback.onPositionChanged(trackTime / 1000);
        }
    }

    public int getPosition() {
        return (int) (mediaPlayer.getCurrentPosition() / 1000.0f);
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public boolean isShuffled() {
        return shuffle;
    }

    /**
     * Toggles shuffle mode
     */
    public void toggleShuffle() {
        shuffle = !shuffle;
        if (callback != null)
            callback.onShuffleModeChanged(shuffle);
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    /**
     * Toggles repeat mode
     */
    public void toggleRepeatMode() {
        repeatMode = RepeatMode.values()[(repeatMode.ordinal() + 1) % RepeatMode.values().length];
        if (callback != null)
            callback.onRepeatModeChanged(repeatMode);
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    public void setTracks(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    /**
     * Convenience method, select a new index and play the track
     * @param trackPosition the new index
     */
    public void selectTrack(int trackPosition) {
        // Track selected manually, clear the shuffle stack
        shuffleStack.clear();
        trackIndex = trackPosition;
        Log.d(LOG_TAG, "Performing selection: " + trackIndex);
        playTrack();
    }

    public void setTrackIndex(int trackIndex) {
        this.trackIndex = trackIndex;
    }

    public int getSelectedTrackIndex() {
        return trackIndex;
    }

    public String getTrackTitle() {
        return trackTitle;
    }

    public void setCallback(MusicServiceCallback callback) {
        this.callback = callback;
    }

    /**
     * Creates a notification and starts foreground
     * @param title notification title
     * @param message notification content text and ticker
     */
    private void notifyAndStartForeground(String title, String message) {
        // Build the notification and start the foreground service
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, App.SERVICE_CHANNEL_ID)
                .setSmallIcon(R.drawable.amp_icon_alpha)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    // --- MediaPlayer interfaces methods

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() > 0) {
            mp.reset();

            if (repeatMode == RepeatMode.REPEAT_TRACK) {
                playTrack();
            } else {
                playNext();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "An error has occurred.", Toast.LENGTH_LONG).show();
        mp.reset();

        if (callback != null)
            callback.onTrackPaused();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playerReady = true;
        mp.start();

        if (callback != null)
            callback.onTrackStarted(trackIndex);

        notifyAndStartForeground("Now Playing...", trackTitle);
    }

    /**
     * The service binder
     */
    public class MusicServiceBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

}
