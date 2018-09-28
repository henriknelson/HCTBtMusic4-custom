package com.microntek.btmusic.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microntek.btmusic.MainActivity;
import com.microntek.btmusic.R;

import pl.droidsonroids.gif.GifImageView;

public class AlarmFragment extends Fragment {

    private TextView btModuleNameTextView;
    private TextView btModulePasswordTextView;
    private GifImageView gifImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("com.microntek.btmusic","AlarmFragment: onCreateView");
        View alarmView = inflater.inflate(R.layout.alarm, container, false);
        btModuleNameTextView = alarmView.findViewById(R.id.tv_name);
        btModulePasswordTextView = alarmView.findViewById(R.id.tv_pincode);
        gifImageView = alarmView.findViewById(R.id.bt_anim);
        gifImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).setupCurrentState();
            }
        });
        return alarmView;
    }

    public void setModuleInformation(String moduleName, String passCode) {
        Log.i("com.microntek.btmusic","AlarmFragment: setModuleInformation");
        this.btModuleNameTextView.setText(moduleName);
        this.btModulePasswordTextView.setText(passCode);
    }

}
