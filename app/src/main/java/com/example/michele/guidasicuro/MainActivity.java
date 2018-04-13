package com.example.michele.guidasicuro;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

// TODO: execute the requests to the Openstreetmap API and pass data to MapFragment

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_USERNAME = 1;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int mBreakReminderInterval = 7200000;
    private Handler mHandler = new Handler();
    private Runnable mBreakReminder;
    private BroadcastReceiver mMyReceiver;
    private UserScore mUserScore;
    private String mUsername;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 10;
    private String mDeviceAddress;

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                mDeviceAddress = device.getAddress(); // MAC address
                // TODO save deviceAddress

                // Register BroadcastReceiver to receive the data from the service
                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                        mMyReceiver, new IntentFilter("setLocationSettings"));

                //Start the service to set the location settings
                getApplication().startService(new Intent(getApplicationContext(), MyLocationService.class));

                new ConnectThread(device).run();
            } else {
                Bundle b = intent.getBundleExtra("Status");
                Status status = b.getParcelable("Status");
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(
                            MainActivity.this, 1000);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "Couldn't start the intent");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1000) {
            if(resultCode == RESULT_OK) {
                Log.i(TAG, "Result ok");
                // Stop register the BroadcastReceiver
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyReceiver);
                //Start the service for location updates
                Intent intent = new Intent(this, MyLocationService.class);
                this.startService(intent);
            }
        } else if(requestCode == REQUEST_USERNAME) {
            if(resultCode == RESULT_OK) {

                mUsername = data.getStringExtra("username");

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // If the device is running Android 6.0 or higher, and your app's target SDK is 23 or higher,
                // the app has to list the permissions in the manifest and request those permissions at run time
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                }

                // BroadcastReceiver
                mMyReceiver = new MyReceiver();

                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Log.i(TAG, "The device doesn't support Bluetooth");
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        queryPairedDevices();
                    }
                }

                mUserScore = new UserScore();

                BottomNavigationView bottomNavigationView = (BottomNavigationView)
                        findViewById(R.id.bottom_navigation);

                bottomNavigationView.setOnNavigationItemSelectedListener
                        (new BottomNavigationView.OnNavigationItemSelectedListener() {
                            @Override
                            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                                Fragment selectedFragment = null;
                                switch (item.getItemId()) {
                                    case R.id.action_item1:
                                        selectedFragment = MapFragment.newInstance(mUsername);
                                        break;
                                    case R.id.action_item2:
                                        selectedFragment = WeatherFragment.newInstance();
                                        break;
                                    case R.id.action_item3:
                                        selectedFragment = CarFragment.newInstance();
                                        break;
                                }
                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.frame_layout, selectedFragment);
                                transaction.addToBackStack(null);
                                transaction.commit();
                                return true;
                            }
                        });

                //Manually displaying the first fragment - one time only
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frame_layout, MapFragment.newInstance(mUsername));
                transaction.commit();

                // Runnable scheduled to remind the user to stop for a break
                mBreakReminder = new Runnable(){
                    public void run() {
                        Toast.makeText(getApplicationContext(), "You need to stop for a break!", Toast.LENGTH_LONG).show();
                        mHandler.postDelayed(this, mBreakReminderInterval);
                    }
                };
                mHandler.postDelayed(mBreakReminder, mBreakReminderInterval);
            }
        } else  if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Bluetooth enabled");
                queryPairedDevices();

            } else if(resultCode == RESULT_CANCELED) {
                Log.i(TAG, "Bluetooth not enabled");
                // Register BroadcastReceiver to receive the data from the service
                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                        mMyReceiver, new IntentFilter("setLocationSettings"));

                //Start the service to set the location settings
                Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
                getApplication().startService(intent);
            }
        }
    }

    private void queryPairedDevices() {
        ArrayList<String> deviceStrs = new ArrayList<String>();
        final ArrayList<String> devices = new ArrayList<String>();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }

            // show list
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice,
                    deviceStrs.toArray(new String[deviceStrs.size()]));

            alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    mDeviceAddress = devices.get(position);
                    // TODO save deviceAddress

                    new ConnectThread((BluetoothDevice)pairedDevices.toArray()[position]).run();

                    // Register BroadcastReceiver to receive the data from the service
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                            mMyReceiver, new IntentFilter("setLocationSettings"));

                    //Start the service to set the location settings
                    Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
                    getApplication().startService(intent);
                }
            });

            alertDialog.setTitle("Choose Bluetooth device");
            alertDialog.show();
        } else {
            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            LocalBroadcastManager.getInstance(this).registerReceiver(mMyReceiver, filter);

            // Register BroadcastReceiver to receive the data from the service
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    mMyReceiver, new IntentFilter("setLocationSettings"));

            //Start the service to set the location settings
            Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
            getApplication().startService(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            // Hide the action bar
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
        }

        Intent i = new Intent(this, LoginActivity.class);
        startActivityForResult(i, REQUEST_USERNAME);
    }

    public void setUserScoreListener(UserScore.UserScoreListener userScoreListener) {
        mUserScore.setUserScoreListener(userScoreListener);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            new ConnectedThread(mmSocket).run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
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

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                // OBD2 initialization
                new EchoOffCommand().run(mmInStream, mmOutStream);
                new LineFeedOffCommand().run(mmInStream, mmOutStream);
                new TimeoutCommand(200).run(mmInStream, mmOutStream);
                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmInStream, mmOutStream);

                SpeedCommand speedCommand = new SpeedCommand();
                int measuresNumber = 0;
                int[] speedMeasures = new int[3];

                while(!Thread.currentThread().isInterrupted()) {
                    speedCommand.run(mmInStream, mmOutStream);
                    Log.i(TAG,"Speed: " + speedCommand.getFormattedResult());
                    speedMeasures[measuresNumber] = speedCommand.getMetricSpeed();
                    measuresNumber ++;

                    if(measuresNumber >= 3) {
                        mUserScore.checkHardBraking(speedMeasures);
                        measuresNumber = 0;
                    }
                }
            } catch(Exception e) {
                Log.i(TAG, "Error occurred when retrieving data from the ELM327 device");
                this.cancel();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO: upload data to the server
        Log.i(TAG, "onStop");
        super.onStop();
        mHandler.removeCallbacks(mBreakReminder);

        // Stop register the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyReceiver);
        //Stop the service
        Intent intent = new Intent(this, MyLocationService.class);
        stopService(intent);
    }

    private void checkLocationPermission() {
        // Check if the Location permission has been granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // TODO: Prompt with explanation!

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
