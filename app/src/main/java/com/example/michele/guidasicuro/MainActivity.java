package com.example.michele.guidasicuro;

import android.Manifest;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.InputStream;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int mWeatherUpdateInterval = 600000;
    private static final int mBreakReminderInterval = 7200000;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private CameraPosition mCameraPosition;
    private Location mLocation;
    private Location mPreviousLocation;
    private Marker mMarker;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLocation = new Location("provider");

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

        ImageView headlights = (ImageView) findViewById(R.id.Headlights);
        headlights.setImageResource(R.drawable.headlights_off);

        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);

        if(currentHour >= 22 || currentHour <= 6) {
            //TODO: Prompt with custom Toast
            Toast.makeText(this, "Se hai bevuto non metterti alla guida!", Toast.LENGTH_LONG).show();
        }

        // Creating the fragment where the map is displayed
        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map_container, mMapFragment);
        fragmentTransaction.commit();

        mMapFragment.getMapAsync(this);

        // The main entry point for Google Play services integration
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Execute the task to download the road info
        new DownloadRoadInfo().execute(mLocation);

        // Runnable scheduled to download the weather info
        Runnable weatherUpdate = new Runnable(){
            public void run() {
                // Execute the task to download the weather info
                new DownloadWeatherInfo().execute(mLocation);
                Toast.makeText(getApplicationContext(), "Weather update", Toast.LENGTH_LONG).show();
                mHandler.postDelayed(this, mWeatherUpdateInterval);
            }
        };
        mHandler.post(weatherUpdate);

        ImageView breakImage = (ImageView) findViewById(R.id.Break);
        breakImage.setImageResource(R.drawable.no_break_needed);

        // Runnable scheduled to remind the user to stop for a break
        Runnable breakReminder = new Runnable(){
            public void run() {
                // Execute the task to download the weather info
                new DownloadWeatherInfo().execute(mLocation);
                Toast.makeText(getApplicationContext(), "You need to stop for a break!", Toast.LENGTH_LONG).show();
                //Set the image to remind the user to stop
                ImageView breakImage = (ImageView) findViewById(R.id.Break);
                breakImage.setColorFilter(Color.RED);
                mHandler.postDelayed(this, mBreakReminderInterval);
            }
        };
        mHandler.postDelayed(breakReminder, mBreakReminderInterval);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Closes the connection to Google Play services.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check if the Location permission has been granted
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Get the last known location
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            // Check for location updates
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

        // Place a marker on the map
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        mMarker = mMap.addMarker(markerOptions);

        // Construct a CameraPosition focusing on the user position and animate the camera to that position.
        mCameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))      // Sets the center of the map to user position
                .zoom(17)                   // Sets the zoom
                .bearing(0)                 // Sets the orientation of the camera to north
                .tilt(45)                   // Sets the tilt of the camera to 45 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

        // Save the previous location
        mPreviousLocation = mLocation;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        Log.i(TAG, "Bearing: " + Bearing.getBearing(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude(), mLocation.getLatitude(), mLocation.getLongitude()));
        // Change the marker position
        mMarker.setPosition(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        // Construct a CameraPosition focusing on the user position and animate the camera to that position.
        mCameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))      // Sets the center of the map to user position
                .zoom(17)                   // Sets the zoom
                .bearing((float)Bearing.getBearing(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude(), mLocation.getLatitude(), mLocation.getLongitude()))                 // Sets the orientation of the camera to north
                .tilt(45)                   // Sets the tilt of the camera to 45 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

        // Save the previous location
        mPreviousLocation = mLocation;

        // Execute the task to download the data
        new DownloadRoadInfo().execute(mLocation);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.getUiSettings().setZoomControlsEnabled(false);

        // Connects the client to Google Play services.
        mGoogleApiClient.connect();
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

    private class DownloadWeatherInfo extends AsyncTask <Location, Void, String> {
        @Override
        protected String doInBackground(Location... locations) {
            HttpHandler httpHandler = new HttpHandler();
            String weatherIcon = null;
            String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + locations[0].getLatitude() + "&lon=" + locations[0].getLongitude() + "&appid=f4811ea576efe623ab627935c542d838";
            // Make a request to url and get response
            String jsonStr = httpHandler.makeServiceCall(url);

            try {
                JSONObject weatherInfo = new JSONObject(jsonStr);
                JSONObject weather = weatherInfo.getJSONArray("weather").getJSONObject(0);
                weatherIcon = weather.getString("icon");

            } catch(JSONException e) {
                Toast.makeText(getApplicationContext(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
            }

            return weatherIcon;
        }

        @Override
        protected void onPostExecute(String weatherIcon) {
            super.onPostExecute(weatherIcon);

            new DownloadImage().execute("http://openweathermap.org/img/w/" + weatherIcon + ".png");
            Log.i(TAG, "Weather icon: " + weatherIcon);
        }
    }

    // DownloadImage AsyncTask
    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... URL) {

            String imageURL = URL[0];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageURL).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView weather = (ImageView) findViewById(R.id.Weather);

            weather.setImageBitmap(result);
        }
    }

    private class DownloadRoadInfo extends AsyncTask <Location, Void, RoadInfo> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RoadInfo doInBackground(Location... location) {
            HttpHandler httpHandler = new HttpHandler();

            String url = "https://nominatim.openstreetmap.org/reverse?email=michelemaestroni9@gmail.com&format=json&lat=" + location[0].getLatitude() + "&lon=" + location[0].getLongitude();
            // Make a request to url and get response
            String jsonStr = httpHandler.makeServiceCall(url);
            // Object in which store road information
            RoadInfo roadInfo = new RoadInfo();

            if(jsonStr != null) {
                try {
                    // Json object containing the place ID
                    JSONObject placeSearch = new JSONObject(jsonStr);
                    String placeId = placeSearch.getString("osm_id");
                    String type = placeSearch.getString("osm_type");

                    url = "http://overpass-api.de/api/interpreter?data=[out:json];" + type + "(" + placeId + ");out;";
                    jsonStr = httpHandler.makeServiceCall(url);

                    // Json object containing the place information
                    JSONObject placeInfo = new JSONObject(jsonStr);
                    JSONArray elements = placeInfo.getJSONArray("elements");
                    JSONObject tags = elements.getJSONObject(0).getJSONObject("tags");
                    Log.i(TAG, "Tags: " + tags);

                    // Get road info
                    if(type.equals("way")) {
                        if(tags.has("highway") || tags.has("junction")) {
                            roadInfo.setHighway(tags.getString("highway"));

                            if (tags.has("name")) {
                                roadInfo.setName(tags.getString("name"));
                            }

                            if(tags.has("maxspeed")) {
                                roadInfo.setMaxSpeed(Integer.parseInt(tags.getString("maxspeed")));
                            } else {
                                // TODO: establish if a road is urban or interurban
                            }

                            if(tags.has("tunnel")) {
                                roadInfo.setTunnel();
                            }
                        }
                    } else if(type.equals("area")) {
                        if((tags.has("highway") && tags.has("area"))) {
                            if(tags.getString("area").equals("yes")) {
                                roadInfo.setHighway(tags.getString("highway"));

                                if (tags.has("name")) {
                                    roadInfo.setName(tags.getString("name"));
                                }

                                if(tags.has("maxspeed")) {
                                    roadInfo.setMaxSpeed(Integer.parseInt(tags.getString("maxspeed")));
                                } else {
                                    // TODO: establish if a road is urban or interurban
                                }

                                if(tags.has("tunnel")) {
                                    roadInfo.setTunnel();
                                }
                            }
                        }
                    }
                    Log.i(TAG, "Highway: " + roadInfo.getHighway());
                }
                catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
                }
            }

            return roadInfo;
        }

        @Override
        protected void onPostExecute(RoadInfo roadInfo) {
            super.onPostExecute(roadInfo);

            TextView name = (TextView) findViewById(R.id.Name);
            TextView highway = (TextView) findViewById(R.id.Highway);
            ImageView maxSpeed = (ImageView) findViewById(R.id.MaxSpeed);
            // Update UI

            if(roadInfo.getName() != null) {
                name.setText(roadInfo.getName());
            }

            if(roadInfo.getHighway() != null) {
                highway.setText(roadInfo.getHighway());
            }

            if(roadInfo.isTunnel()) {
                // TODO: make the Headlights image blink
            }

            switch(roadInfo.getMaxSpeed()) {
                case 5:
                    maxSpeed.setImageResource(R.drawable.speed_limit_5);
                    break;
                case 10:
                    maxSpeed.setImageResource(R.drawable.speed_limit_10);
                    break;
                case 20:
                    maxSpeed.setImageResource(R.drawable.speed_limit_20);
                    break;
                case 30:
                    maxSpeed.setImageResource(R.drawable.speed_limit_30);
                    break;
                case 40:
                    maxSpeed.setImageResource(R.drawable.speed_limit_40);
                    break;
                case 50:
                    maxSpeed.setImageResource(R.drawable.speed_limit_50);
                    break;
                case 60:
                    maxSpeed.setImageResource(R.drawable.speed_limit_60);
                    break;
                case 70:
                    maxSpeed.setImageResource(R.drawable.speed_limit_70);
                    break;
                case 80:
                    maxSpeed.setImageResource(R.drawable.speed_limit_80);
                    break;
                case 90:
                    maxSpeed.setImageResource(R.drawable.speed_limit_90);
                    break;
                case 100:
                    maxSpeed.setImageResource(R.drawable.speed_limit_100);
                    break;
                case 110:
                    maxSpeed.setImageResource(R.drawable.speed_limit_110);
                    break;
                case 120:
                    maxSpeed.setImageResource(R.drawable.speed_limit_120);
                    break;
                case 130:
                    maxSpeed.setImageResource(R.drawable.speed_limit_130);
                    break;
                default:
                    //Toast.makeText(getApplicationContext(), "Speed limit unknown", Toast.LENGTH_LONG).show();
            }
        }
    }
}
