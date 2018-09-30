package com.microntek.btmusic.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.microntek.btmusic.R;
import com.microntek.btmusic.gui.MyButton;
import com.microntek.btmusic.interfaces.IMusicFragmentCallbackReceiver;

import com.ag.lfm.LfmError;
import com.ag.lfm.LfmParameters;
import com.ag.lfm.LfmRequest;
import com.ag.lfm.api.LfmApi;

import org.json.JSONArray;
import org.json.JSONObject;

public class MusicFragment extends Fragment implements View.OnClickListener {

    private IMusicFragmentCallbackReceiver callbackReceiver;

    private TextView songTitleTextView;
    private TextView songArtistTextView;
    private ImageView albumArtImgView;

    private MyButton playNextView;
    private MyButton togglePlayPausView;
    private MyButton playPreviousView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("com.microntek.btmusic","MusicFragment: onCreateView");
        View musicView = inflater.inflate(R.layout.music_fragment, container, false);

        // Set up GUI components
        songArtistTextView = musicView.findViewById(R.id.music_artist);
        songTitleTextView = musicView.findViewById(R.id.music_name);
        albumArtImgView = musicView.findViewById(R.id.album_art);
        playPreviousView = musicView.findViewById(R.id.music_pre);
        togglePlayPausView = musicView.findViewById(R.id.music_play);
        playNextView = musicView.findViewById(R.id.music_next);

        // Set up gui event listeners
        playPreviousView.setOnClickListener(this);
        togglePlayPausView.setOnClickListener(this);
        playNextView.setOnClickListener(this);

        return musicView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("com.microntek.btmusic","MusicFragment: onAttach");
        // Make sure we can callback to our parent activity
        callbackReceiver = (IMusicFragmentCallbackReceiver) context;
    }

    public void setMusicInfo(String songArtist, String songTitle) {
        Log.i("com.microntek.btmusic","MusicFragment: setMusicInfo");
        songArtistTextView.setText(songArtist);
        songTitleTextView.setText(songTitle);
    }

    public void setAlbumArt(String artist, String title) {
        Log.i("com.microntek.btmusic","MusicFragment: setAlbumArt");
        LfmParameters params = new LfmParameters();
        params.put("autocorrect","1");
        params.put("artist",artist);
        params.put("track",title);

        LfmRequest request = LfmApi.track().getInfo(params);
        request.executeWithListener(new LfmRequest.LfmRequestListener() {
            @Override
            public void onComplete(JSONObject response) {
                try {
                    String mbid = response.getJSONObject("track").getJSONObject("album").getString("mbid");
                    LfmParameters params = new LfmParameters();
                    params.put("mbid",mbid);
                    LfmRequest request = LfmApi.album().getInfo(params);
                    request.executeWithListener(new LfmRequest.LfmRequestListener() {
                        @Override
                        public void onComplete(JSONObject response) {
                            try {
                                JSONArray imageArray = response.getJSONObject("album").getJSONArray("image");
                                String albumArtURL = imageArray.getJSONObject(imageArray.length()-1).getString("#text");
                                Glide.with(getContext()).load(albumArtURL).into(albumArtImgView);
                            }catch(Exception e){
                                setUnknownAlbumArt();
                            }
                        }

                        @Override
                        public void onError(LfmError error) {
                            setUnknownAlbumArt();
                        }
                    });

                }catch(Exception e){
                    setUnknownAlbumArt();
                }

            }

            @Override
            public void onError(LfmError error) {
                setUnknownAlbumArt();
            }
        });
    }

    public void setUnknownAlbumArt() {
        Log.i("com.microntek.btmusic","MusicFragment: setUnknownAlbumArt");
        Glide.with(getContext()).load(R.drawable.unknown).into(albumArtImgView);
    }

    @Override
    public void onClick(View view) {
        Log.i("com.microntek.btmusic","MusicFragment: onClick");
        try {
            switch (view.getId()) {
                case R.id.music_pre:
                    callbackReceiver.playPrevious();
                    return;
                case R.id.music_play:
                    callbackReceiver.playOrPauseMusic();
                    return;
                case R.id.music_next:
                    callbackReceiver.playNext();
                    return;
                default:
            }
        } catch (Exception e) {
            Log.e("com.microntek.btmusic","Error in onClick()");
        }
    }
}
