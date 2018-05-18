package com.example.michele.guidasicuro;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Created by Michele on 14/03/2018.
 */

// TODO: Get the data from OBD2 and display that in the form of charts or simple values

public class CarFragment extends Fragment {
    private String mDeviceAddress;
    private static final String TAG = CarFragment.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 10;
    private UserScore mUserScore;
    private ArrayList<CarParameters> mArrayOfCarParameters;
    private ArrayList<String> mArrayOfCarFaultCodes;
    private ListView mListView;

    public class CarParametersAdapter extends ArrayAdapter<CarParameters> {
        public CarParametersAdapter(Context context, ArrayList<CarParameters> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            CarParameters carParameter = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.parameter_car, parent, false);
            }
            // Lookup view for data population
            TextView parameterName = (TextView) convertView.findViewById(R.id.parameterName);
            TextView parameterValue = (TextView) convertView.findViewById(R.id.parameterValue);
            ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.determinateBar);
            // Populate the data into the template view using the data object
            parameterName.setText(carParameter.getName());
            parameterValue.setText(carParameter.getValue());
            progressBar.setMax(carParameter.getMaxValue());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    public static CarFragment newInstance() {
        CarFragment fragment = new CarFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach");
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Construct the data source
        mArrayOfCarParameters = new ArrayList<CarParameters>();
        mArrayOfCarFaultCodes = new ArrayList<String>();

        for(int i=0;i<20;i++) {
            mArrayOfCarParameters.add(new CarParameters("N.d.", "N.d.", 100));
            mArrayOfCarFaultCodes.add("N.d.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        return inflater.inflate(R.layout.fragment_car, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Create the adapter to convert the array to views
        CarParametersAdapter carParametersAdapter = new CarParametersAdapter(getActivity(), mArrayOfCarParameters);
        // Attach the adapter to a ListView
        mListView = (ListView) getView().findViewById(R.id.lvParam);
        mListView.setAdapter(carParametersAdapter);

        ((MainActivity)getActivity()).setCarParametersListener(new CarParameters.CarParametersListener() {
            @Override
            public void onCarParametersChanged(ArrayList<CarParameters> arrayOfCarParameters) {
                // TODO: update changes
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_fragment_car, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.car_data:
                CarParametersAdapter carParametersAdapter = new CarParametersAdapter(getActivity(), mArrayOfCarParameters);
                mListView.setAdapter(carParametersAdapter);
                break;
            case R.id.fault_codes:
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_list_item_1, mArrayOfCarFaultCodes);
                mListView.setAdapter(arrayAdapter);
                break;
            default:
                break;
        }

        return true;
    }
}
