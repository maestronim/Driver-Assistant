package com.example.michele.guidasicuro;

import android.Manifest;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private CameraPosition mCameraPosition;
    private Location mLocation;
    private Marker mMarker;

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
            // The DecorView is the view that actually holds the window’s background drawable
            /*View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);*/
            // Hide the action bar
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
        }

        // If the device is running Android 6.0 or higher, and your app's target SDK is 23 or higher,
        // the app has to list the permissions in the manifest and request those permissions at run time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
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
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2500);
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
                .bearing(0)                 // Sets the orientation of the camera to east
                .tilt(45)                   // Sets the tilt of the camera to 45 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

        new DownloadDataTask().execute(mLocation);
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
        // Change the marker position
        mMarker.setPosition(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        // Animate the camera to the current position
        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mLocation.getLatitude(), mLocation.getLongitude())));

        new DownloadDataTask().execute(mLocation);
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

    private class DownloadDataTask extends AsyncTask <Location, Void, RoadInfo> {
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

                    // If the place is a road
                    if (type.equals("way")) {
                        url = "http://overpass-api.de/api/interpreter?data=[out:json];way(" + placeId + ");out;";
                        jsonStr = httpHandler.makeServiceCall(url);

                        // Json object containing the place information
                        JSONObject placeInfo = new JSONObject(jsonStr);
                        JSONArray elements = placeInfo.getJSONArray("elements");
                        JSONObject tags = elements.getJSONObject(0).getJSONObject("tags");
                        Log.i(TAG, "Tags: " + tags.toString());
                        // Get road information
                        roadInfo.setName(tags.getString("name"));
                        roadInfo.setMaxSpeed(Integer.parseInt(tags.getString("maxspeed")));
                        roadInfo.setHighway(tags.getString("highway"));

                        Log.i(TAG, "Highway: " + tags.getString("highway"));
                    } else {
                        Toast.makeText(getApplicationContext(), "You are not on the road", Toast.LENGTH_LONG).show();
                    }
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

            // Update UI
            name.setText(roadInfo.getName());
            highway.setText(roadInfo.getHighway());
        }
    }
}
