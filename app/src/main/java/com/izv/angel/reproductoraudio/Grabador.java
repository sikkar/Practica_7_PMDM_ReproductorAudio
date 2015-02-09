package com.izv.angel.reproductoraudio;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;


public class Grabador extends Activity {

    private MediaRecorder recorder = null;
    private Button btGrabar, btParar;
    private TextView tvEstado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_grabador);
        btParar = (Button) findViewById(R.id.btParar);
        btGrabar = (Button) findViewById(R.id.btGrabar);
        tvEstado = (TextView)findViewById(R.id.tvGrabar);
    }



    protected void onPause() {
        super.onPause();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }


    public void grabar(View view) {
        tvEstado.setText(R.string.grabando);
        Date fecha = new GregorianCalendar().getTime();
        String archivo = "record" + fecha.toString().replace(" ", "_") + ".mp3";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), archivo);
        if (recorder != null) {
            recorder.release();
        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        recorder.setOutputFile(file.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.v("giftlist", "io problems while preparing [" +file.getAbsolutePath() + "]: " + e.getMessage());
        }


    }

    public void detener(View view) {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            tvEstado.setText(R.string.detener);
        }

    }
}
