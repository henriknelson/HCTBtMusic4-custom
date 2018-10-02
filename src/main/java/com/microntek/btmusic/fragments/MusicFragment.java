package com.microntek.btmusic.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.microntek.btmusic.MainActivity;
import com.microntek.btmusic.R;
import com.microntek.btmusic.gui.MyButton;
import com.microntek.btmusic.helpers.Helper;
import com.microntek.btmusic.interfaces.IMusicFragmentCallbackReceiver;

import com.ag.lfm.LfmError;
import com.ag.lfm.LfmParameters;
import com.ag.lfm.LfmRequest;
import com.ag.lfm.api.LfmApi;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class MusicFragment extends Fragment implements View.OnClickListener {

    private IMusicFragmentCallbackReceiver callbackReceiver;

    private TextView songTitleTextView;
    private TextView songArtistTextView;
    private ImageView albumArtImgView;
    private ImageView albumArtReflectionImgView;

    private SimpleTarget<Drawable> albumArtDrawable;

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
        albumArtReflectionImgView = musicView.findViewById(R.id.album_art_reflection);
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
        // Make sure we can callback to our parent activity
        callbackReceiver = (IMusicFragmentCallbackReceiver) context;
    }

    public void setMusicInfo(String songArtist, String songTitle) {
        songArtistTextView.setText(songArtist);
        songTitleTextView.setText(songTitle);
    }

    public void setAlbumArt(String artist, String title) {
        // Throw away things that might fuck up the API search..
        title = title.toLowerCase().replace("- remastered","");

        // Set up last.fm API call
        LfmParameters params = new LfmParameters();
        params.put("autocorrect","1");
        params.put("artist",artist);
        params.put("track",title);

        LfmRequest request = LfmApi.track().getInfo(params);
        request.executeWithListener(new LfmRequest.LfmRequestListener() {
            @Override
            public void onComplete(JSONObject response) {
                try {
                    JSONArray imageArray = response.getJSONObject("track").getJSONObject("album").getJSONArray("image");
                    String albumArtURL = imageArray.getJSONObject(imageArray.length()-1).getString("#text");
                    Glide.with(getContext())
                        .load(albumArtURL)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                setUnknownAlbumArt();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                albumArtImgView.setImageDrawable(resource);
                                Bitmap mirrorImage = Helper.getMirroredBitmap(resource);
                                albumArtReflectionImgView.setImageBitmap(mirrorImage);
                                return false;
                            }
                        }).submit();

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
        Glide.with(getContext()).
                load(R.drawable.unknown)
               .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        setUnknownAlbumArt();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        albumArtImgView.setImageDrawable(resource);
                        Bitmap mirrorImage = Helper.getMirroredBitmap(resource);
                        albumArtReflectionImgView.setImageBitmap(mirrorImage);
                        return false;
                    }
            }).submit();
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
