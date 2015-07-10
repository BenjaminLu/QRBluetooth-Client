package com.bluetooth.icollect.bluetooth_client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener
{
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> arrayAdapter;
    private Set<BluetoothDevice> bluetoothDevices;
    private boolean receiverIsRegistered = false;
    private TextView statusTextView;
    UUID uuid = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");
    String deviceName;
    String deviceAddress;
    Context context;
    private QRCodeReaderView qrcodeSannerView;

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            String message = msg.getData().getString("message");
            statusTextView.setText(message);
            qrcodeSannerView.getCameraManager().stopPreview();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        statusTextView.setText("Bluetooth off");
                        arrayAdapter.clear();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        statusTextView.setText("Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        showDevices();
                        statusTextView.setText("Bluetooth on");
                        bluetoothAdapter.startDiscovery();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        statusTextView.setText("Turning Bluetooth on...");
                        break;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int size = arrayAdapter.getCount();
                for(int i = 0 ; i < size ; i++) {
                    String row = arrayAdapter.getItem(i);
                    if(row.contains(device.getAddress())) {
                        return;
                    }
                }
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        if(qrcodeSannerView != null) {
            qrcodeSannerView.getCameraManager().startPreview();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(qrcodeSannerView != null) {
            qrcodeSannerView.getCameraManager().stopPreview();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(receiverIsRegistered) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        qrcodeSannerView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        qrcodeSannerView.setOnQRCodeReadListener(this);
        statusTextView = (TextView) findViewById(R.id.status);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            statusTextView.setText("Not support Bluetooth.");
            Toast.makeText(this, "Not support Bluetooth.", Toast.LENGTH_SHORT);
        } else {
            deviceName = bluetoothAdapter.getName();
            deviceAddress = bluetoothAdapter.getAddress();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
            receiverIsRegistered = true;
            if (bluetoothAdapter.isEnabled()) {
                statusTextView.setText("Bluetooth on");
                bluetoothAdapter.startDiscovery();
                showDevices();
            } else {
                statusTextView.setText("Bluetooth off");
                Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(turnOnIntent);
            }
        }
    }

    private void showDevices()
    {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                int size = arrayAdapter.getCount();
                for(int i = 0 ; i < size ; i++) {
                    String row = arrayAdapter.getItem(i);
                    if(row.contains(device.getAddress())) {
                        return;
                    }
                }
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onQRCodeRead(String s, PointF[] pointFs)
    {
        statusTextView.setText(s);
        if(BluetoothAdapter.checkBluetoothAddress(s)) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(s);
            new ConnectThread(device).start();
        }
    }

    @Override
    public void cameraNotFound()
    {

    }

    @Override
    public void QRCodeNotFoundOnCamImage()
    {

    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            qrcodeSannerView.getCameraManager().stopPreview();
            BluetoothSocket tmp = null;
            mmDevice = device;
            statusTextView.setText("Connecting...");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                mmSocket.connect();
                mmInStream = mmSocket.getInputStream();
                mmOutStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                qrcodeSannerView.getCameraManager().startPreview();
                e.printStackTrace();
            }
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            String outMessage = "From " + deviceName;
            try {
                if (mmSocket.isConnected()) {
                    mmOutStream.write(outMessage.getBytes());
                    statusTextView.setText("Message sent");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    if (mmSocket.isConnected()) {
                        // Read from the InputStream
                        bytes = mmInStream.read(buffer);
                        // Send the obtained bytes to the UI activity
                        String message = new String(buffer, "UTF-8");
                        Message m = new Message();
                        Bundle b = new Bundle();
                        b.putString("message", message);
                        m.setData(b);
                        mHandler.sendMessage(m);
                    }
                } catch (IOException e) {
                    break;
                }
            }

        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }
}
