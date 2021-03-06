package bm.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {


    ArrayAdapter<String> listAdapter;
    Button btnConnectNew;
    ListView listView;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<BluetoothDevice> devices;
    ArrayList<String> pairedDevices;
    IntentFilter filter;
    BroadcastReceiver reciever;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    Handler mHandler = new Handler(){
        @Override
    public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch(msg.what){
                case SUCCESS_CONNECT:
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                    Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                    String s = "YOU DID IT GOOD JOB BLUETOOTH";
                    connectedThread.write(s.getBytes());
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-0000-00805F9B34FB");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        if(btAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "No Bluetooth Detected", Toast.LENGTH_LONG).show();
        }
        else
        {
            if(!btAdapter.isEnabled())
            {
                turnOnBT();
            }

            getPairedDevices();
            startDiscovery();
        }

    }

    private void startDiscovery() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    private void init() {
        btnConnectNew = (Button) findViewById(R.id.btnConnectNew);
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter(listAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<String>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices = new ArrayList<BluetoothDevice>();
        reciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    BluetoothDevice device  = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    listAdapter.add(device.getName() + "\n" + device.getAddress());

                    devices.add(device);
                    String s = "";
                    for(int a = 0; a < pairedDevices.size(); a++)
                    {
                        if(pairedDevices != null && pairedDevices.get(a) != null && device.getName().equals(pairedDevices.get(a)))
                        {
                            s = "(paired)";
                            break;
                        }
                    }

                    listAdapter.add(device.getName() + " " + s + " " + "\n" + device.getAddress());


                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                {
                    //run some code
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //run some code


                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
                {
                    //run some code
                    if(btAdapter.getState() == btAdapter.STATE_OFF)
                    {
                        turnOnBT();
                    }
                }
            }
        };

        registerReceiver(reciever, filter);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(reciever, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(reciever, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(reciever, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED)
        {
            Toast.makeText(getApplicationContext(), "Bluetooth must be enables", Toast.LENGTH_SHORT);
            finish();
        }
    }

    public void getPairedDevices() {

        devicesArray = btAdapter.getBondedDevices();
        if(devicesArray.size() > 0)
        {
            for(BluetoothDevice device:devicesArray)
            {
                pairedDevices.add(device.getName());
            }
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(reciever);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if(btAdapter.isDiscovering())
        {
            btAdapter.cancelDiscovery();
        }
        if(listAdapter.getItem(position).contains("Paired"))
        {
            BluetoothDevice selectedDevice = devices.get(position);
            //Object[] o = devicesArray.toArray();
            //BluetoothDevice selectedDevice = (BluetoothDevice)o[position];
            ConnectThread connect = new ConnectThread(selectedDevice);
            connect.start();
            Toast.makeText(getApplicationContext(), "device is paired", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "device isn't paired", Toast.LENGTH_SHORT).show();

        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)

            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        private void manageConnectedSocket(BluetoothSocket mmSocket2)
        {

        }


        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }


    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    buffer = new byte[1024];
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


}
