package com.fk.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    LinearLayout list;
    final int PICK = 40;
    ArrayList<Video> videos = new ArrayList<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 9);
        } else {
            load();
        }
    }

    void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff171717);

        TextView bar = t("FK Video Player", 22, Color.WHITE);
        bar.setPadding(24, 30, 24, 22);
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(16, 0, 16, 12);
        Button pick = btn("＋ Add Videos");
        pick.setOnClickListener(v -> startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("video/*")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
                PICK));
        actions.addView(pick, new LinearLayout.LayoutParams(-1, 52));
        root.addView(actions);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    void load() {
        videos.clear();
        String[] p = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
        };
        try (Cursor c = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                p, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    videos.add(new Video(c.getLong(0), c.getString(1), c.getLong(2)));
                }
            }
        }
        render();
    }

    void render() {
        list.removeAllViews();
        if (videos.isEmpty()) {
            TextView e = t("No videos found\nTap + Add Videos to choose a video", 17, 0xffaaaaaa);
            e.setGravity(Gravity.CENTER);
            e.setPadding(20, 80, 20, 80);
            list.addView(e);
            return;
        }

        for (Video v : videos) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(18, 18, 18, 18);
            TextView n = t(v.name + "\n" + fmt(v.dur), 17, Color.WHITE);
            row.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
            Button play = btn("PLAY");
            play.setOnClickListener(x -> open(
                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "" + v.id),
                    v.name));
            row.addView(play);
            list.addView(row);
        }
    }

    void open(Uri u, String name) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.setData(u);
        i.putExtra("name", name);
        startActivity(i);
    }

    TextView t(String s, int z, int c) {
        TextView x = new TextView(this);
        x.setText(s);
        x.setTextSize(z);
        x.setTextColor(c);
        return x;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    String fmt(long ms) {
        long sec = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    static class Video {
        long id, dur;
        String name;
        Video(long i, String n, long d) { id = i; name = n; dur = d; }
    }

    @Override protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (r == PICK && c == RESULT_OK && d != null) load();
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        load();
    }
}
