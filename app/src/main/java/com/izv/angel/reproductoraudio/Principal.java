package com.izv.angel.reproductoraudio;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;


public class Principal extends ActionBarActivity implements SeekBar.OnSeekBarChangeListener {

    public static ArrayList<Song> playList;
    private ListView lvCanciones;
    private AdaptadorCanciones adC;
    // --Seekbar variables --
    private SeekBar seekBar;
    private int seekMax;
    private static int songEnded = 0;
    boolean mBroadcastIsRegistered;
    public static final String BROADCAST_SEEKBAR = "com.izv.angel.reproductoraudio.sendseekbar";
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);
        // --- set up seekbar intent for broadcasting new position to service ---
        intent = new Intent(BROADCAST_SEEKBAR);
        lvCanciones = (ListView) findViewById(R.id.listView);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        playList = new ArrayList<Song>();
        getCanciones();
        adC = new AdaptadorCanciones(this, playList);
        lvCanciones.setAdapter(adC);
    }

    // -- Broadcast Receiver to update position of seekbar from service --
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    private void updateUI(Intent serviceIntent) {
        String counter = serviceIntent.getStringExtra("counter");
        String mediamax = serviceIntent.getStringExtra("mediamax");
        String strSongEnded = serviceIntent.getStringExtra("song_ended");
        int seekProgress = Integer.parseInt(counter);
        seekMax = Integer.parseInt(mediamax);
        songEnded = Integer.parseInt(strSongEnded);
        seekBar.setMax(seekMax);
        seekBar.setProgress(seekProgress);
        if (songEnded == 1) {
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            stopService(new Intent(this, ServicioAudio.class));
            Intent grabar = new Intent(this,Grabador.class);
            startActivity(grabar);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, ServicioAudio.class));
        if (mBroadcastIsRegistered) {
            try {
                unregisterReceiver(broadcastReceiver);
                mBroadcastIsRegistered = false;
            } catch (Exception e) {
            }
        }
    }

    /*  METODOS DE CONTROL DE REPRODUCCION */

    public void songPicked(View v) {
        int id = (int) v.getTag();
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.putExtra("id", id);
        intent.setAction(ServicioAudio.PLAY);
        startService(intent);
        registerReceiver(broadcastReceiver, new IntentFilter(ServicioAudio.BROADCAST_ACTION));
        mBroadcastIsRegistered = true;
    }

    public void stop(View v) {
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.setAction(ServicioAudio.STOP);
        startService(intent);
    }

    public void pause(View v) {
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.setAction(ServicioAudio.PAUSE);
        startService(intent);
    }


    public void next(View view) {
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.setAction(ServicioAudio.NEXT);
        startService(intent);
    }

    public void prev(View view) {
        Intent intent = new Intent(this, ServicioAudio.class);
        intent.setAction(ServicioAudio.PREV);
        startService(intent);
    }

    /*  METODO PARA SACAR TODAS LAS CANCIONES DEL PROVIDER A UN ARRAYLIST */

    public void getCanciones() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                playList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    /*  METODOS DE CONTROL DE LA SEEKBAR*/

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int seekPos = seekBar.getProgress();
            intent.putExtra("seekpos", seekPos);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
