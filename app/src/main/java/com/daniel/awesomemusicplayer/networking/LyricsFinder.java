package com.daniel.awesomemusicplayer.networking;

import android.util.Log;

import com.daniel.awesomemusicplayer.tracks.Track;
import com.daniel.awesomemusicplayer.util.Utils;

public class LyricsFinder {

    /** Callback to MainActivity */
    public interface LyricsFinderListener {
        /**
         * This method is called if the lyrics are found and parsed successfully.
         * @param result The parsed lyrics.
         */
        void onResult(String result);
    }

    /** Log tag */
    private static final String LOG_TAG = "LyricsFinder";

    /** The HTTP request task */
    private RequestExecutor task;

    /** Callback instance */
    private LyricsFinderListener lyricsFinderListener;

    public LyricsFinder(LyricsFinderListener lyricsFinderListener) {
        this.lyricsFinderListener = lyricsFinderListener;
    }

    /**
     * Get the lyrics for the selected track.
     * Don't call lyricsFinderListener.onResult() if lyrics are not found.
     * @param track The selected track
     */
    public void parse(Track track) {
        // Abort the previous request and start a new one
        stopTask();

        RequestExecutor requestExecutor = new RequestExecutor(new RequestListener() {
            @Override
            public String doOnBackgroundThread(String result) {
                return extractLyrics(result);
            }

            @Override
            public void doOnUIThread(String result) {
                task = null;
                if (lyricsFinderListener != null)
                    lyricsFinderListener.onResult(result);
            }

            @Override
            public void onError(int errorCode, Exception e) {
                Log.d(LOG_TAG, "Error: [" + errorCode + "] " + e.getMessage());
            }
        });

        // Give a reference to the running task
        task = requestExecutor;

        // Parse the track data and convert it to the URL
        String artistName = track.getArtist().replaceAll("[^A-Za-z0-9]", "")
                .replaceAll("\\s", "").toLowerCase();
        String songName = track.getTitle().replaceAll("[^A-Za-z0-9]", "")
                .replaceAll("\\s", "").toLowerCase();
        String lyricsURL = "https://www.azlyrics.com/lyrics/" + artistName + "/" + songName + ".html";

        Log.d(LOG_TAG, "Lyrics URL: " + lyricsURL);

        // Execute the request
        requestExecutor.execute(lyricsURL);
    }

    /**
     * Extracts the lyrics from the webpage
     * @param data HTML content of the webpage
     * @return The song lyrics as plain text
     */
    private String extractLyrics(String data) {

        Log.d(LOG_TAG, "Extracting lyrics from data: " + data);

        String find = ". -->";

        int index = data.indexOf(find);
        if (index != -1) {

            int startPoint = index + find.length();

            String rawText = data.substring(startPoint, data.indexOf("</div>", startPoint));

            Log.d(LOG_TAG, "Raw text: " + rawText);

            // Convert HTML code to plain text
            return Utils.translateSpecialHTMLCharacters(rawText)
                    .replaceAll("<br>", "\r\n")
                    .replaceAll("<i>", "")
                    .replaceAll("</i>", "");
        }

        return null;
    }

    /**
     * Stops the running AsyncTask
     */
    private void stopTask() {
        if (task != null) {
            task.stop();
            task = null;
        }
    }

}