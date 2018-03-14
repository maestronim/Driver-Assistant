package com.example.michele.guidasicuro;

import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by Michele on 14/03/2018.
 */

public class WeatherFragment extends Fragment{
    public static final String TAG = WeatherFragment.class.getSimpleName();
    private static final int mWeatherUpdateInterval = 600000;
    private Handler mHandler = new Handler();
    private Runnable mWeatherUpdate;

    public static WeatherFragment newInstance() {
        WeatherFragment fragment = new WeatherFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Runnable scheduled to download the weather info
        mWeatherUpdate = new Runnable(){
            public void run() {
                // Execute the task to download the weather info
                // TODO: get the location from a broadcast receiver
                //new DownloadWeatherInfo().execute(mLocation);
                Toast.makeText(getActivity(), "Weather update", Toast.LENGTH_LONG).show();
                mHandler.postDelayed(this, mWeatherUpdateInterval);
            }
        };
        mHandler.post(mWeatherUpdate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    private class DownloadWeatherInfo extends AsyncTask<Location, Void, String> {
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
                Log.i(TAG, "Couldn't get json from server");
                Toast.makeText(getActivity(), "Couldn't get json from server", Toast.LENGTH_LONG).show();
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
            // TODO: get the bitmap from OpenWeatherMap
        }
    }
}
