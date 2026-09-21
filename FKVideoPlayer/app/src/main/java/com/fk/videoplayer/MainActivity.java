package com.fk.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private LinearLayout content;
    private final ArrayList<Video> videos = new ArrayList<>();
    private static final int PICK = 40;
    private final int GREEN = Color.rgb(0, 210, 20);
    private final int BG = Color.rgb(38, 38, 38);
    private final int BAR = Color.rgb(31, 31, 31);
    private final int CARD = Color.rgb(72, 72, 72);
    private final int TEXT = Color.rgb(245, 245, 245);
    private final int MUTED = Color.rgb(160, 160, 160);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BAR);
        getWindow().setNavigationBarColor(BG);
        build();
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 9);
        } else {
            load();
        }
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(32, 18, 22, 10);
        top.setBackgroundColor(BAR);

        TextView title = text("Video", 28, TEXT);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, 70, 1));

        TextView cast = text("▱", 34, TEXT); cast.setGravity(Gravity.CENTER);
        TextView search = text("⌕", 42, TEXT); search.setGravity(Gravity.CENTER);
        TextView more = text("⋮", 34, TEXT); more.setGravity(Gravity.CENTER);
        top.addView(cast, lp(58, 70)); top.addView(search, lp(58, 70)); top.addView(more, lp(38, 70));
        more.setOnClickListener(v -> showMenu(root));
        search.setOnClickListener(v -> { });
        root.addView(top);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(32, 18, 20, 18);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        chips.addView(chip("◷  All Videos", true), lp(0, 58, 1));
        chips.addView(space(10));
        chips.addView(chip("⇩  Downloader", false));
        chips.addView(space(10));
        chips.addView(chip("▣  Private", false));
        chipsScroll.addView(chips);
        root.addView(chipsScroll);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(10, 6, 10, 8);
        bottom.setBackgroundColor(BAR);
        bottom.addView(nav("▣", "Video", true), new LinearLayout.LayoutParams(0, 82, 1));
        bottom.addView(nav("♫", "Music", false), new LinearLayout.LayoutParams(0, 82, 1));
        bottom.addView(nav("☷", "Playlist", false), new LinearLayout.LayoutParams(0, 82, 1));
        root.addView(bottom);

        LinearLayout ad = new LinearLayout(this);
        ad.setGravity(Gravity.CENTER_VERTICAL);
        ad.setPadding(28, 8, 18, 8);
        ad.setBackgroundColor(Color.rgb(48,48,48));
        TextView adText = text("Best Screen Recorder\nRecord videos & games in high quality!", 17, TEXT);
        adText.setTypeface(null, android.graphics.Typeface.BOLD);
        ad.addView(adText, new LinearLayout.LayoutParams(0, 72, 1));
        TextView install = text("Install", 18, Color.WHITE);
        install.setGravity(Gravity.CENTER);
        install.setTypeface(null, android.graphics.Typeface.BOLD);
        install.setBackground(round(GREEN, 50));
        ad.addView(install, lp(150, 58));
        root.addView(ad);

        setContentView(root);
    }

    private void load() {
        videos.clear();
        String[] p = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATA};
        try (Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                p, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c != null) while (c.moveToNext()) {
                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                videos.add(new Video(c.getLong(0), c.getString(1), c.getLong(2), path));
            }
        } catch (Exception ignored) {}
        render();
    }

    private void render() {
        content.removeAllViews();
        addRecent();
        addFolders();
    }

    private void addRecent() {
        TextView head = text("Recent videos", 20, TEXT);
        head.setPadding(36, 12, 20, 8);
        head.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(head);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(32, 0, 20, 14);
        int n = Math.min(videos.size(), 6);
        for (int i=0;i<n;i++) {
            Video v=videos.get(i);
            row.addView(videoCard(v), new LinearLayout.LayoutParams(300, 175));
            if(i<n-1) row.addView(space(12));
        }
        if(n==0) {
            TextView e=text("No videos found",17,MUTED);
            e.setPadding(30,30,30,30); row.addView(e);
        }
        hs.addView(row); content.addView(hs);
    }

    private View videoCard(Video v) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        ImageView img=new ImageView(this); img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(Color.rgb(55,55,55));
        loadThumb(img,v.id);
        box.addView(img,new LinearLayout.LayoutParams(-1,145));
        TextView dur=text("▶  "+fmt(v.dur),15,Color.WHITE);
        dur.setGravity(Gravity.CENTER_VERTICAL);
        dur.setPadding(12,0,12,0);
        dur.setBackgroundColor(0xaa000000);
        box.addView(dur,new LinearLayout.LayoutParams(-1,42));
        box.setOnClickListener(x->open(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+v.id),v.name));
        return box;
    }

    private void loadThumb(ImageView image,long id) {
        try {
            Uri u=Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,""+id);
            if(Build.VERSION.SDK_INT>=29) image.setImageBitmap(getContentResolver().loadThumbnail(u,new Size(600,350),null));
        } catch(Exception ignored){}
    }

    private void addFolders() {
        TextView head=text(videos.size()>0 ? folderCount()+" FOLDERS" : "FOLDERS",20,TEXT);
        head.setPadding(36, 16, 20, 10);
        content.addView(head);
        LinkedHashMap<String,Integer> map=new LinkedHashMap<>();
        for(Video v:videos) {
            String f=folderName(v.path);
            Integer old=map.get(f); map.put(f,old==null?1:old+1);
        }
        String[] preferred={"Recent Added","213","Camera","Download","Facebook","Instagram","Private","Restored","Sent","SnapTube Video","WhatsApp Video","Directories"};
        for(String f:preferred) if(map.containsKey(f) || f.equals("Recent Added")) addFolder(f, f.equals("Recent Added")?Math.min(videos.size(),8):map.get(f)==null?0:map.get(f), f.equals("Download"));
        for(Map.Entry<String,Integer> e:map.entrySet()) {
            boolean known=false; for(String p:preferred) if(p.equals(e.getKey())) known=true;
            if(!known) addFolder(e.getKey(),e.getValue(),false);
        }
    }

    private int folderCount() {
        java.util.HashSet<String> s=new java.util.HashSet<>();
        for(Video v:videos) s.add(folderName(v.path));
        return Math.max(1,s.size());
    }

    private void addFolder(String name,int count,boolean isNew) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(34,6,28,6);
        TextView icon=text("▰",38,Color.rgb(105,105,105)); icon.setGravity(Gravity.CENTER);
        row.addView(icon,lp(70,72));
        TextView label=text(name+"  "+count,20,TEXT);
        row.addView(label,new LinearLayout.LayoutParams(0,72,1));
        if(isNew){ TextView n=text("NEW",14,Color.WHITE); n.setTypeface(null,android.graphics.Typeface.BOLD); n.setGravity(Gravity.CENTER); n.setBackgroundColor(Color.RED); row.addView(n,lp(58,34)); }
        TextView dots=text("⋮",30,Color.rgb(125,125,125)); dots.setGravity(Gravity.CENTER);
        row.addView(dots,lp(42,72));
        content.addView(row);
    }

    private String folderName(String path) {
        if(path==null||path.length()==0) return "Directories";
        path=path.replace('\\','/');
        String[] a=path.split("/");
        return a.length>1?a[a.length-2]:"Directories";
    }

    private TextView nav(String ico,String label,boolean active) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER);
        TextView i=text(ico,28,active?GREEN:MUTED); i.setGravity(Gravity.CENTER);
        TextView l=text(label,15,active?GREEN:MUTED); l.setGravity(Gravity.CENTER);
        box.addView(i,lp(50,38)); box.addView(l,lp(90,30)); return box;
    }

    private TextView chip(String s,boolean active) {
        TextView v=text(s,20,active?TEXT:TEXT); v.setGravity(Gravity.CENTER);
        v.setPadding(18,0,18,0); v.setBackground(round(active?CARD:Color.rgb(72,72,72),60)); return v;
    }

    private void showMenu(View anchor) {
        final android.app.AlertDialog d=new android.app.AlertDialog.Builder(this)
            .setItems(new String[]{"Network Stream","Theme","Widgets","Refresh","Equalizer","Remove Ads","Settings"},(x,w)->{
                if(w==3) load();
            }).create();
        d.show();
    }

    private TextView text(String s,float size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private GradientDrawable round(int c,float r){ GradientDrawable g=new GradientDrawable(); g.setColor(c); g.setCornerRadius(r); return g; }
    private LinearLayout.LayoutParams lp(int w,int h){ return new LinearLayout.LayoutParams(w,h); }
    private LinearLayout.LayoutParams lp(int w,int h,float weight){ return new LinearLayout.LayoutParams(w,h,weight); }
    private View space(int w){ Space s=new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(w,1)); return s; }

    private void open(Uri u,String name){ Intent i=new Intent(this,PlayerActivity.class); i.setData(u); i.putExtra("name",name); startActivity(i); }
    private String fmt(long ms){ long sec=ms/1000; return String.format(Locale.US,"%02d:%02d:%02d",(sec/3600),(sec/60)%60,sec%60); }

    static class Video{long id,dur;String name,path;Video(long i,String n,long d,String p){id=i;name=n;dur=d;path=p;}}
    @Override protected void onResume(){super.onResume();if(content!=null)load();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);load();}
}
