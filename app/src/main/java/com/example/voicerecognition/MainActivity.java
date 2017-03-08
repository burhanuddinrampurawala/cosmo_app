package com.example.voicerecognition;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.VideoView;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

public class MainActivity extends Activity {

    private SpeechRecognizerManager mSpeechManager;

    String ch = "";
    int cs;
    BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();;
    BluetoothDevice myDevice = null;
    BluetoothSocket mySocket = null;
    OutputStream myOutputStream;
    final String MAC_ADDRESS = "98:D3:31:80:79:31";
    String s = null;
    VideoView videoView;
    final int neutral = R.raw.neutral;
    final int neutral2 = R.raw.neutral2;
    final int angry = R.raw.angry;
    final int sad = R.raw.sad;
    final int happy = R.raw.happy;
    Handler mHandler;
    TextToSpeech t1;
    String message2;
    private AIService aiService;
    private AIRequest aiRequest;
    private ai.api.AIDataService aiDataService;
    final String TAG = "MAINACTIVITY";
    private AIResponse response;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AIConfiguration config = new AIConfiguration("113d9a1fd6aa47988a6193227689dc99",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiDataService = new AIDataService(config);
        Log.i(TAG, config.toString());
        aiService = AIService.getService(getApplicationContext(), config);
        aiRequest = new AIRequest();

        videoView = (VideoView) findViewById(R.id.videoView);
        play(neutral);

        if (bt == null) {
            Log.d("MAINACTIVITY", "bluetooth not detected");
        } else {
            if (!bt.isEnabled()) {
                Intent enablebt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enablebt, 1);
            }

        }

        if(PermissionHandler.checkPermission(this,PermissionHandler.RECORD_AUDIO)) {
            if(mSpeechManager==null)
            {
                SetSpeechListener();
            }
            else if(!mSpeechManager.ismIsListening())
            {
                mSpeechManager.destroy();
                SetSpeechListener();
            }
        }
        else
        {
            PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO,this);
        }
//        this.mHandler = new Handler();
//        this.mHandler.postDelayed(m_Runnable,8000);

    }



//    private final Runnable m_Runnable = new Runnable()
//    {
//        public void run()
//
//        {
//            play(neutral);
//
////            play(angry);
////            play(sad);
////            play(happy);
//            mHandler.postDelayed(m_Runnable, 8000);
//        }
//
//    };
    private void play(final int video) {
        String uriPath = "android.resource://com.example.voicerecognition/" + video;
        Uri uri2 = Uri.parse(uriPath);
        videoView.setVideoURI(uri2);
        videoView.requestFocus();
        videoView.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case PermissionHandler.RECORD_AUDIO:
                if(grantResults.length>0) {
                    if(grantResults[0]== PackageManager.PERMISSION_GRANTED) {

                    }
                }
                break;
        }
    }
    public void onActivityResult(int request_code, int result_code, Intent i) {
        super.onActivityResult(request_code, result_code, i);
        switch (request_code) {
            case 1:
                if (result_code == RESULT_OK) {
                    Log.d("MAINACTIVITY", "bluetooth enabled");
                } else if (result_code == RESULT_CANCELED) {
                    Log.d("MAINACTIVITY", "permission denied");
                    finish();
                }
        }
    }

    private void SetSpeechListener()
    {
        mSpeechManager=new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {


                if (results != null && results.size() > 0) {
                    Log.i("MAINACTIVITY", results.get(0));
                    if(!results.get(0).isEmpty()) {
                        aiRequest.setQuery(results.get(0));
                        aiRequest.setLanguage("en");
                        new AsyncTask<AIRequest, Void, AIResponse>() {
                            @Override
                            protected AIResponse doInBackground(AIRequest... requests) {
                                final AIRequest request = requests[0];
                                try {
                                    response = aiDataService.request(aiRequest);
                                    return response;
                                } catch (AIServiceException e) {
                                    Log.e(TAG,e.toString());
                                }
                                return null;
                            }
                            @Override
                            protected void onPostExecute(AIResponse aiResponse) {
                                if (aiResponse != null) {
                                    // process aiResponse here
                                    Log.i(TAG, aiResponse.toString());
                                    onResult(aiResponse);
                                }
                            }
                        }.execute(aiRequest);
                    }
//                        check(results.get(0));
                    //result_tv.setText(sb.toString());
                    //}
                }
            }
        });

    }
    public void onResult(final AIResponse response) {

        Result result = response.getResult();

        //Get speech

        final String speech = result.getFulfillment().getSpeech();
        Log.i(TAG, "Speech: " + speech);
        new Thread(new Runnable() {
            @Override
            public void run() {
                t1.speak(speech,TextToSpeech.QUEUE_FLUSH, null);
            }
        }).start();

        //Get action
        ch = result.getAction();
        Log.i(TAG, "Action: " + ch);
        cs = 0;
        //play videos according to action
        if(ch.equalsIgnoreCase("h")){
            play(happy);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("s")){
            play(sad);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("a")){
            play(angry);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("f")){
            play(happy);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("b")){
            play(happy);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("l")){
            play(happy);
            cs = 1;
        }
        else if(ch.equalsIgnoreCase("r")){
            play(happy);
            cs = 1;
        }


    }
