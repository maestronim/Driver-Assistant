package com.example.michele.guidasicuro;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.commands.fuel.WidebandAirFuelRatioCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.google.android.gms.common.api.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_USERNAME = 1;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int mBreakReminderInterval = 7200000;
    private Handler mHandler = new Handler();
    private Runnable mBreakReminder;
    private BroadcastReceiver mMyReceiver;
    private UserScore mUserScore;
    private UserScore mOriginalUserScore;
    private String mUsername;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 10;
    private String mDeviceAddress;
    private Location mLocation;
    private RoadInfo mRoadInfo;
    private String mPreviousRoadID;
    private boolean mShowActionBar;
    private List<Location> mCoordinates;
    private Runnable mUploadToServer;
    private String mPathDate;
    private Chronometer mChronometer;
    private CarParameter mCarParameter;
    private ArrayList<CarParameter> mCarParameterArrayList = new ArrayList<>();
    private Lock mLock = new ReentrantLock();
    private boolean mIsTimeDangerousChecked = false;
    private boolean mIsReceiverUnregistered;
    private CarFaultCodes mCarFaultCodes;

    private void saveDeviceAddress(Context context, BluetoothDevice bluetoothDevice) {
        try {
            FileOutputStream outputStream = context.openFileOutput("deviceAddress", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            objectOutputStream.writeObject(bluetoothDevice);

            objectOutputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BluetoothDevice getDeviceAddress(Context context) {
        try {
            FileInputStream fileInputStream = context.openFileInput("deviceAddress");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            BluetoothDevice bluetoothDevice = (BluetoothDevice) objectInputStream.readObject();

            objectInputStream.close();
            fileInputStream.close();

            return bluetoothDevice;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            String action = intent.getAction();
            Log.i(TAG, "Action: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                saveDeviceAddress(getApplicationContext(), device);

                new ConnectThread(device).run();
            } else if(intent.getAction().equals("setLocationSettings")){
                Bundle b = intent.getBundleExtra("LocationSettings");
                boolean isGPSEnabled = b.getBoolean("isGPSEnabled");

                if(!isGPSEnabled) {
                    Status status = b.getParcelable("Status");
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(
                                MainActivity.this, 1000);
                    } catch (IntentSender.SendIntentException e) {
                        Log.i(TAG, "Couldn't start the intent");
                    }
                } else {
                    // Register BroadcastReceiver to receive the data from the service
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                            mMyReceiver, new IntentFilter("GPSLocationUpdates"));

                    //Start the service for location updates
                    Intent i = new Intent(getApplicationContext(), MyLocationService.class);
                    getApplication().startService(i);
                }
            } else if(intent.getAction().equals("GPSLocationUpdates")) {
                Bundle b = intent.getBundleExtra("Location");
                mLocation = (Location) b.getParcelable("Location");

                if(mCoordinates.size() > 0) {
                    if(!mCoordinates.get(mCoordinates.size() - 1).equals(mLocation)) {
                        mCoordinates.add(mLocation);
                    }
                } else {
                    mCoordinates.add(mLocation);
                }

                Log.i(TAG, "init");

                String url = "https://nominatim.openstreetmap.org/reverse?email=michelemaestroni9@gmail.com&format=json&lat=" + mLocation.getLatitude() + "&lon=" + mLocation.getLongitude() + "&zoom=16";
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    mRoadInfo.setID(response.getString("osm_id"));
                                    mRoadInfo.setType(response.getString("osm_type"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                boolean isSameRoad = false;

                                if(mPreviousRoadID != null) {
                                    if (mRoadInfo.getID().equals(mPreviousRoadID)) {
                                        isSameRoad = true;
                                    }
                                }

                                if(!isSameRoad) {
                                    mPreviousRoadID = mRoadInfo.getID();

                                    Log.i(TAG, "not same road");

                                    String url = "http://overpass-api.de/api/interpreter?data=[out:json];" + mRoadInfo.getType() + "(" + mRoadInfo.getID() + ");out;";
                                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                                            new Response.Listener<JSONObject>() {
                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    boolean isARoad = false;
                                                    boolean maxSpeedIsDefined = false;
                                                    try {
                                                        JSONObject tags = response.getJSONArray("elements").getJSONObject(0).getJSONObject("tags");
                                                        Log.i(TAG, "Tags: " + tags);
                                                        // Get road info
                                                        if (mRoadInfo.getType().equals("way")) {
                                                            if (tags.has("highway") || tags.has("junction")) {
                                                                isARoad = true;
                                                            }
                                                        } else if (mRoadInfo.getType().equals("area")) {
                                                            if ((tags.has("highway") && tags.has("area"))) {
                                                                if (tags.getString("area").equals("yes")) {
                                                                    isARoad = true;
                                                                }
                                                            }
                                                        }

                                                        if (isARoad) {
                                                            mRoadInfo.setHighway(tags.getString("highway"));
                                                            if (mRoadInfo.getHighway().equals("motorway")) {
                                                                mRoadInfo.setMaxSpeed(130);
                                                                maxSpeedIsDefined = true;

                                                            } else if (mRoadInfo.getHighway().equals("residential")) {
                                                                mRoadInfo.setMaxSpeed(50);
                                                                maxSpeedIsDefined = true;
                                                            }

                                                            if (tags.has("name")) {
                                                                mRoadInfo.setName(tags.getString("name"));
                                                            }

                                                            if (tags.has("maxspeed")) {
                                                                mRoadInfo.setMaxSpeed(Integer.parseInt(tags.getString("maxspeed")));
                                                                maxSpeedIsDefined = true;
                                                            }
                                                        }
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    if (!maxSpeedIsDefined) {
                                                        BoundingBox boundingBox = new BoundingBox();
                                                        // TODO: Distance needs to be defined
                                                        boundingBox.calculate(mLocation, 500);

                                                        String url = "http://overpass.osm.rambler.ru/cgi/interpreter?data=[out:json];way[highway=residential](" +
                                                                boundingBox.getSouthernLimit() + "," +
                                                                boundingBox.getWesternLimit() + "," +
                                                                boundingBox.getNorthernLimit() + "," +
                                                                boundingBox.getEasternLimit() + ");out;";

                                                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                                                                new Response.Listener<JSONObject>() {
                                                                    @Override
                                                                    public void onResponse(JSONObject response) {
                                                                        try {
                                                                            // The road is urban if there are residential roads within the distance
                                                                            if (response.getJSONArray("elements").length() > 0) {
                                                                                Log.i(TAG, "The road is urban");
                                                                                switch (mRoadInfo.getHighway()) {
                                                                                    case "trunk":
                                                                                        mRoadInfo.setMaxSpeed(70);
                                                                                        break;
                                                                                    case "primary":
                                                                                        mRoadInfo.setMaxSpeed(50);
                                                                                        break;
                                                                                    case "secondary":
                                                                                        mRoadInfo.setMaxSpeed(50);
                                                                                        break;
                                                                                    case "tertiary":
                                                                                        mRoadInfo.setMaxSpeed(50);
                                                                                        break;
                                                                                    case "unclassified":
                                                                                        mRoadInfo.setMaxSpeed(50);
                                                                                        break;
                                                                                }
                                                                            } else {
                                                                                Log.i(TAG, "The road is interurban");
                                                                                switch (mRoadInfo.getHighway()) {
                                                                                    case "trunk":
                                                                                        mRoadInfo.setMaxSpeed(110);
                                                                                        break;
                                                                                    case "primary":
                                                                                        mRoadInfo.setMaxSpeed(90);
                                                                                        break;
                                                                                    case "secondary":
                                                                                        mRoadInfo.setMaxSpeed(90);
                                                                                        break;
                                                                                    case "tertiary":
                                                                                        mRoadInfo.setMaxSpeed(90);
                                                                                        break;
                                                                                    case "unclassified":
                                                                                        mRoadInfo.setMaxSpeed(70);
                                                                                        break;
                                                                                }
                                                                            }
                                                                        } catch (JSONException e) {
                                                                            e.printStackTrace();
                                                                        } catch (Exception e) {
                                                                            e.printStackTrace();
                                                                        }

                                                                        Log.i(TAG, "end");

                                                                        mRoadInfo.getRoadInfoListener().onRoadChanged(mRoadInfo);
                                                                    }
                                                                }, new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                error.printStackTrace();
                                                                Toast.makeText(getApplicationContext(), "An error occurred in retrieving the road info", Toast.LENGTH_LONG).show();
                                                            }
                                                        });

                                                        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                                                    }
                                                }
                                            }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            error.printStackTrace();
                                            Toast.makeText(getApplicationContext(), "An error occurred in retrieving the road info", Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), "An error occurred in retrieving the road info", Toast.LENGTH_LONG).show();
                    }
                });

                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1000) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this, "GPS enabled", Toast.LENGTH_LONG).show();
            } else if(resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "GPS not enabled", Toast.LENGTH_LONG).show();
            }

            // Register BroadcastReceiver to receive the data from the service
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    mMyReceiver, new IntentFilter("GPSLocationUpdates"));

            //Start the service for location updates
            Intent intent = new Intent(this, MyLocationService.class);
            this.startService(intent);
        } else if(requestCode == REQUEST_USERNAME) {
            if(resultCode == RESULT_OK) {

                mUsername = data.getStringExtra("username");

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // If the device is running Android 6.0 or higher, and your app's target SDK is 23 or higher,
                // the app has to list the permissions in the manifest and request those permissions at run time
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                }

                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Log.i(TAG, "The device doesn't support Bluetooth");
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        BluetoothDevice bluetoothDevice = getDeviceAddress(getApplicationContext());
                        if(bluetoothDevice != null) {
                            new ConnectThread(bluetoothDevice).run();
                        } else {
                            queryPairedDevices();
                        }
                    }
                }

                BottomNavigationView bottomNavigationView = (BottomNavigationView)
                        findViewById(R.id.bottom_navigation);

                bottomNavigationView.setOnNavigationItemSelectedListener
                        (new BottomNavigationView.OnNavigationItemSelectedListener() {
                            @Override
                            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                                Fragment selectedFragment = null;
                                switch (item.getItemId()) {
                                    case R.id.action_item1:
                                        mShowActionBar = false;
                                        selectedFragment = MapFragment.newInstance(mUserScore);
                                        break;
                                    case R.id.action_item2:
                                        mShowActionBar = false;
                                        selectedFragment = WeatherFragment.newInstance();
                                        break;
                                    case R.id.action_item3:
                                        mShowActionBar = true;
                                        selectedFragment = CarFragment.newInstance();
                                        break;
                                }

                                if(!mShowActionBar) {
                                    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                                    }
                                    else {
                                        // Hide the action bar
                                        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                                        actionBar.hide();
                                    }
                                } else {
                                    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                                                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                                    }
                                    else {
                                        // Show the action bar
                                        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                                        actionBar.show();
                                    }
                                }

                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.frame_layout, selectedFragment);
                                transaction.addToBackStack(null);
                                transaction.commit();
                                return true;
                            }
                        });

                bottomNavigationView.setSelectedItemId(R.id.action_item1);
            }
        } else  if(requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Bluetooth enabled");
                BluetoothDevice bluetoothDevice = getDeviceAddress(getApplicationContext());
                if(bluetoothDevice != null) {
                    new ConnectThread(bluetoothDevice).run();
                } else {
                    queryPairedDevices();
                }
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

    private void createPath() {
        mPathDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());

        String url = "http://maestronim.altervista.org/Driver-Assistant/api/user-path/create.php";
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject();
            jsonObject.put("user_id", mUsername);
            jsonObject.put("path_date", mPathDate);
        } catch(JSONException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (url, jsonObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getString("success").equals("no")) {
                                Toast.makeText(getApplicationContext(), "An error occurred in creating the path", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), response.getString("message"), Toast.LENGTH_LONG).show();
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getApplicationContext(), "An error occurred in creating the path", Toast.LENGTH_LONG).show();
                    }
                });

        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void updatePath() {
        if(mCoordinates.size() > 0) {
            long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
            String formattedTime = getDate(elapsedMillis, "hh:mm:ss");

            String url = "http://maestronim.altervista.org/Driver-Assistant/api/user-path/update.php";
            JSONObject jsonObject = null;

            try {
                jsonObject = new JSONObject();
                jsonObject.put("user_id", mUsername);
                jsonObject.put("path_date", mPathDate);
                jsonObject.put("dangerous_time", mUserScore.getDangerousTimeCount());
                jsonObject.put("speed_limit_exceeded", mUserScore.getSpeedLimitExceededCount());
                jsonObject.put("hard_braking", mUserScore.getHardBrakingCount());
                jsonObject.put("duration", formattedTime);
                JSONArray coordinates = new JSONArray();
                for(Location l : mCoordinates) {
                    JSONArray coord = new JSONArray();
                    coord.put(l.getLatitude());
                    coord.put(l.getLongitude());
                    coordinates.put(coord);
                }
                jsonObject.put("coordinates", coordinates);
            } catch(JSONException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (url, jsonObject, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response.getString("success").equals("no")) {
                                    Toast.makeText(getApplicationContext(), "An error occurred in updating the path", Toast.LENGTH_LONG).show();
                                }
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(getApplicationContext(), "An error occurred in updating the path", Toast.LENGTH_LONG).show();
                        }
                    });

            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

            mCoordinates.clear();
        }
    }

    private void createCarParameters() {
        mLock.lock();
        if(mCarParameterArrayList.size() > 0) {
            String url = "http://maestronim.altervista.org/Driver-Assistant/api/car-parameters/create.php";
            JSONObject jsonObject = null;

            try {
                jsonObject = new JSONObject();
                jsonObject.put("user_id", mUsername);
                jsonObject.put("path_date", mPathDate);
                jsonObject.put("speed", mCarParameterArrayList.get(0).getValue());
                jsonObject.put("RPM", mCarParameterArrayList.get(1).getValue());
                jsonObject.put("absoluteLoad", mCarParameterArrayList.get(2).getValue());
                jsonObject.put("load", mCarParameterArrayList.get(3).getValue());
                jsonObject.put("massAirFlow", mCarParameterArrayList.get(4).getValue());
                jsonObject.put("oilTemperature", mCarParameterArrayList.get(5).getValue());
                jsonObject.put("throttlePosition", mCarParameterArrayList.get(6).getValue());
                jsonObject.put("airFuelRatio", mCarParameterArrayList.get(7).getValue());
                jsonObject.put("consumptionRate", mCarParameterArrayList.get(8).getValue());
                jsonObject.put("fuelLevel", mCarParameterArrayList.get(9).getValue());
                jsonObject.put("fuelTrim", mCarParameterArrayList.get(10).getValue());
                jsonObject.put("widebandAirFuelRatio", mCarParameterArrayList.get(11).getValue());
                jsonObject.put("barometricPressure", mCarParameterArrayList.get(12).getValue());
                jsonObject.put("fuelPressure", mCarParameterArrayList.get(13).getValue());
                jsonObject.put("fuelRailPressure", mCarParameterArrayList.get(14).getValue());
                jsonObject.put("intakeManifoldPressure", mCarParameterArrayList.get(15).getValue());
                jsonObject.put("airIntakePressure", mCarParameterArrayList.get(16).getValue());
                jsonObject.put("ambientAirTemperature", mCarParameterArrayList.get(17).getValue());
                jsonObject.put("engineCoolantTemperature", mCarParameterArrayList.get(18).getValue());
                mLock.unlock();
            } catch(JSONException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (url, jsonObject, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response.getString("success").equals("no")) {
                                    Toast.makeText(getApplicationContext(), "An error occurred in uploading the car's parameters to the server", Toast.LENGTH_LONG).show();
                                }
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(getApplicationContext(), "An error occurred in uploading the car's parameters to the server", Toast.LENGTH_LONG).show();
                        }
                    });

            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    private String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
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

                    saveDeviceAddress(getApplicationContext(), (BluetoothDevice)pairedDevices.toArray()[position]);

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

        // BroadcastReceiver
        mMyReceiver = new MyReceiver();

        mUserScore = new UserScore();
        mRoadInfo = new RoadInfo();
        mCarParameter = new CarParameter();
        mCarFaultCodes = new CarFaultCodes();

        mPreviousRoadID = null;

        mCoordinates = new ArrayList<Location>();

        mIsReceiverUnregistered = false;

        // Runnable scheduled to remind the user to stop for a break
        mBreakReminder = new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "You need to stop for a break!", Toast.LENGTH_LONG).show();
                mHandler.postDelayed(this, mBreakReminderInterval);
            }
        };

        // Runnable scheduled to upload data to the server
        mUploadToServer = new Runnable() {
            @Override
            public void run() {
                updatePath();
                createCarParameters();
                mHandler.postDelayed(mUploadToServer, 10000);
            }
        };

        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_USERNAME);
    }

    public void setUserScoreListener(UserScore.UserScoreListener userScoreListener) {
        mUserScore.setUserScoreListener(userScoreListener);
    }

    public void setRoadInfoListener(RoadInfo.RoadInfoListener roadInfoListener) {
        mRoadInfo.setRoadInfoListener(roadInfoListener);
    }

    public void setCarParametersListener(CarParameter.CarParametersListener carParametersListener) {
        mCarParameter.setCarParametersListener(carParametersListener);
    }

    public void setCarFaultCodesListener(CarFaultCodes.CarFaultCodesListener carFaultCodesListener) {
        mCarFaultCodes.setCarFaultCodesListener(carFaultCodesListener);
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
                Log.i(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "ConnectThread");
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this,
                    ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Connecting to " + mmDevice.getName() + "...");
            progressDialog.show();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Unable to connect to " + mmDevice.getName(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "Unable to connect");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.i(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Connected to " + mmDevice.getName(), Toast.LENGTH_LONG).show();
            new ConnectedThread(mmSocket).run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.i(TAG, "Could not close the client socket", e);
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
                Log.i(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.i(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                // path creation
                createPath();
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
                mHandler.postDelayed(mUploadToServer, 10000);
                mHandler.postDelayed(mBreakReminder, mBreakReminderInterval);

                // OBD2 initialization
                new EchoOffCommand().run(mmInStream, mmOutStream);
                new LineFeedOffCommand().run(mmInStream, mmOutStream);
                new TimeoutCommand(200).run(mmInStream, mmOutStream);
                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmInStream, mmOutStream);

                TroubleCodesCommand troubleCodesCommand = new TroubleCodesCommand();

                SpeedCommand speedCommand = new SpeedCommand();
                RPMCommand rpmCommand = new RPMCommand();
                AbsoluteLoadCommand absoluteLoadCommand = new AbsoluteLoadCommand();
                LoadCommand loadCommand = new LoadCommand();
                MassAirFlowCommand massAirFlowCommand = new MassAirFlowCommand();
                OilTempCommand oilTempCommand = new OilTempCommand();
                ThrottlePositionCommand throttlePositionCommand = new ThrottlePositionCommand();
                AirFuelRatioCommand airFuelRatioCommand = new AirFuelRatioCommand();
                ConsumptionRateCommand consumptionRateCommand = new ConsumptionRateCommand();
                FuelLevelCommand fuelLevelCommand = new FuelLevelCommand();
                FuelTrimCommand fuelTrimCommand = new FuelTrimCommand();
                WidebandAirFuelRatioCommand widebandAirFuelRatioCommand = new WidebandAirFuelRatioCommand();
                BarometricPressureCommand barometricPressureCommand = new BarometricPressureCommand();
                FuelPressureCommand fuelPressureCommand = new FuelPressureCommand();
                FuelRailPressureCommand fuelRailPressureCommand = new FuelRailPressureCommand();
                IntakeManifoldPressureCommand intakeManifoldPressureCommand = new IntakeManifoldPressureCommand();
                AirIntakeTemperatureCommand airIntakeTemperatureCommand = new AirIntakeTemperatureCommand();
                AmbientAirTemperatureCommand ambientAirTemperatureCommand = new AmbientAirTemperatureCommand();
                EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = new EngineCoolantTemperatureCommand();

                int measuresNumber = 0;
                int[] speedMeasures = new int[3];
                ArrayList<CarParameter> carParameterArrayList = new ArrayList<>();

                troubleCodesCommand.run(mmInStream, mmOutStream);
                mCarFaultCodes.getCarFaultCodesListener().onCarFaultCodesChanged(troubleCodesCommand.getFormattedResult());

                while(!Thread.currentThread().isInterrupted()) {
                    speedCommand.run(mmInStream, mmOutStream);
                    rpmCommand.run(mmInStream, mmOutStream);
                    absoluteLoadCommand.run(mmInStream, mmOutStream);
                    loadCommand.run(mmInStream, mmOutStream);
                    massAirFlowCommand.run(mmInStream, mmOutStream);
                    oilTempCommand.run(mmInStream, mmOutStream);
                    throttlePositionCommand.run(mmInStream, mmOutStream);
                    airFuelRatioCommand.run(mmInStream, mmOutStream);
                    consumptionRateCommand.run(mmInStream, mmOutStream);
                    fuelLevelCommand.run(mmInStream, mmOutStream);
                    fuelTrimCommand.run(mmInStream, mmOutStream);
                    widebandAirFuelRatioCommand.run(mmInStream, mmOutStream);
                    barometricPressureCommand.run(mmInStream, mmOutStream);
                    fuelPressureCommand.run(mmInStream, mmOutStream);
                    fuelRailPressureCommand.run(mmInStream, mmOutStream);
                    intakeManifoldPressureCommand.run(mmInStream, mmOutStream);
                    airIntakeTemperatureCommand.run(mmInStream, mmOutStream);
                    ambientAirTemperatureCommand.run(mmInStream, mmOutStream);
                    engineCoolantTemperatureCommand.run(mmInStream, mmOutStream);

                    //TODO: Max values needs to be defined
                    carParameterArrayList.add(new CarParameter("Speed", speedCommand.getCalculatedResult(), speedCommand.getResultUnit(), 220));
                    carParameterArrayList.add(new CarParameter("RPM", rpmCommand.getCalculatedResult(), rpmCommand.getResultUnit(), 10000));
                    carParameterArrayList.add(new CarParameter("Absolute Load", absoluteLoadCommand.getCalculatedResult(), absoluteLoadCommand.getResultUnit(), 100));
                    carParameterArrayList.add(new CarParameter("Load", loadCommand.getCalculatedResult(), loadCommand.getResultUnit(), 100));
                    carParameterArrayList.add(new CarParameter("Mass Air Flow", massAirFlowCommand.getCalculatedResult(), massAirFlowCommand.getResultUnit(), 200));
                    carParameterArrayList.add(new CarParameter("Oil Temperature", oilTempCommand.getCalculatedResult(), oilTempCommand.getResultUnit(), 150));
                    carParameterArrayList.add(new CarParameter("Throttle Position", throttlePositionCommand.getCalculatedResult(), throttlePositionCommand.getResultUnit(), 100));
                    carParameterArrayList.add(new CarParameter("Air Fuel Ratio", airFuelRatioCommand.getCalculatedResult(), airFuelRatioCommand.getResultUnit(), 18));
                    carParameterArrayList.add(new CarParameter("Consumption Rate", consumptionRateCommand.getCalculatedResult(), consumptionRateCommand.getResultUnit(), 10));
                    carParameterArrayList.add(new CarParameter("Fuel Level", fuelLevelCommand.getCalculatedResult(), fuelLevelCommand.getResultUnit(), 100));
                    carParameterArrayList.add(new CarParameter("Fuel Trim", fuelTrimCommand.getCalculatedResult(), fuelTrimCommand.getResultUnit(), 100));
                    carParameterArrayList.add(new CarParameter("Wideband Air Fuel Ratio", widebandAirFuelRatioCommand.getCalculatedResult(), widebandAirFuelRatioCommand.getResultUnit(), 20));
                    carParameterArrayList.add(new CarParameter("Barometric Pressure", barometricPressureCommand.getCalculatedResult(), barometricPressureCommand.getResultUnit(), 792));
                    carParameterArrayList.add(new CarParameter("Fuel Pressure", fuelPressureCommand.getCalculatedResult(), fuelPressureCommand.getResultUnit(), 689));
                    carParameterArrayList.add(new CarParameter("Fuel Rail Pressure", fuelRailPressureCommand.getCalculatedResult(), fuelRailPressureCommand.getResultUnit(), 206));
                    carParameterArrayList.add(new CarParameter("Intake Manifold Pressure", intakeManifoldPressureCommand.getCalculatedResult(), intakeManifoldPressureCommand.getResultUnit(), 20));
                    carParameterArrayList.add(new CarParameter("Air Intake Temperature", airIntakeTemperatureCommand.getCalculatedResult(), airIntakeTemperatureCommand.getResultUnit(), 150));
                    carParameterArrayList.add(new CarParameter("Ambient Air Temperature", ambientAirTemperatureCommand.getCalculatedResult(), ambientAirTemperatureCommand.getResultUnit(), 50));
                    carParameterArrayList.add(new CarParameter("Engine Coolant Temperature", engineCoolantTemperatureCommand.getCalculatedResult(), engineCoolantTemperatureCommand.getResultUnit(), 120));

                    mCarParameter.getCarParametersListener().onCarParametersChanged(carParameterArrayList);

                    if(mLock.tryLock()) {
                        mCarParameterArrayList.clear();
                        for(int i=0;i<carParameterArrayList.size();i++) {
                            mCarParameterArrayList.add(carParameterArrayList.get(i));
                        }

                        mLock.unlock();
                    }
                    carParameterArrayList.clear();

                    speedMeasures[measuresNumber] = speedCommand.getMetricSpeed();
                    measuresNumber ++;
                    
                    if(!mIsTimeDangerousChecked) {
                        if (mUserScore.checkDangerousTime()) {
                            mIsTimeDangerousChecked = true;
                        }
                    }

                    if(mRoadInfo.getMaxSpeed() != 0) {
                        mUserScore.checkSpeedLimitExceeded(speedCommand.getMetricSpeed(), mRoadInfo.getMaxSpeed());
                    }

                    if(measuresNumber >= 3) {
                        mUserScore.checkHardBraking(speedMeasures);
                        measuresNumber = 0;
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
                Log.i(TAG, "Error occurred when retrieving data from the ELM327 device");
                //this.cancel();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.i(TAG, "Could not close the connect socket", e);
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

        if(mIsReceiverUnregistered) {
            // Register BroadcastReceiver to receive the data from the service
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    mMyReceiver, new IntentFilter("GPSLocationUpdates"));
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        mHandler.removeCallbacks(mBreakReminder);

        // Stop register the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyReceiver);

        mIsReceiverUnregistered = true;
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
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
