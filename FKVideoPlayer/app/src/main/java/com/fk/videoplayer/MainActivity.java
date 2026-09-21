package com.fk.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout content; private final ArrayList<Video> videos=new ArrayList<>();
    private final int GREEN=Color.rgb(0,190,25),TEXT=Color.rgb(45,45,45),MUTED=Color.rgb(155,155,155);
    private boolean inFolder=false;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.WHITE);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);build();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO},9);else load();}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(28,10,16,4);
        TextView title=text("Video",28,TEXT);title.setTypeface(null,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,72,1));
        TextView cast=text("▱",31,TEXT),search=text("⌕",40,TEXT),more=text("⋮",32,TEXT);for(TextView t:new TextView[]{cast,search,more})t.setGravity(Gravity.CENTER);
        top.addView(cast,lp(54,72));top.addView(search,lp(54,72));top.addView(more,lp(42,72));more.setOnClickListener(v->showMenu());search.setOnClickListener(v->showSearch());root.addView(top);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(Color.WHITE);nav.setPadding(8,4,8,4);nav.addView(navItem("▣","Video",true),weight());nav.addView(navItem("♫","Music",false),weight());nav.addView(navItem("☷","Playlist",false),weight());root.addView(nav,new LinearLayout.LayoutParams(-1,82));
        LinearLayout ad=new LinearLayout(this);ad.setGravity(Gravity.CENTER_VERTICAL);ad.setPadding(28,4,18,4);TextView at=text("Glitch Video Effect\n100+ VHS, Glitch effects & filters.",16,Color.DKGRAY);ad.addView(at,new LinearLayout.LayoutParams(0,62,1));TextView ins=text("Install",18,Color.WHITE);ins.setGravity(Gravity.CENTER);ins.setTypeface(null,Typeface.BOLD);ins.setBackground(round(GREEN,60));ad.addView(ins,lp(140,54));root.addView(ad,new LinearLayout.LayoutParams(-1,72));setContentView(root);
    }
    private void load(){videos.clear();String[] p={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.DATA};try(Cursor c=getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,p,null,null,MediaStore.Video.Media.DATE_ADDED+" DESC")){if(c!=null)while(c.moveToNext())videos.add(new Video(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3)));}catch(Exception ignored){}renderRoot();}
    private void renderRoot(){inFolder=false;content.removeAllViews();addRecent();addFolders();}
    private void addRecent(){TextView h=text("Recent Added",20,TEXT);h.setTypeface(null,Typeface.BOLD);h.setPadding(38,14,20,8);content.addView(h);HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(36,0,20,14);int n=Math.min(6,videos.size());for(int i=0;i<n;i++){row.addView(videoCard(videos.get(i)),new LinearLayout.LayoutParams(330,190));if(i<n-1)row.addView(space(12));}hs.addView(row);content.addView(hs);}
    private View videoCard(Video v){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setBackgroundColor(0xffdddddd);loadThumb(im,v.id);box.addView(im,new LinearLayout.LayoutParams(-1,150));TextView d=text("▶ "+fmt(v.dur),15,Color.WHITE);d.setGravity(Gravity.CENTER_VERTICAL);d.setPadding(12,0,12,0);d.setBackgroundColor(0xaa000000);box.addView(d,lp(-1,40));box.setOnClickListener(x->open(v));return box;}
    private void addFolders(){TextView h=text("13 FOLDERS",20,TEXT);h.setPadding(40,18,20,8);content.addView(h);LinkedHashMap<String,Integer> m=new LinkedHashMap<>();for(Video v:videos){String f=folderName(v.path);m.put(f,m.containsKey(f)?m.get(f)+1:1);}String[] pref={"Recent Added","213","Camera","Download","Facebook","Instagram","Private","Restored","Screen Recording","Sent","SnapTube Video","WhatsApp Video","Directories"};for(String f:pref){int c=m.containsKey(f)?m.get(f):0;if(c>0||f.equals("Recent Added"))addFolder(f,c,f.equals("Download"));}for(Map.Entry<String,Integer> e:m.entrySet()){boolean known=false;for(String f:pref)if(f.equals(e.getKey()))known=true;if(!known)addFolder(e.getKey(),e.getValue(),false);}}
    private void addFolder(String name,int count,boolean isNew){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(36,2,24,2);TextView ic=text("▰",39,folderColor(name));ic.setGravity(Gravity.CENTER);row.addView(ic,lp(66,72));LinearLayout mid=new LinearLayout(this);mid.setGravity(Gravity.CENTER_VERTICAL);mid.addView(text(name,20,TEXT));mid.addView(text("  "+count,16,Color.LTGRAY));row.addView(mid,new LinearLayout.LayoutParams(0,72,1));if(isNew){TextView n=text("NEW",13,Color.WHITE);n.setGravity(Gravity.CENTER);n.setTypeface(null,Typeface.BOLD);n.setBackground(round(Color.RED,8));row.addView(n,lp(54,30));}TextView dots=text("⋮",28,0xffaaaaaa);dots.setGravity(Gravity.CENTER);row.addView(dots,lp(38,72));row.setOnClickListener(v->showFolder(name));content.addView(row);}
    private void showFolder(String folder){inFolder=true;content.removeAllViews();LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(16,4,12,4);TextView back=text("‹",40,TEXT);back.setGravity(Gravity.CENTER);head.addView(back,lp(48,62));TextView t=text(folder,24,TEXT);t.setTypeface(null,Typeface.BOLD);head.addView(t,new LinearLayout.LayoutParams(0,62,1));head.addView(text("⌕",34,TEXT),lp(48,62));head.addView(text("⋮",30,TEXT),lp(40,62));content.addView(head);back.setOnClickListener(v->renderRoot());TextView chips=text("All     ▣ Video     ▧ Image",15,Color.WHITE);chips.setGravity(Gravity.CENTER);chips.setBackground(round(GREEN,40));content.addView(chips,new LinearLayout.LayoutParams(-2,42));int found=0;for(Video v:videos)if(folderName(v.path).equals(folder)){addVideoRow(v);found++;}if(found==0){TextView e=text("No videos in this folder",18,MUTED);e.setGravity(Gravity.CENTER);content.addView(e,new LinearLayout.LayoutParams(-1,180));}}
    private void addVideoRow(Video v){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(20,5,16,5);ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);loadThumb(im,v.id);row.addView(im,lp(150,88));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(16,0,8,0);info.addView(text(v.name,17,TEXT));info.addView(text(fmt(v.dur),14,MUTED));row.addView(info,new LinearLayout.LayoutParams(0,92,1));row.addView(text("⋮",28,0xff888888),lp(40,92));row.setOnClickListener(x->open(v));content.addView(row);}
    private void showMenu(){new AlertDialog.Builder(this).setTitle("FK Video Player").setItems(new String[]{"Network Stream","Theme","Widgets","Refresh","Equalizer","Remove Ads","Settings"},(d,w)->{if(w==3)load();else if(w==1)new AlertDialog.Builder(this).setTitle("Theme").setItems(new String[]{"Light","Dark"},null).show();else if(w==4)Toast.makeText(this,"Open a video for Equalizer.",Toast.LENGTH_SHORT).show();}).show();}
    private void showSearch(){final EditText e=new EditText(this);e.setHint("Search videos");new AlertDialog.Builder(this).setTitle("Search").setView(e).setPositiveButton("Search",(d,w)->filter(e.getText().toString())).setNegativeButton("Cancel",null).show();}
    private void filter(String q){if(q.trim().isEmpty()){renderRoot();return;}content.removeAllViews();for(Video v:videos)if(v.name.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))addVideoRow(v);}
    private void open(Video v){Intent i=new Intent(this,PlayerActivity.class);i.setData(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+v.id));i.putExtra("name",v.name);startActivity(i);}
    private void loadThumb(ImageView im,long id){try{Uri u=Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+id);if(Build.VERSION.SDK_INT>=29)im.setImageBitmap(getContentResolver().loadThumbnail(u,new Size(600,350),null));}catch(Exception ignored){}}
    private String folderName(String p){if(p==null||p.isEmpty())return "Directories";String[] a=p.replace('\\','/').split("/");return a.length>1?a[a.length-2]:"Directories";}
    private int folderColor(String n){if(n.equals("Download"))return 0xff27b34b;if(n.equals("Instagram"))return 0xffd81b8a;if(n.equals("Screen Recording"))return 0xffe53935;if(n.equals("Camera"))return 0xffffb300;return 0xff448aff;}
    private LinearLayout navItem(String i,String l,boolean a){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setGravity(Gravity.CENTER);TextView x=text(i,28,a?GREEN:MUTED),y=text(l,15,a?GREEN:MUTED);x.setGravity(Gravity.CENTER);y.setGravity(Gravity.CENTER);b.addView(x,lp(60,38));b.addView(y,lp(90,30));return b;}
    private TextView text(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
    private GradientDrawable round(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,82,1);}
    private View space(int w){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(w,1));return s;}
    private String fmt(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",s/3600,(s/60)%60,s%60);}
    @Override protected void onResume(){super.onResume();if(content!=null&&!inFolder)load();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);load();}
    static class Video{long id,dur;String name,path;Video(long i,String n,long d,String p){id=i;name=n;dur=d;path=p;}}
}