//    private void check(String message) {
//        boolean hello = false;
//        if(message.length()>4)
//            hello = containsHello(message);
//        cs=0;
//        if(message.equalsIgnoreCase("move forward") || message.equalsIgnoreCase("forward")){
//            ch = "f";
//            cs = 1;
//        }
//        else if(hello){
//            t1.setPitch(2);
//            if(!message2.isEmpty()){
//                t1.speak("hello" + message2,TextToSpeech.QUEUE_FLUSH, null);
//            }
//            else
//                t1.speak("hi",TextToSpeech.QUEUE_FLUSH, null);
//            play(happy);
//        }
//        else if (message.equalsIgnoreCase("move backward") || message.equalsIgnoreCase("backward")
//                || message.equalsIgnoreCase("move backwards") || message.equalsIgnoreCase("awkward")
//                || message.equalsIgnoreCase("backwards") ){
//            ch = "b";
//            cs = 1;
//        }
//        else if (message.equalsIgnoreCase("move right") || message.equalsIgnoreCase("right")
//                || message.equalsIgnoreCase("light") || message.equalsIgnoreCase("turn right") ){
//            ch = "r";
//            cs = 1;
//        }
//        else if (message.equalsIgnoreCase("move left") || message.equalsIgnoreCase("left")
//                || message.equalsIgnoreCase("turn left") ){
//            ch = "l";
//            cs = 1;
//        }
//        else if (message.equalsIgnoreCase("stop") ){
//            ch = "t";
//            cs = 1;
//        }
//        else if(message.equalsIgnoreCase("angry") || message.equalsIgnoreCase("anger")
//               || message.equalsIgnoreCase("I am angry") || message.equalsIgnoreCase("I'm angry")
//                || message.equalsIgnoreCase("hungry") || message.equalsIgnoreCase("I am hungry")
//                || message.equalsIgnoreCase("I'm hungry")){
//            ch = "a";
//            cs = 1;
//            play(angry);
//        }
//        else if(message.equalsIgnoreCase("happy") || message.equalsIgnoreCase("I am happy")
//                || message.equalsIgnoreCase("I'm happy")){
//            ch = "h";
//            cs = 1;
//            play(happy);
//        }
//        else if(message.equalsIgnoreCase("sad") || message.equalsIgnoreCase("I am sad")
//                || message.equalsIgnoreCase("I'm sorry")|| message.equalsIgnoreCase("side")){
//            ch = "s";
//            cs = 1;
//            play(sad);
//        }
//        Log.d("MAINACTIVITY", ch);
//    }

