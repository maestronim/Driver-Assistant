package com.example.michele.guidasicuro;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.aakira.expandablelayout.ExpandableWeightLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Michele on 14/03/2018.
 */

// TODO: update "hard braking", "speed limit exceeded", "dangerous time" indicators

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = MapFragment.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private Location mLocation;
    private Location mPreviousLocation;
    private Marker mMarker;
    private MyReceiver mMyReceiver;
    private AsyncTask mDownloadRoadInfoTask;
    private boolean isFirstMeasure;

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        isFirstMeasure = true;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // BroadcastReceiver
        mMyReceiver = new MyReceiver();

        // Register BroadcastReceiver to receive the data from the service
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMyReceiver, new IntentFilter("GPSLocationUpdates"));

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        // Get the MapView to initialize the map system and view
        MapView mapView = (MapView) getView().findViewById(R.id.map);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        // Stop register the BroadcastReceiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMyReceiver);
        mDownloadRoadInfoTask.cancel(true);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Place a marker on the map
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(45.698264, 9.67727));
        mMarker = mMap.addMarker(markerOptions);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getBundleExtra("Location");
            mLocation = (Location) b.getParcelable("Location");

            if(isFirstMeasure) {
                mPreviousLocation = mLocation;
                isFirstMeasure = false;
            }

            Log.i(TAG, "Latitude: " + mLocation.getLatitude());
            Log.i(TAG, "Longitude: " + mLocation.getLongitude());

            // Change the marker position
            mMarker.setPosition(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

            // Construct a CameraPosition focusing on the user position and animate the camera to that position.
            mCameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))      // Sets the center of the map to user position
                    .zoom(17)                   // Sets the zoom
                    .bearing((float)Bearing.getBearing(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude(), mLocation.getLatitude(), mLocation.getLongitude()))        // Sets the orientation of the camera based on the user direction
                    .tilt(45)                   // Sets the tilt of the camera to 45 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

            // Save the previous location
            mPreviousLocation = mLocation;

            // Execute the task to download the data
            mDownloadRoadInfoTask = new DownloadRoadInfo().execute(mLocation);
        }

    }

    private class DownloadRoadInfo extends AsyncTask <Location, Void, RoadInfo> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RoadInfo doInBackground(Location... location) {
            String url = "https://nominatim.openstreetmap.org/reverse?email=michelemaestroni9@gmail.com&format=json&lat=" + location[0].getLatitude() + "&lon=" + location[0].getLongitude() + "&zoom=16";
            // Make a request to url and get response
            String jsonStr = HttpHandler.makeServiceCall(url);
            // Object in which store road information
            RoadInfo roadInfo = new RoadInfo();
            String type="", placeId="",highway="";
            boolean maxSpeedIsDefined = false, isARoad = false;

            if(jsonStr != null) {
                try {
                    // Json object containing the place ID
                    JSONObject placeSearch = new JSONObject(jsonStr);
                    placeId = placeSearch.getString("osm_id");
                    type = placeSearch.getString("osm_type");
                } catch (JSONException e) {
                    Log.i(TAG, "Couldn't get json from server");
                    Toast.makeText(getActivity(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
                }

                url = "http://overpass-api.de/api/interpreter?data=[out:json];" + type + "(" + placeId + ");out;";
                jsonStr = HttpHandler.makeServiceCall(url);

                if(jsonStr != null) {
                    JSONObject tags = null;
                    try {
                        // Json object containing the place information
                        JSONObject placeInfo = new JSONObject(jsonStr);
                        JSONArray elements = placeInfo.getJSONArray("elements");
                        tags = elements.getJSONObject(0).getJSONObject("tags");
                        Log.i(TAG, "Tags: " + tags);
                        // Get road info
                        if (type.equals("way")) {
                            if (tags.has("highway") || tags.has("junction")) {
                                isARoad = true;
                            }
                        } else if (type.equals("area")) {
                            if ((tags.has("highway") && tags.has("area"))) {
                                if (tags.getString("area").equals("yes")) {
                                    isARoad = true;
                                }
                            }
                        }

                        if (isARoad) {
                            highway = tags.getString("highway");
                            roadInfo.setHighway(highway);
                            if (highway.equals("motorway")) {
                                roadInfo.setMaxSpeed(130);
                                maxSpeedIsDefined = true;

                            } else if (roadInfo.getHighway().equals("residential")) {
                                roadInfo.setMaxSpeed(50);
                                maxSpeedIsDefined = true;
                            }

                            if (tags.has("name")) {
                                roadInfo.setName(tags.getString("name"));
                            }

                            if (tags.has("maxspeed")) {
                                roadInfo.setMaxSpeed(Integer.parseInt(tags.getString("maxspeed")));
                                maxSpeedIsDefined = true;
                            }

                            if (tags.has("tunnel")) {
                                roadInfo.setTunnel();
                            }
                        }
                    } catch (JSONException e) {
                        Log.i(TAG, "Couldn't get json from server");
                        Toast.makeText(getActivity(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
                    }

                    if(!maxSpeedIsDefined) {
                        BoundingBox boundingBox = new BoundingBox();
                        // TODO: Distance needs to be defined
                        boundingBox.calculate(mLocation, 100);

                        url = "http://overpass.osm.rambler.ru/cgi/interpreter?data=[out:json];node[highway=residential](" + boundingBox.getSouthernLimit() + "," + boundingBox.getWesternLimit() + "," + boundingBox.getNorthernLimit() + "," + boundingBox.getEasternLimit() + ");out;";
                        jsonStr =  HttpHandler.makeServiceCall(url);

                        try {
                            if(jsonStr != null) {
                                // Json object containing the nearby residential roads
                                JSONObject nearbyRoads = new JSONObject(jsonStr);

                                // The road is urban if there are residential roads within the distance
                                if(nearbyRoads.getJSONArray("elements").length() > 0) {
                                    Log.i(TAG, "The road is urban");
                                    Log.i(TAG, "Highway: " + highway);
                                    switch (highway) {
                                        case "trunk":
                                            roadInfo.setMaxSpeed(70);
                                            break;
                                        case "primary":
                                            roadInfo.setMaxSpeed(50);
                                            break;
                                        case "secondary":
                                            roadInfo.setMaxSpeed(50);
                                            break;
                                        case "tertiary":
                                            roadInfo.setMaxSpeed(50);
                                            break;
                                        case "unclassified":
                                            roadInfo.setMaxSpeed(50);
                                            break;
                                    }
                                } else {
                                    Log.i(TAG, "The road is interurban");
                                    Log.i(TAG, "Highway: " + highway);
                                    switch (highway) {
                                        case "trunk":
                                            roadInfo.setMaxSpeed(110);
                                            break;
                                        case "primary":
                                            roadInfo.setMaxSpeed(90);
                                            break;
                                        case "secondary":
                                            roadInfo.setMaxSpeed(90);
                                            break;
                                        case "tertiary":
                                            roadInfo.setMaxSpeed(90);
                                            break;
                                        case "unclassified":
                                            roadInfo.setMaxSpeed(70);
                                            break;
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.i(TAG, "Couldn't get json from server");
                            Toast.makeText(getActivity(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            return roadInfo;
        }

        @Override
        protected void onPostExecute(RoadInfo roadInfo) {
            super.onPostExecute(roadInfo);

            try {
                TextView name = (TextView) getView().findViewById(R.id.Name);
                TextView highway = (TextView) getView().findViewById(R.id.Highway);
                ImageView maxSpeed = (ImageView) getView().findViewById(R.id.MaxSpeed);
                // Update UI
                if (roadInfo.getName() != null) {
                    name.setText(roadInfo.getName());
                }

                if (roadInfo.getHighway() != null) {
                    highway.setText(roadInfo.getHighway());
                }

                if (roadInfo.isTunnel()) {
                    // TODO: make the Headlights image blink
                }

                switch (roadInfo.getMaxSpeed()) {
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
            } catch(NullPointerException e) {
                Log.i(TAG, e.getMessage());
            }
        }
    }
}
