package com.example.flask_model_keras;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button predict, stop;
    TextView result;
    String url = "https://speech-recog.herokuapp.com/predict";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    private WavAudioRecorder mRecorder;
//    private static final String mRcordFilePath = Environment.getExternalStorageDirectory() + "/testwave.wav";
    String mRcordFilePath;
    private Button btnControl, btnClear;
    private TextView textDisplay;

    private static int MICROPHONE_PERMISSION_CODE = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        predict = findViewById(R.id.predict);
        stop = findViewById(R.id.stop);
        result= findViewById(R.id.result);
        btnControl = findViewById(R.id.btnControl);
        btnControl.setText("Start");
        mRcordFilePath = getRecordingFilePath();

        if (isMicrophonePresent()){
            getMicroPhonePermission();
        }

        mRecorder = WavAudioRecorder.getInstanse();
        mRecorder.setOutputFile(mRcordFilePath);

        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    if (WavAudioRecorder.State.INITIALIZING == mRecorder.getState()) {
                        mRecorder.prepare();
                        mRecorder.start();
                        btnControl.setText("Stop");
                    } else if (WavAudioRecorder.State.ERROR == mRecorder.getState()) {
                        mRecorder.release();
                        mRecorder = WavAudioRecorder.getInstanse();
                        mRecorder.setOutputFile(mRcordFilePath);
                        btnControl.setText("Start");
                    } else {
                        mRecorder.stop();
                        mRecorder.reset();
                    }

                    btnClear = findViewById(R.id.btnClear);
                    btnClear.setText("Clear");
                    btnClear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            File pcmFile = new File(mRcordFilePath);
                            if (pcmFile.exists()) {
                                pcmFile.delete();
                            }
                        }
                    });
                    textDisplay = findViewById(R.id.Textdisplay);
                    textDisplay.setText("recording saved to: " + mRcordFilePath);
//                    mediaRecorder = new MediaRecorder();
//                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//                    mediaRecorder.setOutputFile(getRecordingFilePath());
//                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//                    mediaRecorder.prepare();
//                    mediaRecorder.start();
//                    Log.d("path", getRecordingFilePath());
//
//                    Toast.makeText(getBaseContext(), "Recording is started", Toast.LENGTH_SHORT).show();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mRecorder) {
            mRecorder.release();
        }
    }

    public void generateNoteOnSD(String sFileName, String sBody) {

//        return file.getPath();
        try {
//            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
//            if (!root.exists()) {
//                root.mkdirs();
//            }
            ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
            File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File file = new File(musicDirectory, "tmp" + ".txt");

//            Log.d("path",Environment.getExternalStorageDirectory().getPath());
//            File gpxfile = new File(root, sFileName);

            FileWriter writer = new FileWriter(file);
            writer.append(sBody);
            writer.flush();
            writer.close();
//            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void btnStop(View v){
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        // hit the API -> Volley

        File audioFile = new File(getRecordingFilePath());
        byte[] bytes = new byte[0];
        try {
            bytes = FileUtils.readFileToByteArray(audioFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String encoded = Base64.encodeToString(bytes, 0);
        generateNoteOnSD("tmp.txt",encoded);
        System.out.print(encoded);

//        FileInputStream fileInputStreamReader = null;
//        try {
//            fileInputStreamReader = new FileInputStream(audioFile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        byte[] bytes = new byte[(int)audioFile.length()];
//        try {
//            fileInputStreamReader.read(bytes);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
//        Log.d("Encoded", encoded);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response); //zadt casting String
                            String data = jsonObject.getString("prediction");
                            result.setText(data);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(MainActivity.this, error.getMessage()+ " bad", Toast.LENGTH_SHORT).show();
                        Log.d("tag",error.getMessage()+ "bad");
//                                 As of f605da3 the following should work
//                        NetworkResponse response = error.networkResponse;
//                        if (error instanceof ServerError && response != null) {
//                            try {
//                                String res = new String(response.data,
//                                        HttpHeaderParser.parseCharset(response.headers, "utf-8"));
//                                // Now you can use any deserializer to make sense of data
//                                JSONObject obj = new JSONObject(res);
//                            } catch (UnsupportedEncodingException e1) {
//                                // Couldn't properly decode data to string
//                                e1.printStackTrace();
//                            } catch (JSONException e2) {
//                                // returned data is not JSONObject?
//                                e2.printStackTrace();
//                            }
//                        }
                    }
                }) {
            @Override
            protected Map<String,String> getParams() {

                Map<String,String> params = new HashMap<String,String>();
                params.put("file", encoded);
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);

        Toast.makeText(this, "Recording is stopped", Toast.LENGTH_SHORT).show();
    }

    public void btnPlay(View v){
       try {
           mediaPlayer = new MediaPlayer();
           mediaPlayer.setDataSource(getRecordingFilePath());
           mediaPlayer.prepare();
           mediaPlayer.start();
           Toast.makeText(this, "Recording is playing", Toast.LENGTH_SHORT).show();
       }catch (Exception e){
           e.printStackTrace();
       }

    }

    public boolean isMicrophonePresent(){
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){
            return true;
        }else {
            return false;
        }
    }

    public void getMicroPhonePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    MICROPHONE_PERMISSION_CODE);
        }
    }

    private String getRecordingFilePath(){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "file" + ".wav");
        return file.getPath();
    }
}