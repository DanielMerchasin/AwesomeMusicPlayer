package com.daniel.awesomemusicplayer.networking;

public interface RequestListener {
    String doOnBackgroundThread(String result);
    void doOnUIThread(String result);
    void onError(int errorCode, Exception e);
}
