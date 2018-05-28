package com.daniel.awesomemusicplayer.networking;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * RequestExecutor handles HTTP requests, reads the response and passes it
 * to the listener using RequestListener.
 * The response can be handled and processed on both the background and the UI threads.
 */
public class RequestExecutor extends AsyncTask<String, Void, String> {

    /** Log tag */
    private static final String LOG_TAG = "RequestExecutor";

    /** Listener instance */
    private RequestListener requestListener;

    /** Variable that determines if the task should run. This is set to false in the stop() method. */
    private volatile boolean go;

    public RequestExecutor(@NonNull RequestListener requestListener) {
        this.requestListener = requestListener;
        this.go = true;
    }

    @Override
    protected String doInBackground(String... strings) {

        int resultCode = 0;
        try {

            Log.d(LOG_TAG, "Executing request to URL: " + strings[0]);

            // Build the request
            URL url = new URL(strings[0]);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "text/html");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0");
            con.connect();

            if (!go)
                return null;

            // Get the response code
            resultCode = con.getResponseCode();

            // Initialize a StringBuilder for reading the response
            StringBuilder sb = new StringBuilder();

            // Read the response
            BufferedReader input = null;
            try {
                input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while (go && (line = input.readLine()) != null)
                    sb.append(line);
            } finally {
                if (input != null)
                    input.close();
            }

            String result = sb.toString();

            // Give the listener an opportunity to handle the response on the background thread
            // Before passing it to the UI thread.
            if (go && requestListener != null)
                return requestListener.doOnBackgroundThread(result);
            else
                return result;

        } catch (Exception e) {
            // Notify the listener an error has occurred.
            if (go && requestListener != null)
                requestListener.onError(resultCode, e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        // Handle on UI thread
        if (go && requestListener != null)
            requestListener.doOnUIThread(s);
    }

    /**
     * Abort the task
     */
    public void stop() {
        go = false;
    }

}
