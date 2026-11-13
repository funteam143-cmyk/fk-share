package com.fkfaceai.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;

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
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (uploadMessage != null) uploadMessage.onReceiveValue(null);
        uploadMessage = callback;
        try { startActivityForResult(params.createIntent(), FILE_REQUEST); return true; }
        catch (Exception e) { uploadMessage = null; Toast.makeText(MainActivity.this, "File picker unavailable", Toast.LENGTH_SHORT).show(); return false; }
      }
    });
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl("file:///android_asset/index.html");
    setContentView(webView);
  }
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_REQUEST && uploadMessage != null) {
      Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
      uploadMessage.onReceiveValue(result); uploadMessage = null;
    }
  }
  @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
