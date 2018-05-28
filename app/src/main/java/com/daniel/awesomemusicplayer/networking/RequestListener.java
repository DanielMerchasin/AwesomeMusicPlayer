package com.daniel.awesomemusicplayer.networking;

/**
 * RequestExecutor event listener.
 */
public interface RequestListener {

    /**
     * This method is called in the background thread,
     * after the response from the HTTP request has been read.
     * @param result The response body as string
     * @return String to pass to the UI thread.
     */
    String doOnBackgroundThread(String result);

    /**
     * This method is called in the UI thread after the task is finished.
     * @param result The string passed from doOnBackgroundThread()
     */
    void doOnUIThread(String result);

    /**
     * This method is called in case of an error
     * @param errorCode The response code. 0 if the connection hasn't been established.
     * @param e The exception that has been thrown
     */
    void onError(int errorCode, Exception e);

}
