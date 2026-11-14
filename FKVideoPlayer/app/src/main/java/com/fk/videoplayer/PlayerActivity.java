package com.fk.videoplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private Uri videoUri;
    private Handler handler=new Handler();
    private Runnable sleepTask;
    private boolean locked=false;
    private final int PICK_SUBTITLE=31;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        videoUri=getIntent().getData();
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        playerView=new PlayerView(this); playerView.setUseController(true); playerView.setControllerShowTimeoutMs(3500);
        root.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER); bar.setPadding(4,6,4,6); bar.setBackgroundColor(0xcc082f3a);
        String[] labels={"Speed","Audio","Subtitle","Timer","Quality","Lock"};
        for(String s:labels){Button btt=new Button(this);btt.setText(s);btt.setTextColor(Color.WHITE);btt.setTextSize(10);btt.setAllCaps(false);btt.setBackgroundColor(Color.TRANSPARENT);bar.addView(btt,new LinearLayout.LayoutParams(0,52,1));btt.setOnClickListener(v->action(s));}
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,58);bp.gravity=Gravity.BOTTOM;root.addView(bar,bp);
        setContentView(root); setupPlayer();
    }

    private void setupPlayer() {
        player=new ExoPlayer.Builder(this).build(); playerView.setPlayer(player);
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){new AlertDialog.Builder(PlayerActivity.this).setTitle("Can't play this file").setMessage("This device may not support the video's codec or audio format. EAC3 playback depends on the Android device's decoder.").setPositiveButton("OK",null).show();}
        });
        if(videoUri!=null){player.setMediaItem(MediaItem.fromUri(videoUri));player.prepare();player.play();}
    }

    private void action(String s) {
        if(player==null)return;
        switch(s){case "Speed":speed();break;case "Audio":audioTracks();break;case "Subtitle":subtitleMenu();break;case "Timer":sleepTimer();break;case "Quality":qualityInfo();break;case "Lock":locked=!locked;playerView.setUseController(!locked);Toast.makeText(this,locked?"Controls hidden":"Controls unlocked",Toast.LENGTH_SHORT).show();break;}
    }

    private void speed() {
        String[] a={"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","2.5×","3.0×","4.0×"};
        float[] f={.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f,2.5f,3f,4f};
        int selected=3;float current=player.getPlaybackParameters().speed;for(int i=0;i<f.length;i++)if(f[i]==current)selected=i;
        new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(a,selected,(d,w)->{player.setPlaybackSpeed(f[w]);d.dismiss();}).setNegativeButton("Cancel",null).show();
    }

    private void audioTracks() {
        ArrayList<String> names=new ArrayList<>();ArrayList<TrackSelectionOverride> overrides=new ArrayList<>();
        names.add("Auto / device default"); overrides.add(null);
        for(Tracks.Group group:player.getCurrentTracks().getGroups()) {
            if(group.getType()!=C.TRACK_TYPE_AUDIO)continue;
            TrackGroup tg=group.getMediaTrackGroup();
            for(int i=0;i<tg.length;i++) {
                Format f=tg.getFormat(i);String lang=f.language==null?"Unknown":f.language;
                String label=lang+"  "+(f.label==null?"Audio "+(i+1):f.label);
                if(f.channelCount>0)label+="  •  "+f.channelCount+"ch";
                names.add(label);overrides.add(new TrackSelectionOverride(tg,i));
            }
        }
        if(names.size()==1){Toast.makeText(this,"No alternate audio tracks found.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Audio track").setItems(names.toArray(new String[0]),(d,w)->{
            TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO);
            if(w>0)b.addOverride(overrides.get(w));
            player.setTrackSelectionParameters(b.build());
        }).show();
    }

    private void subtitleMenu() {
        new AlertDialog.Builder(this).setTitle("Subtitles").setItems(new String[]{"Open subtitle file (.srt / .vtt)","Turn subtitles off"},(d,w)->{
            if(w==0){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/x-subrip","text/vtt","text/plain"});startActivityForResult(i,PICK_SUBTITLE);}
            else if(videoUri!=null){long pos=player.getCurrentPosition();player.setMediaItem(MediaItem.fromUri(videoUri));player.prepare();player.seekTo(pos);player.play();}
        }).show();
    }

    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(req==PICK_SUBTITLE&&result==RESULT_OK&&data!=null&&data.getData()!=null&&videoUri!=null) {
            Uri sub=data.getData();
            try{getContentResolver().takePersistableUriPermission(sub,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            String path=sub.toString().toLowerCase(Locale.ROOT);String mime=path.endsWith(".vtt")?"text/vtt":"application/x-subrip";
            MediaItem.SubtitleConfiguration cfg=new MediaItem.SubtitleConfiguration.Builder(sub).setMimeType(mime).setLanguage("und").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
            long pos=player.getCurrentPosition();
            MediaItem item=new MediaItem.Builder().setUri(videoUri).setSubtitleConfigurations(Collections.singletonList(cfg)).build();
            player.setMediaItem(item);player.prepare();player.seekTo(pos);player.play();
        }
    }

    private void sleepTimer() {
        String[] a={"Off","5 minutes","15 minutes","30 minutes","45 minutes","60 minutes"};
        new AlertDialog.Builder(this).setTitle("Sleep timer").setItems(a,(d,w)->{
            if(sleepTask!=null)handler.removeCallbacks(sleepTask);
            if(w==0){Toast.makeText(this,"Timer off",Toast.LENGTH_SHORT).show();return;}
            long[] ms={0,300000,900000,1800000,2700000,3600000};
            sleepTask=()->{if(player!=null)player.pause();Toast.makeText(this,"Sleep timer finished",Toast.LENGTH_LONG).show();};
            handler.postDelayed(sleepTask,ms[w]);Toast.makeText(this,"Sleep timer set",Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void qualityInfo() {
        Format f=player.getVideoFormat();
        String msg=f==null?"Playing original file quality.":("Original file: "+f.width+" × "+f.height+"\nCodec: "+(f.sampleMimeType==null?"Unknown":f.sampleMimeType)+"\nLocal video quality cannot be increased beyond the source file.");
        new AlertDialog.Builder(this).setTitle("Video quality").setMessage(msg).setPositiveButton("OK",null).show();
    }

    @Override public void onBackPressed(){if(locked){locked=false;playerView.setUseController(true);}else super.onBackPressed();}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
    @Override protected void onDestroy(){if(sleepTask!=null)handler.removeCallbacks(sleepTask);if(player!=null){playerView.setPlayer(null);player.release();player=null;}super.onDestroy();}
}