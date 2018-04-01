package com.example.michele.guidasicuro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.weathericonview.WeatherIconView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michele on 14/03/2018.
 */

public class WeatherFragment extends Fragment{
    public static final String TAG = WeatherFragment.class.getSimpleName();
    private Location mLocation;
    private BroadcastReceiver mMyReceiver;

    public static WeatherFragment newInstance() {
        WeatherFragment fragment = new WeatherFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mMyReceiver = new MyReceiver();
    }

    private int getMainLayoutHeight() {
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                getActivity().findViewById(R.id.bottom_navigation);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y - bottomNavigationView.getMeasuredHeight();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        // BroadcastReceiver
        mMyReceiver = new WeatherFragment.MyReceiver();

        // Register BroadcastReceiver to receive the data from the service
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMyReceiver, new IntentFilter("GPSLocationUpdates"));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        // get view you want to resize
        RelativeLayout mainLayout = (RelativeLayout) getView().findViewById(R.id.weather_top_layout);
        // get layout parameters for that view
        ViewGroup.LayoutParams params = mainLayout.getLayoutParams();
        // change height of the params e.g. 480dp
        params.height = getMainLayoutHeight();
        // initialize new parameters for my element
        mainLayout.setLayoutParams(new RelativeLayout.LayoutParams(params));
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        // Stop register the BroadcastReceiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMyReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getBundleExtra("Location");
            mLocation = (Location) b.getParcelable("Location");

