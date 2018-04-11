package com.example.michele.guidasicuro;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.widget.Toast;


import com.google.android.gms.common.api.Status;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements UserScore.UserScoreListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_USERNAME = 1;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int mBreakReminderInterval = 7200000;
    private Handler mHandler = new Handler();
    private Runnable mBreakReminder;
    private BroadcastReceiver mMyReceiver;
    private UserScore mUserScore;
    private String mUsername;

    @Override
    public void onDangerousTime() {
        mUserScore.setIsDangerousTime(true);
    }

    @Override
    public void onHardBraking() {
        mUserScore.setHardBrakingCount(mUserScore.getHardBrakingCount() + 1);
    }

    @Override
    public void onSpeedLimitExceeded() {
        mUserScore.setSpeedLimitExceededCount(mUserScore.getSpeedLimitExceededCount() + 1);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
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

                mUserScore = new UserScore();

                // BroadcastReceiver
                mMyReceiver = new MyReceiver();

                // Register BroadcastReceiver to receive the data from the service
                LocalBroadcastManager.getInstance(this).registerReceiver(
                        mMyReceiver, new IntentFilter("setLocationSettings"));

                //Start the service to set the location settings
                Intent intent = new Intent(this, MyLocationService.class);
                this.startService(intent);

                BottomNavigationView bottomNavigationView = (BottomNavigationView)
                        findViewById(R.id.bottom_navigation);

                bottomNavigationView.setOnNavigationItemSelectedListener
                        (new BottomNavigationView.OnNavigationItemSelectedListener() {
                            @Override
                            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                                Fragment selectedFragment = null;
                                switch (item.getItemId()) {
                                    case R.id.action_item1:
                                        selectedFragment = MapFragment.newInstance();
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
                transaction.replace(R.id.frame_layout, MapFragment.newInstance());
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
