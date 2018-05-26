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

public class MusicPlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    /** Log tag */
    private static final String LOG_TAG         = "MusicPlayerService";

    /**  */
    private static final int NOTIFICATION_ID    = 1234;

    private MediaPlayer mediaPlayer;
    private ArrayList<Track> tracks;
    private int trackIndex;
    private final IBinder musicServiceBinder = new MusicServiceBinder();
    private String trackTitle;
    private boolean shuffle;
    private Stack<Integer> shuffleStack;
    private RepeatMode repeatMode;
    private Random random;
    private MusicServiceCallback callback;
    private boolean playerReady;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "In onCreate.");

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

    private MediaPlayer initMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        mp.setOnPreparedListener(this);
        mp.setOnErrorListener(this);
        mp.setOnCompletionListener(this);
        mp.reset();
        return mp;
    }

    public void playTrack() {
        mediaPlayer.reset();

        Track track = tracks.get(trackIndex);
        trackTitle = track.getArtist() + " - " + track.getTitle();
        track.setPlaying(true);

        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.getId());

        try {
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Error: Failed to load track: " + trackTitle, Toast.LENGTH_LONG).show();
            playNext();
            return;
        }

        mediaPlayer.prepareAsync();
    }

    public void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            pause();
        } else if (!playerReady) {
            playTrack();
        } else {
            mediaPlayer.start();

            tracks.get(trackIndex).setPlaying(true);

            if (callback != null)
                callback.onTrackResumed();
            notifyAndStartForeground("Now Playing...", trackTitle);
        }
    }

    public void pause() {
        mediaPlayer.pause();
        tracks.get(trackIndex).setPlaying(false);
        if (callback != null)
            callback.onTrackPaused();
        notifyAndStartForeground("Paused", trackTitle);
    }

    public void stop() {
        tracks.get(trackIndex).setPlaying(false);
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();
        playerReady = false;
        if (callback != null)
            callback.onTrackStopped();
        stopForeground(true);
    }

    public void playNext() {
        tracks.get(trackIndex).setPlaying(false);

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
        playTrack();
    }

    public void playPrevious() {
        tracks.get(trackIndex).setPlaying(false);
        if (shuffle && !shuffleStack.isEmpty()) {
            trackIndex = shuffleStack.pop();
        } else {
            trackIndex--;
            if (trackIndex < 0)
                trackIndex = tracks.size() - 1;
        }
        playTrack();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public boolean isReady() { return playerReady; }

    public ArrayList<Track> getTracks() { return tracks; }

    public void seekTo(int position) {
        if (playerReady) {
            int trackTime = (int) (mediaPlayer.getDuration() / 100.0f * position);
            Log.d(LOG_TAG, "seekTo() called. position: " + position + ", track time: " + trackTime
                    + ", formatted millis: " + Utils.formatMillis(trackTime)
                    + ", formatted seconds: " + Utils.formatSeconds(trackTime));
            mediaPlayer.seekTo(trackTime);
            if (callback != null)
                callback.onPositionChanged(position, trackTime / 1000);
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

    public void toggleShuffle() {
        shuffle = !shuffle;
        if (callback != null)
            callback.onShuffleModeChanged(shuffle);
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

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

    public class MusicServiceBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
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

}
