package com.faiz.tempgmail;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(24, 35, 58);
    private final int muted = Color.rgb(105, 118, 140);
    private final int blue = Color.rgb(47, 105, 224);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(246, 248, 252));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 248, 252));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(30), dp(22), dp(28));
        scroll.addView(page);
        page.addView(text("✉  Temp Gmail", 27, ink, true));
        TextView subtitle = text("Real Google account • temporary-style access", 14, muted, false);
        LinearLayout.LayoutParams subLp = params(-1, -2); subLp.topMargin = dp(7); page.addView(subtitle, subLp);

        LinearLayout hero = card();
        LinearLayout.LayoutParams heroLp = params(-1, -2); heroLp.topMargin = dp(26); page.addView(hero, heroLp);
        hero.addView(text("REAL GMAIL ACCOUNT", 11, blue, true));
        TextView heading = text("Create your Gmail", 23, ink, true);
        LinearLayout.LayoutParams hLp = params(-1, -2); hLp.topMargin = dp(12); hero.addView(heading, hLp);
        TextView explain = text("Start Google's official sign-up process. Choose your address and complete any phone or identity checks Google requests.", 14, muted, false);
        LinearLayout.LayoutParams eLp = params(-1, -2); eLp.topMargin = dp(8); hero.addView(explain, eLp);
        Button create = button("Create real Gmail account", blue, Color.WHITE);
        LinearLayout.LayoutParams cLp = params(-1, dp(52)); cLp.topMargin = dp(20); hero.addView(create, cLp);
        create.setOnClickListener(v -> openInBrowser("https://accounts.google.com/signup"));

        LinearLayout inbox = card();
        LinearLayout.LayoutParams iLp = params(-1, -2); iLp.topMargin = dp(16); page.addView(inbox, iLp);
        inbox.addView(text("📥  Your Gmail inbox", 19, ink, true));
        TextView inboxDesc = text("After creating your account, sign in to Gmail to receive messages and verification codes.", 14, muted, false);
        LinearLayout.LayoutParams dLp = params(-1, -2); dLp.topMargin = dp(8); inbox.addView(inboxDesc, dLp);
        Button openInbox = button("Open Gmail inbox", Color.rgb(232, 239, 255), blue);
        LinearLayout.LayoutParams oLp = params(-1, dp(50)); oLp.topMargin = dp(16); inbox.addView(openInbox, oLp);
        openInbox.setOnClickListener(v -> openInBrowser("https://mail.google.com/"));

        LinearLayout note = card();
        LinearLayout.LayoutParams nLp = params(-1, -2); nLp.topMargin = dp(16); page.addView(note, nLp);
        note.addView(text("How temporary access works", 17, ink, true));
        TextView noteBody = text("This app opens Google's genuine account and inbox pages in a web browser. Google controls account creation and may require manual verification. Gmail accounts and emails are not automatically deleted by this app.", 13, muted, false);
        LinearLayout.LayoutParams nb = params(-1, -2); nb.topMargin = dp(9); note.addView(noteBody, nb);
        TextView footer = text("Your password and verification codes are entered only on Google pages.", 12, muted, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fLp = params(-1, -2); fLp.topMargin = dp(22); page.addView(footer, fLp);
        setContentView(scroll);
    }

    private void openInBrowser(String url) {
        Uri uri = Uri.parse(url);
        try {
            Intent chrome = new Intent(Intent.ACTION_VIEW, uri);
            chrome.addCategory(Intent.CATEGORY_BROWSABLE);
            chrome.setPackage("com.android.chrome");
            startActivity(chrome);
        } catch (Exception noChrome) {
            try {
                Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                browser.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(Intent.createChooser(browser, "Open with browser"));
            } catch (Exception noBrowser) {
                Toast.makeText(this, "Please install or enable Chrome or another web browser.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20));
        box.setBackground(bg); box.setElevation(dp(2));
        return box;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String label, int bg, int fg) {
        Button b = new Button(this); b.setText(label); b.setTextSize(15); b.setTextColor(fg);
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable shape = new GradientDrawable(); shape.setColor(bg); shape.setCornerRadius(dp(14));
        b.setBackground(shape); b.setStateListAnimator(null);
        return b;
    }
    private LinearLayout.LayoutParams params(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}