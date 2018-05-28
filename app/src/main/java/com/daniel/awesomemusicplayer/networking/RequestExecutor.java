package com.daniel.awesomemusicplayer.networking;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RequestExecutor extends AsyncTask<String, Void, String> {

    private static final String LOG_TAG = "RequestExecutor";

    private RequestListener requestListener;
    private boolean go;

    public RequestExecutor(RequestListener requestListener) {
        this.requestListener = requestListener;
        this.go = true;
    }

    @Override
    protected String doInBackground(String... strings) {

        int resultCode = 0;
        try {

            Log.d(LOG_TAG, "Executing request to URL: " + strings[0]);

            URL url = new URL(strings[0]);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "text/html");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0");
            con.connect();
            resultCode = con.getResponseCode();

            StringBuilder sb = new StringBuilder();

            if (!go)
                return null;

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

            if (go && requestListener != null)
                return requestListener.doOnBackgroundThread(result);
            else
                return result;

        } catch (Exception e) {
            if (go && requestListener != null)
                requestListener.onError(resultCode, e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        if (go && requestListener != null)
            requestListener.doOnUIThread(s);
    }

    public void stop() {
        go = false;
    }

}
