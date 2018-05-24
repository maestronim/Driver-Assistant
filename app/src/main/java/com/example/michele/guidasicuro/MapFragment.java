package com.example.michele.guidasicuro;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcel;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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

import java.util.Calendar;

/**
 * Created by Michele on 14/03/2018.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = MapFragment.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private Location mLocation;
    private Location mPreviousLocation;
    private Marker mMarker;
    private MyReceiver mMyReceiver;
    private boolean isFirstMeasure;
    private TextView mSpeedLimitExceededText;
    private TextView mHardBrakingText;
    private TextView mDangerousTimeText;

    public static MapFragment newInstance(UserScore userScore) {
        Log.i(TAG, "newInstance");
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putParcelable("user_score", userScore);
        fragment.setArguments(args);
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

        mSpeedLimitExceededText = getView().findViewById(R.id.speed_limit_exceeded);
        mHardBrakingText = getView().findViewById(R.id.hard_braking);
        mDangerousTimeText = getView().findViewById(R.id.dangerous_time);

        // Get the MapView to initialize the map system and view
        MapView mapView = (MapView) getView().findViewById(R.id.map);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
            mapView.getMapAsync(this);
        }

        Bundle args = getArguments();

        if(args != null) {
            UserScore userScore = args.getParcelable("user_score");
            updateUserScore(userScore.getHardBrakingCount(), userScore.getSpeedLimitExceededCount(),
                    userScore.getDangerousTimeCount());
        }

        ((MainActivity)getActivity()).setUserScoreListener(new UserScore.UserScoreListener() {
            @Override
            public void onSpeedLimitExceeded(int speedLimitExceededCount) {
                Toast.makeText(getActivity(), "Speed limit exceeded", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onHardBraking(int hardBrakingCount) {
                Toast.makeText(getActivity(), "Hard braking detected", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDangerousTime(int dangerousTimeCount) {
                Toast.makeText(getActivity(), "You are driving on dangerous time", Toast.LENGTH_LONG).show();
            }
        });

        ((MainActivity)getActivity()).setRoadInfoListener(new RoadInfo.RoadInfoListener() {
            @Override
            public void onRoadChanged(RoadInfo roadInfo) {
                Log.i(TAG, "onRoadChanged");
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
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateUserScore(int hardBrakingCount, int speedLimitExceededCount, int dangerousTimeCount) {
        mHardBrakingText.setText(String.valueOf(hardBrakingCount));
        mSpeedLimitExceededText.setText(String.valueOf(speedLimitExceededCount));
        mDangerousTimeText.setText(String.valueOf(dangerousTimeCount));
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");
        super.onDestroyView();

        // Stop register the BroadcastReceiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMyReceiver);
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
            Log.i(TAG, "Speed : " + mLocation.getSpeed());

            // Change the marker position
            mMarker.setPosition(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

            // Construct a CameraPosition focusing on the user position and animate the camera to that position.
            mCameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))      // Sets the center of the map to user position
                    .zoom(17)                   // Sets the zoom
                    .bearing((float)Bearing.getBearing(mPreviousLocation.getLatitude(), mPreviousLocation.getLongitude(), //T0DO: calculate bearing only when the distance between coordinates is long enough
                            mLocation.getLatitude(), mLocation.getLongitude()))        // Sets the orientation of the camera based on the user direction
                    .tilt(45)                   // Sets the tilt of the camera to 45 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

            // Save the previous location
            mPreviousLocation = mLocation;
        }
    }
}
