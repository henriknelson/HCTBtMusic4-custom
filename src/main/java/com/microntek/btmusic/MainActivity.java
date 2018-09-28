package com.microntek.btmusic;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.microntek.CarManager;
import android.microntek.mtcser.BTServiceInf;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.microntek.btmusic.fragments.AlarmFragment;
import com.microntek.btmusic.fragments.MusicFragment;
import com.microntek.btmusic.gui.MyButton;
import com.microntek.btmusic.interfaces.IMusicFragmentCallbackReceiver;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
import static android.microntek.IRTable.IR_DOWN;
import static android.microntek.IRTable.IR_NEXT;
import static android.microntek.IRTable.IR_PLAY_PAUSE;
import static android.microntek.IRTable.IR_PREV;
import static android.microntek.IRTable.IR_SEEKDOWN;
import static android.microntek.IRTable.IR_SEEKUP;
import static android.microntek.IRTable.IR_STOP;
import static android.microntek.IRTable.IR_TUNEDOWN;
import static android.microntek.IRTable.IR_TUNEUP;
import static android.microntek.IRTable.IR_UP;

public class MainActivity extends FragmentActivity implements IMusicFragmentCallbackReceiver {

    // Instance variables
    private AudioManager audioManager = null;
    private CarManager carManager = null;
    private BTServiceInf btServiceInterface = null;


    // State variables
    private int avState = 0;
    private int btState = 0;
    private boolean isInMultiWindowMode = false;
    private boolean hasAudioFocus = false;
    private boolean isStopped = false;

    // Fragments

    private MusicFragment musicFragment = new MusicFragment();
    private AlarmFragment alarmFragment = new AlarmFragment();

    // Lifecycle callbacks

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("com.microntek.btmusic","MainActivity: onCreate");

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setupViews();

        this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        this.carManager = new CarManager();

        this.isInMultiWindowMode = isInMultiWindowMode();
        this.carManager.setParameters("av_channel_enter=gsm_bt");

        requestAudioFocus();
        notifyA2DPTurnedOn();

        Intent intent = new Intent("com.microntek.bootcheck");
        intent.putExtra("class", "com.microntek.bluetooth");
        sendBroadcast(intent);

