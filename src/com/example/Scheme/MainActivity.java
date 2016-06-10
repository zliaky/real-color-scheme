package com.example.Scheme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.os.Environment;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private Button camBtn = null;
    private Button albumBtn,msBtn = null;
    private Button btBtn = null;
    //TextView info;
    ProgressDialog myDialog;

    private String filename = "/sdcard/temp.jpg";
    Uri imageUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);


        Global.btState = false;

        camBtn = (Button) findViewById(R.id.Camera);
        camBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                imageUri = Uri.fromFile(new File(filename));

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult (cameraIntent, 1);

            }

        });

        albumBtn = (Button) findViewById(R.id.Album);
        albumBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//调用android的图库
                startActivityForResult(intent, 2);
            }

        });

        msBtn = (Button) findViewById(R.id.Repository);
        msBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//调用android的图库
                startActivityForResult(intent, 3);
            }

        });

        btBtn = (Button) findViewById(R.id.Bluetooth);

        if(Global.btState == true) {
            btBtn.setBackgroundResource(R.drawable.bluetooth_connected);
        }
        else
            btBtn.setBackgroundResource(R.drawable.bluetooth);


        btBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Global.btState == false) {
                    myDialog = ProgressDialog.show(MainActivity.this, "Connect to Pallete", "Please wait...", true, false);

                    new Thread(){

                        @Override
                        public void run() {
                            try {
                                findBT();
                                openBT();
                                Global.btState = true;
                            } catch (IOException ex) {
                            }
                            handler.sendEmptyMessage(0);
                        }}.start();

                }
                else {
                    try
                    {
                        closeBT();
                        Global.btState = false;
                        btBtn.setBackgroundResource(R.drawable.bluetooth);

                    }
                    catch (IOException ex) { }
                }
            }
        });

        //info = (TextView)findViewById(R.id.info);
        System.out.println("in create!!!!!!!!!!!!!!!!!!!!!!");


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1)//拍照
        {

            if (resultCode == Activity.RESULT_OK) {
                Global.filename = "sdcard/temp.jpg";
                Intent intent=new Intent(MainActivity.this,SchemeActivity.class);
                startActivity(intent);

            }
        } else {//相册
            switch (resultCode) {
                case Activity.RESULT_OK: {
                    Uri uri = data.getData();

                    final String scheme = uri.getScheme();
                    String filename = null;
                    if ( scheme == null )
                        filename = uri.getPath();
                    else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
                        filename = uri.getPath();
                    } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
                        Cursor cursor = getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
                        if ( null != cursor ) {
                            if ( cursor.moveToFirst() ) {
                                int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                                if ( index > -1 ) {
                                    filename= cursor.getString( index );
                                }
                            }
                            cursor.close();
                        }
                    }

                    Global.filename = filename;

                    Intent intent;
                    if(requestCode == 2)
                        intent=new Intent(MainActivity.this,SchemeActivity.class);
                    else
                        intent=new Intent(MainActivity.this,MeanShiftActivity.class);

                    startActivity(intent);
                    this.finish();
                }
                break;
                case Activity.RESULT_CANCELED:// 取消
                    break;
            }
        }

    }

    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {

            //关闭ProgressDialog
            myDialog.dismiss();
            btBtn.setBackgroundResource(R.drawable.bluetooth_connected);
            //更新UI
            //statusTextView.setText("Completed!");
        }};

    void findBT()
    {
        Global.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if(Global.mBluetoothAdapter == null)
//        {
//            info.setText("No bluetooth adapter available");
//        }

        if(!Global.mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = Global.mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-06"))
                {
                    Global.mmDevice = device;
                    break;
                }
            }
        }
        //info.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

        Global.mmSocket = Global.mmDevice.createRfcommSocketToServiceRecord(uuid);
        Log.d("openBt","create");

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Global.mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                Global.mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        //mmSocket.connect();
        Log.d("openBt","connect");
        Global.mmOutputStream = Global.mmSocket.getOutputStream();
        Log.d("openBt","out");
        Global.mmInputStream = Global.mmSocket.getInputStream();
        Log.d("openBt","in");

        beginListenForData();
        Log.d("openBt","listen");

        //info.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        //final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        Global.stopWorker = false;
        Global.readBufferPosition = 0;
        Global.readBuffer = new byte[1024];
        Global.workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !Global.stopWorker)
                {
                    try
                    {
                        int bytesAvailable = Global.mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            Global.mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[Global.readBufferPosition];
                                    System.arraycopy(Global.readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    Global.readBufferPosition = 0;
                                }
                                else
                                {
                                    Global.readBuffer[Global.readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        Global.stopWorker = true;
                    }
                }
            }
        });

        Global.workerThread.start();
    }

    void closeBT() throws IOException
    {
        Global.stopWorker = true;
        Global.mmOutputStream.close();
        Global. mmInputStream.close();
        Global.mmSocket.close();
        //info.setText("Bluetooth Closed");
    }
}
