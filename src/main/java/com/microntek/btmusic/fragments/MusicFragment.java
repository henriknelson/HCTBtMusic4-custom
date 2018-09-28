package com.microntek.btmusic.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.microntek.btmusic.MainActivity;
import com.microntek.btmusic.R;
import com.microntek.btmusic.gui.MyButton;

/*import com.ag.lfm.LfmError;
import com.ag.lfm.LfmParameters;
import com.ag.lfm.LfmRequest;
import com.ag.lfm.api.LfmApi;
import com.bumptech.glide.Glide;
import com.bumptech.glide.module.AppGlideModule;*/

public class MusicFragment extends Fragment implements View.OnClickListener {

    private TextView songTitleTextView;
    private TextView songArtistTextView;
    private ImageView albumArtImgView;

    private MyButton playNextView;
    private MyButton togglePlayPausView;
    private MyButton playPreviousView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("com.microntek.btmusic","MusicFragment: onCreateView");
        View musicView = inflater.inflate(R.layout.music, container, false);
        songArtistTextView = musicView.findViewById(R.id.music_artist);
        songTitleTextView = musicView.findViewById(R.id.music_name);
        albumArtImgView = musicView.findViewById(R.id.album_art);
        setUnknownAlbumArt();
        albumArtImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).setupCurrentState();
            }
        });

        this.playPreviousView = (MyButton) musicView.findViewById(R.id.music_pre);
        this.togglePlayPausView = (MyButton) musicView.findViewById(R.id.music_play);
        this.playNextView = (MyButton) musicView.findViewById(R.id.music_next);
        this.playPreviousView.setOnClickListener(this);
        this.togglePlayPausView.setOnClickListener(this);
        this.playNextView.setOnClickListener(this);

        return musicView;
    }

    public void setMusicInfo(String str) {
        Log.i("com.microntek.btmusic","MusicFragment: setMusicInfo");
        Log.i("com.microntek.btmusic","Setting new music in  GUI: " + str);
        if (str != null) {
            String[] split = str.split("\n");
            if (split.length >= 2) {
                String musicTitle = split[0].isEmpty() ? getString(R.string.unknown).toString() : split[0].toString();
                String musicArtist = split[1].isEmpty() ? getString(R.string.unknown).toString() : split[1].toString();
                songTitleTextView.setText(musicTitle);
                songArtistTextView.setText(musicArtist);
                setAlbumArt(musicArtist,musicTitle);
                ((MainActivity)getActivity()).setWidgetMusicInfo("music.title", musicTitle);
                ((MainActivity)getActivity()).setWidgetMusicInfo("music.albunm", musicArtist);
            }
        }
    }

    private void setAlbumArt(String artist, String title) {
        Log.i("com.microntek.btmusic","MusicFragment: setAlbumArt");
        setUnknownAlbumArt();
        /*LfmParameters params = new LfmParameters();
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
        });*/
    }

    private void setUnknownAlbumArt() {
        Log.i("com.microntek.btmusic","MusicFragment: setUnknownAlbumArt");
        albumArtImgView.setImageResource(R.drawable.unknown);
        //Glide.with(getContext()).load(R.drawable.unknown).into(albumArtImgView);
    }

    @Override
    public void onClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.music_pre:
                    ((MainActivity)getActivity()).playPrevious();
                    ((MainActivity)getActivity()).setWidgetMusicInfo("music.title", "previousTitle");
                    ((MainActivity)getActivity()).setWidgetMusicInfo("music.albunm", "previousAlbunm");
                    return;
                case R.id.music_play:
                    ((MainActivity)getActivity()).playOrPauseMusic();
                    return;
                case R.id.music_next:
                    ((MainActivity)getActivity()).playNext();
                    ((MainActivity)getActivity()).setWidgetMusicState("music.title", 1);
                    ((MainActivity)getActivity()).setWidgetMusicState("music.albunm", 1);
                    return;
                default:
                    return;
            }
        } catch (Exception e) {

        }
    }
}
