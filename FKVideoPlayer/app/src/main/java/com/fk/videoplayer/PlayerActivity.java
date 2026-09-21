package com.fk.videoplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public class PlayerActivity extends Activity {
    private ExoPlayer player; private PlayerView pv; private FrameLayout root; private View tint;
    private final String[] filters={"Original","Clear","Ultra Clear","Super Clear","HDR","HDR+","Super HDR","Vivid","Dynamic","Cinema","Movie","Deep Contrast","Bright","Warm","Cool","Arctic","Sunset","Emerald","Skin Tone","Soft Film","Sharp","Crystal Clear","Night","Black & White","Vintage","Golden","AMOLED","Deep Black"};
    private final int[] filterColors={0,0x18ffffff,0x22ffffff,0x30ffffff,0x2288aaff,0x2a88ccff,0x3299ddff,0x18ffff55,0x16ffffff,0x14003300,0x14002244,0x20ffffff,0x18ffffbb,0x18ff9922,0x183388ff,0x223366ff,0x22ff6600,0x2222aa55,0x18ffccaa,0x14005555,0x22ffffff,0x22aaddff,0x2233ff33,0x553f3f3f,0x223f2f1f,0x22ffbb55,0x22330044,0x22333333};
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        pv=new PlayerView(this);pv.setUseController(true);root.addView(pv,new FrameLayout.LayoutParams(-1,-1));
        tint=new View(this);tint.setBackgroundColor(Color.TRANSPARENT);tint.setAlpha(0);root.addView(tint,new FrameLayout.LayoutParams(-1,-1));
        TextView title=label(getIntent().getStringExtra("name"),16,Color.WHITE);title.setPadding(20,16,20,8);FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2);tp.gravity=Gravity.TOP;root.addView(title,tp);
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.VERTICAL);tools.setPadding(8,4,8,10);tools.setBackgroundColor(0xcc000000);
        String[][] rows={{"🔒","Lock"},{"◷","Sleep Timer"},{"1×","Playback Speed"},{"≋","Equalizer"},{"▣","Subtitles"},{"▤","Video Track"},{"♪","Audio Track"},{"✦","Visual Enhancer"},{"HD","Quality"},{"⟳","Rotate"},{"▣","Screenshot"},{"⋮","More"}};
        for(String[] r:rows){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER);Button q=tool(r[0],r[1]);line.addView(q,new LinearLayout.LayoutParams(0,62,1));tools.addView(line);}
        FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-1,420);wp.gravity=Gravity.BOTTOM;root.addView(tools,wp);
        setContentView(root);setupPlayer();
        for(int i=0;i<rows.length;i++){/* listeners assigned below by button text */ }
        int idx=0; for(int i=0;i<tools.getChildCount();i++){LinearLayout line=(LinearLayout)tools.getChildAt(i);Button q=(Button)line.getChildAt(0);String s=q.getTag().toString();q.setOnClickListener(v->handle(s));idx++;}
    }
    private Button tool(String icon,String name){Button b=new Button(this);b.setText(icon+"\n"+name);b.setTextColor(Color.WHITE);b.setTextSize(11);b.setAllCaps(false);b.setTag(name);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private TextView label(String s,float z,int c){TextView t=new TextView(this);t.setText(s==null?"FK Video Player":s);t.setTextSize(z);t.setTextColor(c);return t;}
    private void setupPlayer(){
        player=new ExoPlayer.Builder(this).build();pv.setPlayer(player);
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){Toast.makeText(PlayerActivity.this,"This audio/video format is not supported on this device.",Toast.LENGTH_LONG).show();}});
        Uri u=getIntent().getData();if(u==null){String s=getIntent().getStringExtra("uri");if(s!=null)u=Uri.parse(s);}
        if(u!=null)try{player.setMediaItem(MediaItem.fromUri(u));player.prepare();player.play();}catch(Exception e){Toast.makeText(this,"Unable to open this video.",Toast.LENGTH_LONG).show();}
    }
    private void handle(String s){
        switch(s){
            case "Playback Speed": speed();break;
            case "Equalizer": equalizer();break;
            case "Visual Enhancer": enhancer();break;
            case "Quality": quality();break;
            case "Subtitles": subtitles();break;
            case "Audio Track": tracks(true);break;
            case "Video Track": tracks(false);break;
            case "More": more();break;
            case "Rotate": setRequestedOrientation(getResources().getConfiguration().orientation==1?0:1);break;
            case "Screenshot": Toast.makeText(this,"Screenshot option selected.",Toast.LENGTH_SHORT).show();break;
            case "Sleep Timer": sleep();break;
            case "Lock": Toast.makeText(this,"Controls locked.",Toast.LENGTH_SHORT).show();break;
            default: break;
        }
    }
    private void speed(){String[] a={"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};new AlertDialog.Builder(this).setTitle("Playback Speed").setSingleChoiceItems(a,3,(d,w)->{player.setPlaybackSpeed(new float[]{.25f,.5f,.75f,1f,1.25f,1.5f,1.75f,2f}[w]);d.dismiss();}).show();}
    private void quality(){String[] a={"Auto","144p","240p","360p","480p","720p HD","1080p Full HD","Source / Original"};new AlertDialog.Builder(this).setTitle("Quality / HD").setItems(a,(d,w)->Toast.makeText(this,a[w]+" selected",Toast.LENGTH_SHORT).show()).show();}
    private void tracks(boolean audio){int n=audio?player.getCurrentTracks().getGroups().size():player.getCurrentTracks().getGroups().size();new AlertDialog.Builder(this).setTitle(audio?"Audio Track":"Video Track").setItems(new String[]{"Default","Track 1","Track 2","Hindi","English"},(d,w)->Toast.makeText(this,"Track selected",Toast.LENGTH_SHORT).show()).show();}
    private void subtitles(){new AlertDialog.Builder(this).setTitle("Subtitles").setItems(new String[]{"Current subtitle","Open file","Online download","AI generate PRO","Auto-generate subtitles","Hindi","English","Translate subtitles"},(d,w)->Toast.makeText(this,"Subtitle option selected",Toast.LENGTH_SHORT).show()).show();}
    private void equalizer(){final LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(25,20,25,20);TextView h=label("Equalizer",22,Color.WHITE);box.setBackgroundColor(0xff202020);box.addView(h);String[] f={"60Hz","230Hz","910Hz","3.6kHz","14kHz"};for(String x:f){SeekBar s=new SeekBar(this);box.addView(label(x,13,Color.LTGRAY));box.addView(s);}new AlertDialog.Builder(this).setView(box).setPositiveButton("Done",null).show();}
    private void enhancer(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(18,12,18,12);box.setBackgroundColor(0xff202020);TextView h=label("HD / Visual Enhancer",21,Color.WHITE);box.addView(h);ScrollView sv=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<filters.length;i++){final int k=i;Button b=new Button(this);b.setText(filters[i]);b.setTextColor(Color.WHITE);b.setOnClickListener(v->{apply(k);});list.addView(b,new LinearLayout.LayoutParams(-1,54));}sv.addView(list);box.addView(sv,new LinearLayout.LayoutParams(-1,0,1));new AlertDialog.Builder(this).setView(box).setPositiveButton("Close",null).show();}
    private void apply(int i){tint.setBackgroundColor(filterColors[i]);tint.setAlpha(i==0?0:.55f);}
    private void sleep(){new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(new String[]{"Off","15 minutes","30 minutes","45 minutes","60 minutes","End of video"},null).show();}
    private void more(){new AlertDialog.Builder(this).setTitle("More Options").setItems(new String[]{"Share","Hide","Add to Playlist","Details","Delete"},(d,w)->Toast.makeText(this,"Option selected",Toast.LENGTH_SHORT).show()).show();}
    @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
    @Override protected void onDestroy(){if(player!=null){pv.setPlayer(null);player.release();player=null;}super.onDestroy();}
}