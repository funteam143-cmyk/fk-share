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
    private LinearLayout content;
    private final ArrayList<Video> videos = new ArrayList<>();
    private final int INK=0xff12323a, TEAL=0xff008f91, MUTED=0xff71858a, BG=0xfff4fafa;
    private boolean inFolder=false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        build();
        requestVideoPermission();
    }

    private void requestVideoPermission() {
        String p = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{p}, 9);
        else load();
    }

    private void build() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(22,8,14,4);
        LinearLayout brand=new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("Apna Video Player",25,INK); title.setTypeface(null,Typeface.BOLD);
        TextView sub=text("Your videos, your way",12,MUTED);
        brand.addView(title); brand.addView(sub);
        top.addView(brand,new LinearLayout.LayoutParams(0,68,1));
        TextView search=text("⌕",36,TEAL); search.setGravity(Gravity.CENTER);
        top.addView(search,lp(48,64)); search.setOnClickListener(v->showSearch());
        TextView menu=text("⋮",30,INK); menu.setGravity(Gravity.CENTER);
        top.addView(menu,lp(40,64)); menu.setOnClickListener(v->showMenu());
        root.addView(top);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.addView(content);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(Color.WHITE);
        nav.addView(navItem("▣","Videos",true),weight());
        nav.addView(navItem("▤","Folders",false),weight());
        nav.addView(navItem("◷","History",false),weight());
        root.addView(nav,new LinearLayout.LayoutParams(-1,70));
        setContentView(root);
    }

    private void load() {
        videos.clear();
        String[] p={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.DATA};
        try(Cursor c=getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,p,null,null,MediaStore.Video.Media.DATE_ADDED+" DESC")) {
            if(c!=null) while(c.moveToNext()) videos.add(new Video(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3)));
        } catch(Exception ignored) {}
        renderRoot();
    }

    private void renderRoot() {
        inFolder=false; content.removeAllViews();
        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(20,18,20,18);
        hero.setBackground(round(0xffe0f3f1,22));
        TextView h=text("Your video library",21,INK); h.setTypeface(null,Typeface.BOLD);
        TextView count=text(videos.size()+" videos found on this device",14,MUTED);
        hero.addView(h); hero.addView(count); content.addView(hero,margin(-1,-2,16,10,16,10));
        TextView recent=text("Recently added",19,INK); recent.setTypeface(null,Typeface.BOLD); recent.setPadding(20,14,20,8); content.addView(recent);
        if(videos.isEmpty()) empty("No videos found","Allow video access, then tap Refresh.");
        for(int i=0;i<Math.min(5,videos.size());i++) addVideoRow(videos.get(i));
        TextView folders=text("Folders",19,INK); folders.setTypeface(null,Typeface.BOLD); folders.setPadding(20,18,20,8); content.addView(folders);
        LinkedHashMap<String,Integer> map=new LinkedHashMap<>();
        for(Video v:videos){String f=folderName(v.path);map.put(f,map.containsKey(f)?map.get(f)+1:1);}
        for(Map.Entry<String,Integer> e:map.entrySet()) addFolder(e.getKey(),e.getValue());
    }

    private void addFolder(String name,int count) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(18,4,18,4);
        TextView icon=text("▰",28,folderColor(name)); icon.setGravity(Gravity.CENTER); row.addView(icon,lp(52,58));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        TextView n=text(name,16,INK); n.setTypeface(null,Typeface.BOLD);
        info.addView(n); info.addView(text(count+" videos",12,MUTED));
        row.addView(info,new LinearLayout.LayoutParams(0,58,1));
        TextView arrow=text("›",30,MUTED); row.addView(arrow);
        row.setOnClickListener(v->showFolder(name));
        content.addView(row);
    }

    private void showFolder(String folder) {
        inFolder=true; content.removeAllViews();
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); head.setPadding(14,4,14,4);
        TextView back=text("‹",36,TEAL); back.setGravity(Gravity.CENTER); head.addView(back,lp(42,58));
        TextView name=text(folder,21,INK); name.setTypeface(null,Typeface.BOLD); head.addView(name);
        content.addView(head); back.setOnClickListener(v->renderRoot());
        int found=0;
        for(Video v:videos) if(folderName(v.path).equals(folder)){addVideoRow(v);found++;}
        if(found==0) empty("Folder is empty","No videos are available here.");
    }

    private void addVideoRow(Video v) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(16,7,16,7);
        ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); im.setBackground(round(0xffdce8e8,10)); loadThumb(im,v.id);
        row.addView(im,lp(116,72));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(13,0,6,0);
        TextView name=text(v.name,15,INK); name.setTypeface(null,Typeface.BOLD);
        info.addView(name); info.addView(text(fmt(v.dur)+"  •  "+folderName(v.path),12,MUTED));
        row.addView(info,new LinearLayout.LayoutParams(0,76,1));
        TextView play=text("▶",20,TEAL); row.addView(play,lp(34,60));
        row.setOnClickListener(x->open(v)); content.addView(row);
    }

    private void empty(String title,String message) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(20,36,20,36);
        TextView t=text(title,18,INK); t.setTypeface(null,Typeface.BOLD); box.addView(t);
        TextView m=text(message,13,MUTED); box.addView(m);
        content.addView(box,new LinearLayout.LayoutParams(-1,-2));
    }

    private void showMenu() {
        new AlertDialog.Builder(this).setTitle("Apna Video Player")
          .setItems(new String[]{"Refresh library","App information"},(d,w)->{
              if(w==0)load();
              else new AlertDialog.Builder(this).setTitle("Apna Video Player").setMessage("Offline video player with local folders and Media3 playback. EAC3 audio depends on device decoder support.").setPositiveButton("OK",null).show();
          }).show();
    }

    private void showSearch() {
        EditText e=new EditText(this); e.setSingleLine(true); e.setHint("Search video name");
        new AlertDialog.Builder(this).setTitle("Search videos").setView(e)
          .setPositiveButton("Search",(d,w)->filter(e.getText().toString())).setNegativeButton("Cancel",null).show();
    }

    private void filter(String q) {
        content.removeAllViews();
        if(q.trim().isEmpty()){renderRoot();return;}
        int n=0;
        for(Video v:videos) if(v.name.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))){addVideoRow(v);n++;}
        if(n==0)empty("No matches","Try another video name.");
    }

    private void open(Video v) {
        Intent i=new Intent(this,PlayerActivity.class);
        i.setData(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+v.id));
        i.putExtra("name",v.name); startActivity(i);
    }

    private void loadThumb(ImageView im,long id) {
        try { Uri u=Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+id);
            if(Build.VERSION.SDK_INT>=29) im.setImageBitmap(getContentResolver().loadThumbnail(u,new Size(500,300),null));
        } catch(Exception ignored) {}
    }

    private String folderName(String p) {
        if(p==null||p.isEmpty())return "Other";
        String[] a=p.replace('\\','/').split("/");
        return a.length>1?a[a.length-2]:"Other";
    }
    private int folderColor(String n){if(n.toLowerCase(Locale.ROOT).contains("download"))return 0xff199c63;if(n.toLowerCase(Locale.ROOT).contains("instagram"))return 0xffbf4d93;if(n.toLowerCase(Locale.ROOT).contains("screen"))return 0xffdf6655;if(n.toLowerCase(Locale.ROOT).contains("camera"))return 0xffbd8a25;return TEAL;}
    private LinearLayout navItem(String i,String l,boolean a){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setGravity(Gravity.CENTER);TextView x=text(i,23,a?TEAL:MUTED),y=text(l,12,a?TEAL:MUTED);x.setGravity(Gravity.CENTER);y.setGravity(Gravity.CENTER);b.addView(x);b.addView(y);return b;}
    private TextView text(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,70,1);}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private String fmt(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",s/3600,(s/60)%60,s%60);}
    @Override protected void onResume(){super.onResume();if(content!=null&&!inFolder)load();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==9&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)load();else if(r==9)empty("Permission needed","Allow video access in Settings to scan your library.");}
    static class Video{long id,dur;String name,path;Video(long i,String n,long d,String p){id=i;name=n;dur=d;path=p;}}
}