        intent = new Intent();
        intent.setComponent(new ComponentName("android.microntek.mtcser", "android.microntek.mtcser.BlueToothService"));
        bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.microntek.bootcheck");
        intentFilter.addAction("com.microntek.removetask");
        intentFilter.addAction("hct.btmusic.play");
        intentFilter.addAction("hct.btmusic.pause");
        intentFilter.addAction("hct.btmusic.prev");
        intentFilter.addAction("hct.btmusic.next");
        intentFilter.addAction("hct.btmusic.info");
        intentFilter.addAction("hct.btmusic.playpause");
        intentFilter.addAction("com.microntek.bt.report");
        intentFilter.addAction("com.microntek.btbarstatechange");
        intentFilter.addAction("com.btmusic.finish");
        registerReceiver(this.onSystemCommandReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music_fragment.musicservicecommand");
        registerReceiver(this.onPauseCommandReceiver, intentFilter);

        this.carManager.attach(new Handler(){
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if ("KeyDown".equals(message.obj)) {
                    Bundle data = message.getData();
                    if ("key".equals(data.getString("type"))) {
                        handleKeyPress(data.getInt("value"));
                    }
                }
            }
        }, "KeyDown");
    }

    protected void onResume() {
        setupCurrentState();
        super.onResume();
        requestAudioFocus();
    }

    protected void onDestroy() {
        stop();
        super.onDestroy();
    }

    private void stop() {
        if (!this.isStopped) {
            this.isStopped = true;
            stopMusic();
            unregisterReceiver(this.onSystemCommandReceiver);
            unregisterReceiver(this.onPauseCommandReceiver);
            unbindService(this.serviceConnection);
            notifyA2DPTurnedOff();
            notifyWidgetOfInactiveState();
            this.carManager.setParameters("av_channel_exit=gsm_bt");
            abandonAudioFocus();
            this.carManager.detach();
        }
    }

    public void finish() {
        stop();
        super.finish();
    }


    // BT service connection

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btServiceInterface = BTServiceInf.Stub.asInterface(service);
            try {
                btServiceInterface.init();
            } catch (Exception e) {
            }
            if (btServiceInterface != null) {
                try {
                    btServiceInterface.syncMatchList();
                    btState = btServiceInterface.getBTState();
                    avState = btServiceInterface.getAVState();
                    setMusicInfo(btServiceInterface.getMusicInfo());
                    setModuleInfo(btServiceInterface.getModuleName(),btServiceInterface.getModulePassword());
                } catch (Exception e2) {
                }
            }
            setupCurrentState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btServiceInterface = null;
        }
    };



    // GUI management

    private void setupViews() {
        Log.i("com.microntek.btmusic","MainActivity: setupViews");
        setContentView(R.layout.fragment_container);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_root, musicFragment).commit();
    }

    private void setupCurrentState() {
        Log.i("com.microntek.btmusic","MainActivity: setupCurrentState");

        // If we do not have a BT connection..
        if (this.btState == 0 || this.avState == 0) {
            // ..and if we are not already showing the "alarm" fragment..
            if (getSupportFragmentManager().findFragmentByTag("alarm_fragment") == null){
                // ..show it!
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_root, alarmFragment).addToBackStack("alarm_fragment").commit();
            }
            notifyWidgetOfInactiveState();
            return;
        }
        notifyWidgetOfActiveState();
        // Else, if a BT connection exists and we are showing the "alarm fragment"..
        if (getSupportFragmentManager().findFragmentByTag("alarm_fragment") != null){
            getSupportFragmentManager().popBackStackImmediate("alarm_fragment",FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (this.btServiceInterface != null) {
            try {
                setMusicInfo(this.btServiceInterface.getMusicInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.isInMultiWindowMode != isInMultiWindowMode()) {
            this.isInMultiWindowMode = isInMultiWindowMode();
            setupViews();
        }
    }
    // Music related

    public void playNext() {
        try {
            this.btServiceInterface.avPlayNext();
        } catch (Exception e) {
        }
    }

    public void playOrPauseMusic() {
        try {
            this.btServiceInterface.avPlayPause();
        } catch (Exception e) {
        }
    }

    public void playPrevious() {
        try {
            this.btServiceInterface.avPlayPrev();
        } catch (Exception e) {
        }
    }


    public void stopMusic() {
        try {
            this.btServiceInterface.avPlayStop();
        } catch (Exception e) {
        }
    }

    public void goBack() {
        this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
    }


    // Audio focus

    private OnAudioFocusChangeListener onAudioFocusChanged = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            Log.i("com.microntek.btmusic","MainActivity: onAudioFocusChanged");
            if (focusChange == AUDIOFOCUS_GAIN) {
                carManager.setParameters("av_focus_gain=gsm_bt");
                hasAudioFocus = true;
            } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                carManager.setParameters("av_focus_loss=gsm_bt");
                hasAudioFocus = false;
            } else if (focusChange == AUDIOFOCUS_LOSS) {
                carManager.setParameters("av_focus_loss=gsm_bt");
                hasAudioFocus = false;
                finish();
            }
        }
    } ;

    protected void requestAudioFocus() {
        this.audioManager.requestAudioFocus(this.onAudioFocusChanged, 3, 1);
        if (!this.hasAudioFocus) {
            this.carManager.setParameters("av_focus_gain=gsm_bt");
            this.hasAudioFocus = true;
        }
    }

    protected void abandonAudioFocus() {
        this.audioManager.abandonAudioFocus(this.onAudioFocusChanged);
        if (this.hasAudioFocus) {
            this.carManager.setParameters("av_focus_loss=gsm_bt");
            this.hasAudioFocus = false;
        }
    }


    // Widget control

    private void notifyA2DPTurnedOn() {
        Intent intent = new Intent("com.microntek.canbusdisplay");
        intent.putExtra("type", "a2dp-on");
        sendBroadcast(intent);
    }

    private void notifyA2DPTurnedOff() {
        Intent intent = new Intent("com.microntek.canbusdisplay");
        intent.putExtra("type", "a2dp-off");
        sendBroadcast(intent);
    }

    private void notifyWidgetOfActiveState() {
        Intent intent = new Intent("com.android.MTClauncher.action.INSTALL_WIDGETS");
        intent.putExtra("myWidget.action", 10520);
        intent.putExtra("myWidget.packageName", "com.microntek.widget.bluetooth");
        sendBroadcast(intent);
    }

    private void notifyWidgetOfInactiveState() {
        Intent intent = new Intent("com.android.MTClauncher.action.INSTALL_WIDGETS");
        intent.putExtra("myWidget.action", 10521);
        intent.putExtra("myWidget.packageName", "com.microntek.widget.bluetooth");
        sendBroadcast(intent);
    }

    private void setMusicInfo(String musicInfoStr) {
        if (musicInfoStr != null) {
            String[] split = musicInfoStr.split("\n");
            if (split.length >= 2) {
                String songTitle = split[0].isEmpty() ? getString(R.string.unknown) : split[0];
                String songArtist = split[1].isEmpty() ? getString(R.string.unknown) : split[1];
                musicFragment.setMusicInfo(songTitle,songArtist);
                setWidgetMusicInfo("music_fragment.title", songTitle);
                setWidgetMusicInfo("music_fragment.albunm", songArtist);
            }
        }
    }

    private void setModuleInfo(String moduleName, String modulePasscode) {
        alarmFragment.setModuleInformation(moduleName,modulePasscode);
    }

    private void setWidgetMusicInfo(String str, String str2) {
        Intent intent = new Intent("com.microntek.btmusic.report");
        intent.putExtra("type", str);
        intent.putExtra("value", str2);
        sendBroadcast(intent);
    }

    private void setWidgetMusicState(String str, int i) {
        Intent intent = new Intent("com.microntek.btmusic.report");
        intent.putExtra("type", str);
        intent.putExtra("value", i);
        sendBroadcast(intent);
    }


    // Command receivers / handlers

    private BroadcastReceiver onPauseCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("com.microntek.btmusic","MainActivity: onPauseCommandReceiver");
            if (intent.getAction().equals("com.android.music_fragment.musicservicecommand") && intent.hasExtra("command") && intent.getStringExtra("command").equals("pause")) {
                MainActivity.this.carManager.setParameters("av_focus_loss=gsm_bt");
                MainActivity.this.hasAudioFocus = false;
                MainActivity.this.finish();
            }
        }
    };

    private BroadcastReceiver onSystemCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.microntek.bootcheck")) {
                action = intent.getStringExtra("class");
                if (!(action.equals("com.microntek.bluetooth") || (action.equals("phonecallin")) || (action.equals("phonecallout")))) {
                    finish();
                }
            } else if (action.equals("com.btmusic.finish")) {
                finish();
            } else if (action.equals("com.microntek.removetask")) {
                if (intent.getStringExtra("class").equals("com.microntek.btmusic")) {
                    finish();
                }
            } else if (action.equals("com.microntek.bt.report")) {
                if (intent.hasExtra("music_info")) {
                    setMusicInfo(intent.getStringExtra("music_info"));
                }
                if (btServiceInterface != null) {
                    try {
                        MainActivity.this.avState = btServiceInterface.getAVState();
                        MainActivity.this.btState = btServiceInterface.getBTState();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    setupCurrentState();
                }
            } else {
                if (action.equals("hct.btmusic.play")) {
                    playOrPauseMusic();
                } else if (action.equals("hct.btmusic.pause")) {
                    playOrPauseMusic();
                } else if (action.equals("hct.btmusic.prev")) {
                    playPrevious();
                } else if (action.equals("hct.btmusic.next")) {
                    playNext();
                } else if (action.equals("hct.btmusic.playpause")) {
                    playOrPauseMusic();
                } else if (action.equals("hct.btmusic.stop")) {
                    stopMusic();
                } else if (action.equals("hct.btmusic.info")) {
                    setWidgetMusicState("music_fragment.state", avState);
                } else if (action.equals("com.microntek.btbarstatechange")) {
                    if (btServiceInterface != null) {
                        try {
                            avState = btServiceInterface.getAVState();
                            btState = btServiceInterface.getBTState();
                        } catch (RemoteException e2) {
                            e2.printStackTrace();
                        }
                        setupCurrentState();
                    }
                    return;
                }
                requestAudioFocus();
            }
        }
    };

    private void handleKeyPress(int i) {
        boolean equals = (((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(40).get(0)).topActivity.getPackageName().equals("com.microntek.radio");
        boolean equals2 = SystemProperties.get("ro.product.customer.sub").equals("KLD25");
        if (this.hasAudioFocus) {
            switch (i) {
                case IR_PLAY_PAUSE:
                    playOrPauseMusic();
                    break;
                case IR_UP:
                case IR_SEEKDOWN:
                case IR_PREV:
                case IR_TUNEDOWN:
                    if (equals || !equals2) {
                        playPrevious();
                        break;
                    }
                    break;
                case IR_STOP:
                    stopMusic();
                    break;
                case IR_DOWN:
                case IR_SEEKUP:
                case IR_NEXT:
                case IR_TUNEUP:
                    if (equals || !equals2) {
                        playNext();
                        break;
                    }
                    break;
                default:
                    return;
            }
            requestAudioFocus();
        }
    }

}