            new DownloadWeatherInfo().execute(mLocation);
        }
    }

    private class DownloadWeatherInfo extends AsyncTask<Location, Void, WeatherInfo> {
        @Override
        protected WeatherInfo doInBackground(Location... locations) {
            WeatherInfo weatherInfo = new WeatherInfo();
            String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + locations[0].getLatitude() + "&lon=" + locations[0].getLongitude() + "&units=metric&appid=f4811ea576efe623ab627935c542d838";
            // Make a request to url and get response
            String jsonStr = HttpHandler.makeServiceCall(url);

            Log.i(TAG, jsonStr);

            try {
                // TODO: remove the decimal part from the integer values
                JSONObject weatherInfoObject = new JSONObject(jsonStr);
                weatherInfo.setCity(weatherInfoObject.getString("name"));
                weatherInfo.setHumidity(Integer.parseInt(weatherInfoObject.getJSONObject("main").getString("humidity")));
                weatherInfo.setTemperature((int)Math.round(Double.parseDouble(weatherInfoObject.getJSONObject("main").getString("temp"))));
                weatherInfo.setMinTemperature((int)Math.round(Double.parseDouble(weatherInfoObject.getJSONObject("main").getString("temp_min"))));
                weatherInfo.setMaxTemperature((int)Math.round(Double.parseDouble(weatherInfoObject.getJSONObject("main").getString("temp_max"))));
                weatherInfo.setWind((int)Math.round(Double.parseDouble(weatherInfoObject.getJSONObject("wind").getString("speed"))), Integer.parseInt(weatherInfoObject.getJSONObject("wind").getString("deg")));
                weatherInfo.setSunrise(Double.parseDouble(weatherInfoObject.getJSONObject("sys").getString("sunrise")));
                weatherInfo.setSunset(Double.parseDouble(weatherInfoObject.getJSONObject("sys").getString("sunset")));
                weatherInfo.setDescription(weatherInfoObject.getJSONArray("weather").getJSONObject(0).getString("description"));
                weatherInfo.setIcon(weatherInfoObject.getJSONArray("weather").getJSONObject(0).getString("icon"));
            } catch(JSONException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }

            return weatherInfo;
        }

        @Override
        protected void onPostExecute(WeatherInfo weatherInfo) {
            super.onPostExecute(weatherInfo);

            if(weatherInfo.getCity() != null) {
                Log.i(TAG, "City: " + weatherInfo.getCity());
            }

            try {
                TextView weatherCity = (TextView) getView().findViewById(R.id.weather_city);
                weatherCity.setText(weatherInfo.getCity());

                TextView weatherDescription = (TextView) getView().findViewById(R.id.weather_description);
                weatherDescription.setText(weatherInfo.getDescription());

                TextView temperature = (TextView) getView().findViewById(R.id.temp);
                temperature.setText(String.valueOf(weatherInfo.getTemperature()) + "°");

                Log.i(TAG, "temperature: " + String.valueOf(weatherInfo.getTemperature()));

                TextView minTemperature = (TextView) getView().findViewById(R.id.min_temp_value);
                minTemperature.setText(String.valueOf(weatherInfo.getMinTemperature()));

                TextView maxTemperature = (TextView) getView().findViewById(R.id.max_temp_value);
                maxTemperature.setText(String.valueOf(weatherInfo.getMaxTemperature()));

                TextView humidity = (TextView) getView().findViewById(R.id.humidity_value);
                humidity.setText(String.valueOf(weatherInfo.getHumidity()) + "%");

                TextView windSpeed = (TextView) getView().findViewById(R.id.wind_speed_value);
                windSpeed.setText(String.valueOf(weatherInfo.getWindSpeed()));

                TextView windDirection = (TextView) getView().findViewById(R.id.wind_direction_value);
                windDirection.setText(String.valueOf(weatherInfo.getWindDirection()));

                HashMap<String, Integer> icon_and_background = getIconAndBackground(weatherInfo.getIcon());

                WeatherIconView weatherIcon = (WeatherIconView) getView().findViewById(R.id.weather_condition_icon);
                weatherIcon.setIconResource(getString(icon_and_background.get("icon")));

                LinearLayout weatherMainLayout = (LinearLayout) getView().findViewById(R.id.weather_main_layout);
                weatherMainLayout.setBackgroundResource(icon_and_background.get("background"));
            } catch(NullPointerException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String, Integer> getIconAndBackground(String icon) {
        HashMap<String, Integer> icon_and_background = new HashMap<>();
        boolean isDayTime = false;

        if(icon.charAt(2) == 'd') {
            isDayTime = true;
        }

        if(isDayTime) {
            switch(Integer.parseInt(icon.substring(0, 2))) {
                case 1:
                    icon_and_background.put("icon", R.string.wi_day_sunny);
                    icon_and_background.put("background", R.drawable.day_sunny);
                    break;
                case 2:
                    icon_and_background.put("icon", R.string.wi_day_cloudy);
                    icon_and_background.put("background", R.drawable.day_scattered_clouds);
                    break;
                case 3:
                    icon_and_background.put("icon", R.string.wi_cloud);
                    icon_and_background.put("background", R.drawable.day_scattered_clouds);
                    break;
                case 4:
                    icon_and_background.put("icon", R.string.wi_cloudy);
                    icon_and_background.put("background", R.drawable.day_broken_clouds);
                    break;
                case 9:
                    icon_and_background.put("icon", R.string.wi_showers);
                    icon_and_background.put("background", R.drawable.day_rain);
                    break;
                case 10:
                    icon_and_background.put("icon", R.string.wi_day_showers);
                    icon_and_background.put("background", R.drawable.day_rain);
                    break;
                case 11:
                    icon_and_background.put("icon", R.string.wi_thunderstorm);
                    icon_and_background.put("background", R.drawable.thunderstorm);
                    break;
                case 13:
                    icon_and_background.put("icon", R.string.wi_snow);
                    icon_and_background.put("background", R.drawable.day_snow);
                    break;
                case 50:
                    icon_and_background.put("icon", R.string.wi_fog);
                    icon_and_background.put("background", R.drawable.day_fog);
                    break;
                default:
                    icon_and_background.put("icon", R.string.wi_na);
                    icon_and_background.put("background", 0);
            }
        } else {
            switch(Integer.parseInt(icon.substring(0, 2))) {
                case 1:
                    icon_and_background.put("icon", R.string.wi_night_clear);
                    icon_and_background.put("background", R.drawable.night_clear);
                    break;
                case 2:
                    icon_and_background.put("icon", R.string.wi_night_alt_cloudy);
                    icon_and_background.put("background", R.drawable.night_few_clouds);
                    break;
                case 3:
                    icon_and_background.put("icon", R.string.wi_cloud);
                    icon_and_background.put("background", R.drawable.night_cloudy);
                    break;
                case 4:
                    icon_and_background.put("icon", R.string.wi_cloudy);
                    icon_and_background.put("background", R.drawable.night_cloudy);
                    break;
                case 9:
                    icon_and_background.put("icon", R.string.wi_showers);
                    icon_and_background.put("background", R.drawable.night_rain);
                    break;
                case 10:
                    icon_and_background.put("icon", R.string.wi_night_alt_showers);
                    icon_and_background.put("background", R.drawable.night_rain);
                    break;
                case 11:
                    icon_and_background.put("icon", R.string.wi_thunderstorm);
                    icon_and_background.put("background", R.drawable.thunderstorm);
                    break;
                case 13:
                    icon_and_background.put("icon", R.string.wi_snow);
                    icon_and_background.put("background", R.drawable.night_snow);
                case 50:
                    icon_and_background.put("icon", R.string.wi_fog);
                    icon_and_background.put("background", R.drawable.night_fog);
                    break;
                default:
                    icon_and_background.put("icon", R.string.wi_na);
                    icon_and_background.put("background", 0);
            }
        }

        return icon_and_background;
    }
}
