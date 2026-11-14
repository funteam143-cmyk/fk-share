package com.fkfaceai.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import android.content.ClipData;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
  private WebView webView;
  private ValueCallback<Uri[]> uploadMessage;
  private static final int FILE_REQUEST = 501;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    webView = new WebView(this);
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setMediaPlaybackRequiresUserGesture(true);
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (uploadMessage != null) uploadMessage.onReceiveValue(null);
        uploadMessage = callback;
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        String[] accepted = params.getAcceptTypes();
        ArrayList<String> mimeTypes = new ArrayList<>();
        if (accepted != null) {
          for (String type : accepted) {
            if (type == null) continue;
            for (String part : type.split(",")) {
              String clean = part.trim();
              if (clean.startsWith(".")) continue;
              if (clean.contains("/")) mimeTypes.add(clean);
            }
          }
        }
        if (!mimeTypes.isEmpty()) picker.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
        try {
          startActivityForResult(picker, FILE_REQUEST);
          return true;
        } catch (Exception first) {
          try {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("*/*");
            if (!mimeTypes.isEmpty()) fallback.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
            fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            startActivityForResult(Intent.createChooser(fallback, "Select photo or video"), FILE_REQUEST);
            return true;
          } catch (Exception second) {
            uploadMessage = null;
            callback.onReceiveValue(null);
            Toast.makeText(MainActivity.this, "No file picker found", Toast.LENGTH_LONG).show();
            return false;
          }
        }
      }
    });
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl("file:///android_asset/index.html");
    setContentView(webView);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != FILE_REQUEST || uploadMessage == null) return;
    Uri[] results = null;
    if (resultCode == RESULT_OK && data != null) {
      if (data.getClipData() != null) {
        ClipData clip = data.getClipData();
        results = new Uri[clip.getItemCount()];
        for (int i = 0; i < clip.getItemCount(); i++) results[i] = clip.getItemAt(i).getUri();
      } else if (data.getData() != null) {
        results = new Uri[]{data.getData()};
      }
    }
    uploadMessage.onReceiveValue(results);
    uploadMessage = null;
  }

  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) webView.goBack();
    else super.onBackPressed();
  }

  @Override protected void onDestroy() {
    if (uploadMessage != null) { uploadMessage.onReceiveValue(null); uploadMessage = null; }
    if (webView != null) { webView.destroy(); webView = null; }
    super.onDestroy();
  }
}