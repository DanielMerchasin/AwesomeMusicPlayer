package com.daniel.awesomemusicplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.daniel.awesomemusicplayer.service.MusicPlayerService;
import com.daniel.awesomemusicplayer.service.MusicServiceCallback;
import com.daniel.awesomemusicplayer.tracks.RepeatMode;
import com.daniel.awesomemusicplayer.tracks.Track;
import com.daniel.awesomemusicplayer.tracks.TrackAdapter;
import com.daniel.awesomemusicplayer.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * This is the app's Main Activity.
 * It has a list of all the tracks on the device and UI media player controls.
 * The activity starts the Music Player Service, which is in charge of playing the music.
 *
 * Interaction between the activity and the service:
 *
 * 1. onStart:
 *      The service is initialized and bound.
 *      TrackTimerThread is initialized and launched.
 *
 * 2. onServiceConnected:
 *      The necessary data is passed to the service.
 *      If the track list is already loaded on the service, use it in the activity
 *      If the track list hasn't been loaded - load it from the device storage
 *
 * 3. onResume:
 *      The UI is updating using data from the service.
 *
 * 4. onStop:
 *      TrackTimerThread is stopped.
 *      The service callback is removed.
 *      The service is unbound.
 *      If the music player is stopped - the service is stopped as well.
 */
@SuppressWarnings("unchecked")
public class MainActivity extends AppCompatActivity implements MusicServiceCallback {

    /** Log tag */
    private static final String LOG_TAG         = "MainActivity";

    /** SharedPreferences Key */
    public static final String PREFS_KEY        = "com.daniel.awesomemusicplayer";

    /** Key constants for shared preferences */
    private static final String KEY_TRACK_INDEX = "KEY_TRACK_INDEX";
    private static final String KEY_TRACK_TIME  = "KEY_TRACK_TIME";
    private static final String KEY_SHUFFLE_ON  = "KEY_SHUFFLE_ON";
    private static final String KEY_REPEAT_MODE = "KEY_REPEAT_MODE";

    /**
     * Permission request constant for reading external storage
     * (used when initializing the track list with content resolver)
     */
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    /** List of all tracks */
    private ArrayList<Track> tracks;

    /** ListView track adapter */
    private TrackAdapter trackAdapter;

    /** Music Player Service */
    private MusicPlayerService musicPlayerService;

    /** Music Player Service intent (for starting and stopping the service) */
    private Intent serviceIntent;

    /** Is the service bound to the activity? */
    private boolean serviceBound;

    /**
     * Is the service running?
     * A flag used to decide whether to stop the service in onStop
     */
    private boolean serviceRunning;

    /** Is shuffle mode enabled on the service? */
    private boolean shuffleEnabled;

    /** The repeat mode on the service */
    private RepeatMode repeatMode;

    /** UI components */
    private ListView lstTracks;
    private ImageView imgAlbum, btnPrevious, btnPlay,
            btnNext, btnStop, btnShuffle, btnRepeat;
    private TextView lblPosition, lblDuration, lblTrackName;
    private SeekBar skbrSlider;

    /** Shared Preferences */
    private SharedPreferences prefs;

    /** Index of the selected track */
    private int trackIndex;

    /**
     * Track position timer thread
     * (A thread updating the seekbar and the position (time) label)
     */
    private TrackTimerThread trackTimerThread;

    /** Track time (position) IN SECONDS */
    private int trackTime;

    /** Should the timer run? True when a track is playing, otherwise false */
    private boolean timerRunning;

    /** Handler used to update the UI from the TrackTimerThread */
    private final Handler handler = new Handler();


    // --- Activity lifecycle methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        lstTracks = findViewById(R.id.lstTracks);
        imgAlbum = findViewById(R.id.imgAlbum);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnStop = findViewById(R.id.btnStop);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        lblPosition = findViewById(R.id.lblPosition);
        lblDuration = findViewById(R.id.lblDuration);
        lblTrackName = findViewById(R.id.lblTrackName);
        skbrSlider = findViewById(R.id.skbrSlider);

        // Initialize data from shared preferences
        prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
        trackIndex = prefs.getInt(KEY_TRACK_INDEX, 0);
        trackTime = prefs.getInt(KEY_TRACK_TIME, 0);
        shuffleEnabled = prefs.getBoolean(KEY_SHUFFLE_ON, false);
        repeatMode = RepeatMode.values()[prefs.getInt(KEY_REPEAT_MODE, 0)];

        // -- Prepare listeners

        lstTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (!serviceBound || position == trackIndex)
                    return;

                // Select song on the service
                musicPlayerService.selectTrack(position);

                // Change play button to pause
                btnPlay.setImageDrawable(getDrawable(R.drawable.btn_pause));
                serviceRunning = true;
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Play button clicked, toggle play/pause on the service
                if (!serviceBound) return;
                musicPlayerService.togglePlayPause();
                serviceRunning = true;
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Stop button clicked, stop the playback on the service
                if (!serviceBound) return;
                musicPlayerService.stop();
                serviceRunning = false;
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Play the next track
                if (!serviceBound) return;
                musicPlayerService.playNext();
                performTrackListSelection(true);
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Play the previous track
                if (!serviceBound) return;
                musicPlayerService.playPrevious();
                performTrackListSelection(true);
            }
        });

        skbrSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                // If the user changes the progress, call seekTo() on the service
                if (!fromUser || !serviceBound) return;
                musicPlayerService.seekTo(position);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle shuffle mode on the service
                if (!serviceBound) return;
                musicPlayerService.toggleShuffle();
            }
        });

        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle repeat mode on the service
                if (!serviceBound) return;
                musicPlayerService.toggleRepeatMode();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start and bind the service
        initService();

        // Initialize and start the timer thread
        trackTimerThread = new TrackTimerThread();
        trackTimerThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the UI using data from the service
        updateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind the service, don't stop it just yet
        if (serviceBound) {
            musicPlayerService.setCallback(null);
            unbindService(musicServiceConnection);
            serviceBound = false;
        }

        // If the media player and the activity are stopped - stop the service and close the basta
        if (!serviceRunning) {

            // Save preferences
            prefs.edit()
                    .putInt(KEY_TRACK_INDEX, trackIndex)
                    .putInt(KEY_TRACK_TIME, trackTime)
                    .putInt(KEY_REPEAT_MODE, repeatMode.ordinal())
                    .putBoolean(KEY_SHUFFLE_ON, shuffleEnabled)
                    .apply();

            stopService(serviceIntent);
        } else {
            // If the service is still running, the track might be paused
            // Save the position of the track
            prefs.edit()
                    .putInt(KEY_TRACK_TIME, trackTime)
                    .apply();
        }

        // Stop the track timer thread to prevent unnecessary memory consumption
        trackTimerThread.interrupt();
        trackTimerThread = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - load the tracks
                Log.d(LOG_TAG, "Permission granted.");
                initTrackList();
            } else {
                // Permission denied, notify user and close the app
                Log.d(LOG_TAG, "Permission denied.");
                Toast.makeText(this, "Unable to start the app without permissions to access the device storage, please grant them!",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    // --- Helper methods

    /**
     * This method updates the UI based on data from the service
     */
    private void updateUI() {

        if (serviceBound) {
            // Pull data from service
            timerRunning = musicPlayerService.isPlaying();
            trackTime = timerRunning
                    ? musicPlayerService.getPosition()
                    : prefs.getInt(KEY_TRACK_TIME, 0);
            shuffleEnabled = musicPlayerService.isShuffled();
            repeatMode = musicPlayerService.getRepeatMode();

            if (tracks.size() > 0) {

                // Update the ListView and select the track
                performTrackListSelection(true);

                // Update UI components
                Track track = tracks.get(trackIndex);
                Log.d(LOG_TAG, "Selected track on UI update: " + track);
                int sliderProgress = (int) (trackTime / (track.getDuration() / 1000.0f) * 100.0f);
                skbrSlider.setProgress(sliderProgress);
                lblTrackName.setText(track.getFullTitle());
                lblDuration.setText(Utils.formatMillis(track.getDuration()));
                lblPosition.setText(Utils.formatSeconds(trackTime));
                btnPlay.setImageDrawable(getDrawable(musicPlayerService.isPlaying()
                        ? R.drawable.btn_pause
                        : R.drawable.btn_play));

                // Update the RepeatMode and shuffle buttons
                switch (repeatMode) {
                    case NONE:
                        btnRepeat.setImageDrawable(getDrawable(R.drawable.btn_repeat_off));
                        break;
                    case REPEAT_TRACK:
                        btnRepeat.setImageDrawable(getDrawable(R.drawable.btn_repeat_track));
                        break;
                    case REPEAT_ALL:
                        btnRepeat.setImageDrawable(getDrawable(R.drawable.btn_repeat_all));
                        break;
                }

                btnShuffle.setImageDrawable(getDrawable(shuffleEnabled
                        ? R.drawable.btn_shuffle_on
                        : R.drawable.btn_shuffle_off));

                // Load album art image
                updateAlbumImage(track);
            }
        }
    }

    /**
     * Service connection - determines behaviour on service binding
     */
    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBound = true;
            MusicPlayerService.MusicServiceBinder binder = (MusicPlayerService.MusicServiceBinder) service;
            musicPlayerService = binder.getService();
            musicPlayerService.setCallback(MainActivity.this);

            // If the tracks have already been initialized on the service, grab a reference
            // to use in the Main Activity. If not, pass the loaded tracks to the service.
            if (musicPlayerService.getTracks() != null) {
                tracks = musicPlayerService.getTracks();
                trackAdapter = new TrackAdapter(MainActivity.this, tracks);
                lstTracks.setAdapter(trackAdapter);
            } else {
                initTrackList();
            }

            // If the player is stopped, pass it the initial values
            if (!musicPlayerService.isReady()) {
                musicPlayerService.setTrackIndex(trackIndex);
                musicPlayerService.setShuffle(shuffleEnabled);
                musicPlayerService.setRepeatMode(repeatMode);
            }

            updateUI();
            Log.d(LOG_TAG, "Service bound.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d(LOG_TAG, "Service unbound.");
        }
    };

    /**
     * Start and bind the service
     */
    private void initService() {
        Log.d(LOG_TAG, "Initializing service.");
        if (serviceIntent == null)
            serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        serviceRunning = true;
        if (!serviceBound)
            bindService(serviceIntent, musicServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * Load the track list from the device's external storage
     */
    private void initTrackList() {

        if (tracks == null)
            tracks = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        }

        Log.d(LOG_TAG, "Reading tracks...");

        // Load the tracks
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.TITLE + " ASC");
        if (c != null && c.moveToFirst()) {

            // Save the column indexes to variables
            int idColumn = c.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int durationColumn = c.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            // Create the track objects and add them to the list
            do {
                Track track = new Track();
                track.setId(c.getLong(idColumn));
                track.setTitle(c.getString(titleColumn));
                track.setArtist(c.getString(artistColumn));
                track.setDuration(c.getLong(durationColumn));
                track.setAlbumArtURI(getAlbumArtURI(c.getInt(albumIdColumn)));
                tracks.add(track);
            } while (c.moveToNext());
            c.close();
        }

        // Sort
//        Collections.sort(tracks, new Comparator<Track>() {
//            @Override
//            public int compare(Track t1, Track t2) {
//                return t1.getTitle().compareToIgnoreCase(t2.getTitle());
//            }
//        });

        // Pass the track list to the service
        if (serviceBound) {
            musicPlayerService.setTracks(tracks);
        }

        // If the trackIndex pulled from prefs is larger than the list size,
        // meaning the list has been changed - reset the index
        if (trackIndex >= tracks.size())
            trackIndex = 0;

        Log.d(LOG_TAG, "Initializing UI...");

        // Prepare the UI
        Track track = tracks.get(trackIndex);
        int sliderProgress = (int) ((trackTime * 1000.0f) / track.getDuration() * 100.0f);
        skbrSlider.setProgress(sliderProgress);
        track.setSelected(true);
        lblTrackName.setText(track.getFullTitle());
        trackAdapter = new TrackAdapter(this, tracks);
        lstTracks.setAdapter(trackAdapter);
        lblPosition.setText(Utils.formatSeconds(trackTime));
        updateAlbumImage(track);
    }

    /**
     * Get the album art path using the album ID
     * @param albumId Album ID
     * @return the path to the album thumb art as string
     */
    private String getAlbumArtURI(int albumId) {
        String result = null;

        // Query
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + " = ?",
                new String[] {String.valueOf(albumId)},
                null
        );

        // If data exists - grab it
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getString(
                        cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            }
            cursor.close();
        }

        return result;
    }

    /**
     * Handles track selection on the ListView
     * @param moveToItem if true, the ListView will scroll to the item position
     */
    private void performTrackListSelection(boolean moveToItem) {
        if (!serviceBound)
            return;

        tracks.get(trackIndex).setSelected(false);
        trackIndex = musicPlayerService.getSelectedTrackIndex();
        tracks.get(trackIndex).setSelected(true);
        trackAdapter.notifyDataSetChanged();
        if (moveToItem)
            lstTracks.setSelection(Math.max(0, trackIndex - 3));
        Log.d(LOG_TAG, "Performing selection: " + trackIndex);
    }

    /**
     * Load the album art image of the selected track to the imgAlbum ImageView
     * @param track selected track
     */
    private void updateAlbumImage(Track track) {
        Log.d(LOG_TAG, "Track Album URI: " + track.getAlbumArtURI());
        if (track.getAlbumArtURI() != null) {
            Glide.with(this)
                    .load(track.getAlbumArtURI())
                    .placeholder(R.drawable.amp_icon)
                    .error(R.drawable.amp_icon)
                    .into(imgAlbum);
        } else {
            imgAlbum.setImageDrawable(getDrawable(R.drawable.amp_icon));
        }
    }

    /**
     * TrackTimerThread is controlling the position label value and seekbar progress value
     * based on the amount of time passed from a track's beginning.
     * The thread starts running at onStart() and stops at onStop().
     */
    private class TrackTimerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000L);
                    if (timerRunning) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (this) {
                                    trackTime++;
                                    String formattedTime = Utils.formatSeconds(trackTime);
                                    lblPosition.setText(formattedTime);
                                    Track track = tracks.get(trackIndex);
                                    int sliderProgress = (int) ((trackTime * 1000.0f)
                                            / track.getDuration() * 100.0f);
                                    skbrSlider.setProgress(sliderProgress);
                                    Log.d(LOG_TAG, "Progress: " + sliderProgress + "% ["
                                            + Utils.formatSeconds(trackTime) + "/"
                                            + Utils.formatMillis(track.getDuration()) + "]");
                                }
                            }
                        });
                    }
                    Log.d(LOG_TAG, "Timer Running: " + timerRunning + ", Track Time: " + trackTime);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    // --- MusicServiceCallback methods

    @Override
    public void onTrackStarted(int trackIndex) {
        // Update UI
        trackAdapter.notifyDataSetChanged();
        performTrackListSelection(shuffleEnabled);
        timerRunning = true;
        trackTime = 0;
        Track track = tracks.get(trackIndex);
        lblPosition.setText(Utils.formatMillis(0));
        lblDuration.setText(Utils.formatMillis(track.getDuration()));
        lblTrackName.setText(track.getFullTitle());
        btnPlay.setImageDrawable(getDrawable(R.drawable.btn_pause));

        // Load album image
        updateAlbumImage(track);
    }

    @Override
    public void onTrackPaused() {
        trackAdapter.notifyDataSetChanged();
        btnPlay.setImageDrawable(getDrawable(R.drawable.btn_play));
        timerRunning = false;
    }

    @Override
    public void onTrackResumed() {
        trackAdapter.notifyDataSetChanged();
        btnPlay.setImageDrawable(getDrawable(R.drawable.btn_pause));
        timerRunning = true;
    }

    @Override
    public void onTrackStopped() {
        trackAdapter.notifyDataSetChanged();
        btnPlay.setImageDrawable(getDrawable(R.drawable.btn_play));
        lblPosition.setText(Utils.formatMillis(0));
        skbrSlider.setProgress(0);
        timerRunning = false;
        trackTime = 0;
    }

    @Override
    public void onRepeatModeChanged(RepeatMode repeatMode) {
        Log.d(LOG_TAG, "Repeat mode selected: " + repeatMode.toString());

        this.repeatMode = repeatMode;

        String mode = null;
        int drawableId = 0;
        switch (repeatMode) {
            case NONE:
                mode = "Repeat OFF";
                drawableId = R.drawable.btn_repeat_off;
                break;
            case REPEAT_TRACK:
                mode = "Repeat ON - Track";
                drawableId = R.drawable.btn_repeat_track;
                break;
            case REPEAT_ALL:
                mode = "Repeat ON - All";
                drawableId = R.drawable.btn_repeat_all;
                break;
        }
        btnRepeat.setImageDrawable(getDrawable(drawableId));
        Toast.makeText(this, mode, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onShuffleModeChanged(boolean shuffleEnabled) {
        Log.d(LOG_TAG, "Shuffle enabled: " + shuffleEnabled);

        this.shuffleEnabled = shuffleEnabled;

        btnShuffle.setImageDrawable(getDrawable(shuffleEnabled
                ? R.drawable.btn_shuffle_on
                : R.drawable.btn_shuffle_off));

        Toast.makeText(this, "Shuffle " + (shuffleEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPositionChanged(int trackTime) {
        this.trackTime = trackTime;
        lblPosition.setText(Utils.formatSeconds(trackTime));
    }

}