//    private boolean containsHello(String message) {
//        if(message.equalsIgnoreCase("hello")){
//            message2 = "";
//            return true;
//        }
//        else if (message.substring(0,5).equalsIgnoreCase("hello")){
//            char [] a = message.toCharArray();
//            int i;
//            for( i = a.length-1; i>=0; i--){
//                if(a[i] == ' ')
//                    break;
//            }
//            message2 = message.substring(++i);
//            Log.i("MAINACTIVITY",message2);
//            return true;
//        }
//        else
//            return false;
//    }

    @Override
    protected void onStart() {
        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status!=TextToSpeech.ERROR){
                    t1.setLanguage(Locale.US);
                }
            }
        });
        super.onStart();
    }

    @Override
    protected void onResume() {
        if(cs==1){
            bluetoothConnect();
            try {
                createSocket(s);
            } catch (IOException ioe) {
                Log.d("MAINACTIVITY", ioe.toString());
            }
        }


        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cs==1){
            if (bt.isEnabled()) {
                try {
                    mySocket.close();
                } catch (IOException e) {
                    Log.d("MAINACTIVITY", "error while closing socket , " + e);
                }
            }
        }

        if(mSpeechManager!=null) {
            mSpeechManager.destroy();
            mSpeechManager=null;
        }

    }

    @Override
    protected void onDestroy() {


        if(cs == 1){
            if (bt.isEnabled()) {

                try {
                    mySocket.close();
                } catch (IOException ioe) {
                    Log.d("MAINACTIVITY", "unable to close bt socket , " + ioe);
                }
                bt.disable();
                Log.d("MAINACTIVITY", "BT DISABLED");

            }

        }
        if(mSpeechManager!=null) {
            mSpeechManager.destroy();
            mSpeechManager=null;
        }
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onDestroy();

    }
    public void bluetoothConnect() {
        Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
        ArrayList<String> listOfDevices = new ArrayList<>();
        int c = 0;

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice bd : pairedDevices) {

                c++;
                listOfDevices.add(bd.getName() + "\n" + bd.getAddress() + "\n" + bd.getBluetoothClass());

                //change MAC as device to connect changes
                if (bd.getAddress().equals(MAC_ADDRESS)) {
                    myDevice = bd;
                    Log.d("MAINACTIVITY", myDevice.getName());


                    break;
                } else if (c == pairedDevices.size()) {
                    Log.d("MAINACTIVITY", "device not found, pair device first");
                }

            }


        }
    }


    void createSocket(String f) throws IOException {


        bt.cancelDiscovery();
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mySocket = myDevice.createRfcommSocketToServiceRecord(uuid);
        try {
            mySocket.connect();
            Log.d("MAINACTIVITY", "socket connected");
        } catch (IOException e) {
//            Toast.makeText(this, "socket connection failed \n" + e, Toast.LENGTH_SHORT).show();
//            Log.d("MAINACTIVITY", e.toString());

            try {
                Log.d("MAINACTIVITY", "trying fallback...");

                mySocket = (BluetoothSocket) myDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(myDevice, 1);
                mySocket.connect();

                Log.d("MAINACTIVITY", " socket Connected");
            } catch (Exception e2) {
                Log.d("MAINACTIVITY", "Couldn't establish Bluetooth connection!");
            }
        }

        ConnectedThread c = new ConnectedThread();
        c.connectedThread(mySocket, f);
    }

    public class ConnectedThread extends Thread {
        BluetoothSocket socket;
        String f;
        public void connectedThread  (BluetoothSocket socket, String f){
            this.socket = socket;
            this.f=f;
        }
        @Override
        public void run() {
            try {
                myOutputStream = socket.getOutputStream();
                Log.d("MAINACTIVITY", "got output stream");
            } catch (IOException e) {
                Log.d("MAINACTIVITY", e.toString());
            }

            try {
                myOutputStream.write(f.getBytes());
                Log.d("MAINACTIVITY", "wrote value on serial out");
            } catch (IOException e) {
                Log.d("MAINACTIVITY", e.toString());
            }

        }
    }
}
