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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int mBreakReminderInterval = 7200000;
    private Handler mHandler = new Handler();
    private Runnable mBreakReminder;
    private FragmentPagerAdapter mAdapterViewPager;
    private BroadcastReceiver mMyReceiver;

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
        }
    }

    public static class MyPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 3;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            Log.i(TAG, " Current item: " + String.valueOf(position));
            switch (position) {
                case 0: // Fragment # 0 - This will show MapFragment
                    return MapFragment.newInstance();
                case 1: // Fragment # 1 - This will show WeatherFragment
                    return WeatherFragment.newInstance();
                case 2: // Fragment # 2 - This will show CarFragment
                    return CarFragment.newInstance();
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            // Hide the action bar
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
        }

        // If the device is running Android 6.0 or higher, and your app's target SDK is 23 or higher,
        // the app has to list the permissions in the manifest and request those permissions at run time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // BroadcastReceiver
        mMyReceiver = new MyReceiver();

        // Register BroadcastReceiver to receive the data from the service
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMyReceiver, new IntentFilter("setLocationSettings"));

        //Start the service to set the location settings
        Intent intent = new Intent(this, MyLocationService.class);
        this.startService(intent);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        mAdapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mAdapterViewPager);
        viewPager.setOffscreenPageLimit(3);

        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_item1:
                                viewPager.setCurrentItem(0);
                                break;
                            case R.id.action_item2:
                                viewPager.setCurrentItem(1);
                                break;
                            case R.id.action_item3:
                                viewPager.setCurrentItem(2);
                                break;
                        }
                        return true;
                    }
                });

        // Runnable scheduled to remind the user to stop for a break
        mBreakReminder = new Runnable(){
            public void run() {
                Toast.makeText(getApplicationContext(), "You need to stop for a break!", Toast.LENGTH_LONG).show();
                mHandler.postDelayed(this, mBreakReminderInterval);
            }
        };
        mHandler.postDelayed(mBreakReminder, mBreakReminderInterval);
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
