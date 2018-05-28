# AwesomeMusicPlayer

This is a simple music player app for my Android module project. :)

## Basic functionality:
* Reading tracks from the device storage.
* The music is playing on a service, meaning the app can be closed or the screen can been locked while the music is playing.
* The app sends an HTTP request to azlyrics.com to retrieve the lyrics of the selected song and display them on the screen. (I know it's prohibited, but this app is for educational purposes only.)
* Simple UI controls.
* Shuffle and repeat functions.
* Album cover art thumbnail.
* Equalizer animation to demonstrate a playing track.
* The service is stopped using the stop (square) button.

## Main elements used for the app:
* Service, specifically Bound Service
* ContentResolver
* MediaPlayer
* Glide library
* AsyncTask
* Thread
* Handler
* Interfaces
