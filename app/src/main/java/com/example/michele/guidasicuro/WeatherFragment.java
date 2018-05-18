package com.example.michele.guidasicuro;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Parcel;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.pwittchen.weathericonview.WeatherIconView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private WeatherInfo mWeatherInfo;
    private ProgressDialog mProgressDialog;

    public static WeatherFragment newInstance() {
        WeatherFragment fragment = new WeatherFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity(),
                ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.show();
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

        // BroadcastReceiver
        mMyReceiver = new MyReceiver();
        
        mWeatherInfo = new WeatherInfo();

        // Register BroadcastReceiver to receive the data from the service
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMyReceiver, new IntentFilter("GPSLocationUpdates"));

        return inflater.inflate(R.layout.fragment_weather, container, false);
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

        WeatherIconView weatherConditionIcon = (WeatherIconView) getView().findViewById(R.id.weather_condition_icon);
        weatherConditionIcon.setHeight(getPixelsFromDp(100f));
        weatherConditionIcon.setWidth(getPixelsFromDp(100f));
        weatherConditionIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 75f);

        WeatherIconView humidityIcon = (WeatherIconView) getView().findViewById(R.id.humidity_icon);
        humidityIcon.setHeight(getPixelsFromDp(75f));
        humidityIcon.setWidth(getPixelsFromDp(75f));
        humidityIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55f);

        WeatherIconView windIcon = (WeatherIconView) getView().findViewById(R.id.wind_icon);
        windIcon.setHeight(getPixelsFromDp(75f));
        windIcon.setWidth(getPixelsFromDp(75f));
        windIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55f);

        WeatherIconView sunriseIcon = (WeatherIconView) getView().findViewById(R.id.sunrise_icon);
        sunriseIcon.setHeight(getPixelsFromDp(75f));
        sunriseIcon.setWidth(getPixelsFromDp(75f));
        sunriseIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55f);

        WeatherIconView sunsetIcon = (WeatherIconView) getView().findViewById(R.id.sunset_icon);
        sunsetIcon.setHeight(getPixelsFromDp(75f));
        sunsetIcon.setWidth(getPixelsFromDp(75f));
        sunsetIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 55f);
    }

    int getPixelsFromDp(float dp) {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float fpixels = metrics.density * dp;

        return (int) (fpixels + 0.5f);
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
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getBundleExtra("Location");
            mLocation = (Location) b.getParcelable("Location");

            String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + mLocation.getLatitude() + "&lon=" + mLocation.getLongitude() + "&units=metric&appid=f4811ea576efe623ab627935c542d838";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject weatherInfoObject = response;
                                if(weatherInfoObject.has("name")) {
                                    mWeatherInfo.setCity(weatherInfoObject.getString("name"));
                                }
                                if(weatherInfoObject.getJSONObject("main").has("humidity")) {
                                    mWeatherInfo.setHumidity(weatherInfoObject.getJSONObject("main").getInt("humidity"));
                                }
                                if(weatherInfoObject.getJSONObject("main").has("temp")) {
                                    mWeatherInfo.setTemperature(Math.round(weatherInfoObject.getJSONObject("main").getDouble("temp")));
                                }
                                if(weatherInfoObject.getJSONObject("main").has("temp_min")) {
                                    mWeatherInfo.setMinTemperature(weatherInfoObject.getJSONObject("main").getInt("temp_min"));
                                }
                                if(weatherInfoObject.getJSONObject("main").has("temp_max")) {
                                    mWeatherInfo.setMaxTemperature(weatherInfoObject.getJSONObject("main").getInt("temp_max"));
                                }
                                if(weatherInfoObject.getJSONObject("wind").has("speed") && weatherInfoObject.getJSONObject("wind").has("deg")) {
                                    mWeatherInfo.setWind(Math.round(weatherInfoObject.getJSONObject("wind").getDouble("speed")), weatherInfoObject.getJSONObject("wind").getInt("deg"));
                                } else if(weatherInfoObject.getJSONObject("wind").has("speed") && !weatherInfoObject.getJSONObject("wind").has("deg")) {
                                    mWeatherInfo.setWind(Math.round(weatherInfoObject.getJSONObject("wind").getDouble("speed")), -1);
                                }
                                if(weatherInfoObject.getJSONObject("sys").has("sunrise")) {
                                    mWeatherInfo.setSunrise(weatherInfoObject.getJSONObject("sys").getLong("sunrise"));
                                }
                                if(weatherInfoObject.getJSONObject("sys").has("sunset")) {
                                    mWeatherInfo.setSunset(weatherInfoObject.getJSONObject("sys").getLong("sunset"));
                                }
                                if(weatherInfoObject.getJSONArray("weather").getJSONObject(0).has("description")) {
                                    mWeatherInfo.setDescription(weatherInfoObject.getJSONArray("weather").getJSONObject(0).getString("description"));
                                }
                                if(weatherInfoObject.getJSONArray("weather").getJSONObject(0).has("icon")) {
                                    mWeatherInfo.setIcon(weatherInfoObject.getJSONArray("weather").getJSONObject(0).getString("icon"));
                                }
                            } catch(JSONException e) {
                                e.printStackTrace();
                            } catch(Exception e) {
                                e.printStackTrace();
                            }

                            updateUI();
                            mProgressDialog.dismiss();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });

            MySingleton.getInstance(getActivity()).addToRequestQueue(jsonObjectRequest);
        }
    }
    
    private void updateUI() {
        try {
            TextView weatherCity = (TextView) getView().findViewById(R.id.weather_city);
            weatherCity.setText(mWeatherInfo.getCity());

            TextView weatherDescription = (TextView) getView().findViewById(R.id.weather_description);
            weatherDescription.setText(mWeatherInfo.getDescription());

            TextView temperature = (TextView) getView().findViewById(R.id.temp);
            temperature.setText(formatNumber(mWeatherInfo.getTemperature()) + "°");

            TextView minTemperature = (TextView) getView().findViewById(R.id.min_temp_value);
            minTemperature.setText(String.valueOf(mWeatherInfo.getMinTemperature()) + "°");

            TextView maxTemperature = (TextView) getView().findViewById(R.id.max_temp_value);
            maxTemperature.setText(String.valueOf(mWeatherInfo.getMaxTemperature()) + "°");

            TextView humidity = (TextView) getView().findViewById(R.id.humidity_value);
            humidity.setText(String.valueOf(mWeatherInfo.getHumidity()) + "%");

            TextView windSpeed = (TextView) getView().findViewById(R.id.wind_speed_value);
            windSpeed.setText(formatNumber(mWeatherInfo.getWindSpeed()) + "km/h");

            if(mWeatherInfo.getWindDirection() != -1) {
                TextView windDirection = (TextView) getView().findViewById(R.id.wind_direction_value);
                windDirection.setText(String.valueOf(mWeatherInfo.getWindDirection()));
            } else {
                TextView windDirection = (TextView) getView().findViewById(R.id.wind_direction_value);
                windDirection.setText("n.d.");
            }

            DateFormat formatter = new SimpleDateFormat("HH:mm");
            Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(mWeatherInfo.getSunrise() * 1000);

            TextView sunrise = (TextView) getView().findViewById(R.id.sunrise_text);
            sunrise.setText(formatter.format(calendar.getTime()));

            calendar.setTimeInMillis(mWeatherInfo.getSunset() * 1000);

            TextView sunset = (TextView) getView().findViewById(R.id.sunset_text);
            sunset.setText(formatter.format(calendar.getTime()));

            HashMap<String, Integer> icon_and_background = getIconAndBackground(mWeatherInfo.getIcon());

            WeatherIconView weatherIcon = (WeatherIconView) getView().findViewById(R.id.weather_condition_icon);
            weatherIcon.setIconResource(getString(icon_and_background.get("icon")));

            Log.i(TAG, getString(icon_and_background.get("background")));

            RelativeLayout weatherTopLayout = (RelativeLayout) getView().findViewById(R.id.weather_top_layout);
            weatherTopLayout.setBackgroundResource(icon_and_background.get("background"));
        } catch(NullPointerException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String formatNumber(double number) {
        DecimalFormat df = new DecimalFormat("#");

        return df.format(number);
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
