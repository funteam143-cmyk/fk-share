package com.fk.videoplayer;

import android.app.Dialog;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    ExoPlayer player;
    PlayerView pv;
    TextView title;
    View tint;

    final String[] names = {
        "Original","Clear","Ultra Clear","Super Clear","HDR","Super HDR","Vivid",
        "Arctic Blue","Warm Glow","Deep Contrast","Cinema","Night Vision",
        "Black & White","Emerald","Sunset","Skin Tone","Cool Ice","Soft Film",
        "Crystal","Ultra HDR","Dynamic","Natural","Deep Black","Golden Film"
    };

    final int[] colors = {
        0x00000000,0x14ffffff,0x22ffffff,0x2effffff,0x1a88aaff,0x2288aaff,
        0x18ffff55,0x223366ff,0x22ff9922,0x18ffffff,0x14003300,0x2233ff33,
        0x553f3f3f,0x2222aa55,0x22ff6600,0x18ffccaa,0x2233ccff,0x14005555,
        0x20ffffff,0x2299ccff,0x12ffffaa,0x1000ff44,0x22333333,0x22ffbb55
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        pv = new PlayerView(this);
        pv.setUseController(true);
        root.addView(pv, new FrameLayout.LayoutParams(-1, -1));

        tint = new View(this);
        tint.setBackgroundColor(Color.TRANSPARENT);
        tint.setAlpha(0f);
        root.addView(tint, new FrameLayout.LayoutParams(-1, -1));

        title = new TextView(this);
        title.setText(getIntent().getStringExtra("name"));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setPadding(20, 18, 20, 10);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2);
        tp.gravity = Gravity.TOP;
        root.addView(title, tp);

        Button enh = new Button(this);
        enh.setText("HD • Visual Enhancer");
        enh.setOnClickListener(v -> showEnhancer());
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(-2, -2);
        ep.gravity = Gravity.BOTTOM | Gravity.END;
        ep.setMargins(0, 0, 18, 70);
        root.addView(enh, ep);

        setContentView(root);

        player = new ExoPlayer.Builder(this).build();
        pv.setPlayer(player);
        Uri u = getIntent().getData();
        if (u != null) {
            player.setMediaItem(MediaItem.fromUri(u));
            player.prepare();
            player.play();
        }
    }

    void showEnhancer() {
        final Dialog d = new Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 18, 24, 24);
        box.setBackgroundColor(0xff202020);

        TextView h = new TextView(this);
        h.setText("HD / Visual Enhancer");
        h.setTextColor(Color.WHITE);
        h.setTextSize(21);
        box.addView(h);

        TextView sub = new TextView(this);
        sub.setText("High-quality visual presets • source resolution is unchanged");
        sub.setTextColor(0xffbdbdbd);
        box.addView(sub);

        ScrollView sv = new ScrollView(this);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(names[i]);
            b.setTextColor(Color.WHITE);
            b.setOnClickListener(v -> {
                apply(idx);
                d.dismiss();
            });
            grid.addView(b, new LinearLayout.LayoutParams(-1, 56));
        }

        sv.addView(grid);
        box.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        d.setContentView(box);

        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        d.show();
        w = d.getWindow();
        if (w != null) {
            w.setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * .92),
                (int)(getResources().getDisplayMetrics().heightPixels * .78));
        }
    }

    void apply(int i) {
        tint.setBackgroundColor(colors[i]);
        tint.setAlpha(colors[i] == 0 ? 0f : .55f);
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        super.onDestroy();
    }
}
