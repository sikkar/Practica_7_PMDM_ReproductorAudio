package com.izv.angel.reproductoraudio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class ServicioAudio extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener {

    public static MediaPlayer mp;
    private enum Estados {
        idle,
        initialized,
        prepairing,
        prepared,
        started,
        paused,
        completed,
        stopped,
        end,
        error
    }

    private Estados estado;
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String STOP = "stop";
    public static final String NEXT = "next";
    public static final String PREV = "prev";
    private boolean reproducir;
    public static int songPosn;
    private String songTitle;
    private static final int NOTIFY_ID=1;
    int mediaPosition;
    int mediaMax;
    //Intent intent;
    private final Handler handler = new Handler();
    private static int songEnded;
    public static final String BROADCAST_ACTION = "izv.com.angel.reproductoraudio.seekprogress";
    Intent seekIntent;


    /* ******************************************************* */
    // METODO CONSTRUCTOR //
    /* ****************************************************** */

    public ServicioAudio() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(broadcastReceiver, new IntentFilter(Principal.BROADCAST_SEEKBAR));
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnCompletionListener(this);
        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mp = new MediaPlayer();
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setOnBufferingUpdateListener(this);
            mp.setOnSeekCompleteListener(this);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            estado = estado.idle;
        } else {
            //
        }
        seekIntent = new Intent(BROADCAST_ACTION);
        estado = Estados.idle;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ******************************************************* */
    // METODOS SOBREESCRITO //
    /* ****************************************************** */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(PLAY)) {
            songPosn = intent.getIntExtra("id", 0);
            Log.v("numero", songPosn + "");
            stop();
            play();
        } else if (action.equals(STOP)) {
            stop();
        } else if (action.equals(PAUSE)) {
            pause();
        } else if (action.equals(NEXT)) {
            next();
        }else if (action.equals(PREV)) {
            prev();
        }
        setupHandler();
        return super.onStartCommand(intent, flags, startId);
    }

     /* ******************************************************* */
    // METODOS PARA EL HANDLER QUE ACTUALIZA LA SEEKBAR//
    /* ****************************************************** */

    private void setupHandler() {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {

            LogMediaPosition();

            handler.postDelayed(this, 1000); // 2 seconds

        }
    };

    private void LogMediaPosition() {
        if (mp.isPlaying()) {
            mediaPosition = mp.getCurrentPosition();
            mediaMax = mp.getDuration();
            seekIntent.putExtra("counter", String.valueOf(mediaPosition));
            seekIntent.putExtra("mediamax", String.valueOf(mediaMax));
            seekIntent.putExtra("song_ended", String.valueOf(songEnded));
            sendBroadcast(seekIntent);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!mp.isPlaying()){
            play();
        }
    }

    // --Receive seekbar position if it has been changed by the user in the
    // activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    // Update seek position from Activity
    public void updateSeekPos(Intent intent) {
        int seekPos = intent.getIntExtra("seekpos", 0);
        if (mp.isPlaying()) {
            handler.removeCallbacks(sendUpdatesToUI);
            mp.seekTo(seekPos);
            setupHandler();
        }

    }
    /* ******************************************************* */
    // INTERFAZ PREPARED, COMPLETED LISTENER //
    /* ****************************************************** */

    @Override
    public void onPrepared(MediaPlayer mp) {
        estado = Estados.prepared;
        if (reproducir) {
            mp.start();
            estado = Estados.started;
            Intent notIntent = new Intent(this, Principal.class);
            notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder = new Notification.Builder(this);

            builder.setContentIntent(pendInt)
                    .setSmallIcon(R.mipmap.playsmall)
                    .setTicker(songTitle)
                    .setOngoing(true)
                    .setContentTitle("Playing")
                    .setContentText(songTitle);
            Notification not = builder.build();
            startForeground(NOTIFY_ID, not);
        }
    }



    @Override
    public void onCompletion(MediaPlayer mp) {
        estado = Estados.completed;
        next();
        //mp.stop();
        //estado=Estados.sttoped;
    }

    /* ******************************************************* */
    // INTERFAZ AUDIOFOCUSCHANGE LISTENER //
    /* ****************************************************** */

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                play();
                mp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.4f, 0.4f);
                break;
        }
    }


    @Override
    public void onDestroy() {
        mp.reset();
        mp.release();
        mp = null;
        stopService(new Intent(this, ServicioAudio.class));
        stopForeground(true);
        handler.removeCallbacks(sendUpdatesToUI);
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    /* ******************************************************* */
    // METODOS DE AUDIO //
    /* ****************************************************** */

    private void play() {
        if (estado == Estados.error) {
            estado = Estados.idle;
        }
        if (estado == Estados.idle || estado == Estados.stopped) {
            reproducir = true;
            try {
                Song cancion = Principal.playList.get(songPosn);
                songTitle=cancion.getTitle();
                long cancionActual = cancion.getId();
                Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cancionActual);
                mp.setDataSource(getApplicationContext(), trackUri);
                estado = Estados.initialized;
            } catch (IOException e) {
                estado = Estados.error;
            }
        }
        if (estado == Estados.initialized || estado == Estados.stopped) {
            reproducir = true;
            mp.prepareAsync();
            estado = Estados.prepairing;
        } else if (estado == Estados.prepairing) {
            reproducir = true;
        } else if (estado == Estados.prepared || estado == Estados.started || estado == Estados.paused || estado == Estados.completed) {
            mp.start();
            estado = Estados.started;
        }
    }

    private void pause() {
        if (estado == Estados.started) {
            mp.pause();
            estado = Estados.paused;
        }else{
            play();
        }
    }

    private void stop() {
        if (estado == Estados.prepared || estado == Estados.started || estado == Estados.paused || estado == Estados.completed) {
            mp.seekTo(0);
            mp.stop();
            mp.reset();
            estado = Estados.stopped;
        }
        reproducir = false;
        stopForeground(true);
    }

    public void next(){
        stop();
        songPosn++;
        if(songPosn>=Principal.playList.size()) songPosn=0;
        play();
    }
    public void prev(){
        stop();
        songPosn--;
        if(songPosn<0) songPosn=Principal.playList.size()-1;
        play();
    }

}