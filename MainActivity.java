package app.ruvi.privateapp;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import android.webkit.WebResourceResponse;
import android.content.Intent;
import android.net.Uri;
public class MainActivity extends Activity {
 WebView view;
 @Override public void onCreate(Bundle b){super.onCreate(b);view=new WebView(this);setContentView(view);
  WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  view.setWebChromeClient(new WebChromeClient());view.setWebViewClient(new WebViewClientCompat(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView w,Uri u){return loader.shouldInterceptRequest(u);}
   @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){Uri u=r.getUrl();if(!"https".equals(u.getScheme())&&!"http".equals(u.getScheme()))return true;startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}
  });
  view.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 @Override public void onBackPressed(){if(view.canGoBack())view.goBack();else super.onBackPressed();}
}
