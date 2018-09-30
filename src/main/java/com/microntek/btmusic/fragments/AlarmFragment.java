package com.microntek.btmusic.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microntek.btmusic.MainActivity;
import com.microntek.btmusic.R;
import com.microntek.btmusic.interfaces.IMusicFragmentCallbackReceiver;

import pl.droidsonroids.gif.GifImageView;

public class AlarmFragment extends Fragment {

    private IMusicFragmentCallbackReceiver callbackReceiver;

    private TextView btModuleNameTextView;
    private TextView btModulePasswordTextView;
    private GifImageView gifImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("com.microntek.btmusic","AlarmFragment: onCreateView");
        View alarmView = inflater.inflate(R.layout.alarm_fragment, container, false);
        btModuleNameTextView = alarmView.findViewById(R.id.tv_name);
        btModulePasswordTextView = alarmView.findViewById(R.id.tv_pincode);
        gifImageView = alarmView.findViewById(R.id.bt_anim);
        return alarmView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("com.microntek.btmusic","AlarmFragment: onAttach");
        // Make sure we can callback to our parent activity
        callbackReceiver = (IMusicFragmentCallbackReceiver) context;
    }

    public void setModuleInformation(String moduleName, String passCode) {
        Log.i("com.microntek.btmusic","AlarmFragment: setModuleInformation");
        btModuleNameTextView.setText(moduleName);
        btModulePasswordTextView.setText(passCode);
    }

}
