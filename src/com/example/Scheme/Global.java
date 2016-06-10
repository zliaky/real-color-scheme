package com.example.Scheme;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by windows on 2016/3/12.
 */
public class Global {
    public static String filename;

    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothSocket mmSocket;
    public static BluetoothDevice mmDevice;
    public static OutputStream mmOutputStream;
    public static InputStream mmInputStream;
    public static Thread workerThread;

    public static byte[] readBuffer;
    public static int readBufferPosition;
    public static volatile boolean stopWorker;

    public static boolean btState;

}
