package com.example.michele.guidasicuro;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Michele on 14/03/2018.
 */

// TODO: Get the data from OBD2 and display that in the form of charts or simple values

public class CarFragment extends Fragment {
    public static CarFragment newInstance() {
        CarFragment fragment = new CarFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_car, container, false);
    }
}
