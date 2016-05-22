package com.alxst1.android.redditeqviewer;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class RedditAuthorizationActivity extends AppCompatActivity {
    private static final String LOG_TAG = RedditAuthorizationActivity.class.getSimpleName();
    public static int REQUEST_CODE_AUTHORIZATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
        setContentView(R.layout.activity_reddit_authorization);
        WebView webView = (WebView) findViewById(R.id.webView);
        if (webView == null) return;
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
        WebSettings stgs = webView.getSettings();
        stgs.setSaveFormData(false);
        String url = Constants.OAUTH_URL +
                "?client_id=" + Constants.CLIENT_ID +
                "&response_type=code" +
                "&state=" + Constants.OAUTH_STATE +
                "&redirect_uri=" + Constants.REDIRECT_URI +
                "&duration=permanent" +
                "&scope=" + Constants.OAUTH_SCOPE;
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            boolean isCodeReceived = false;
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(LOG_TAG, "shouldOverrideUrlLoading, url: " + url);
                return false;
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.i(LOG_TAG, "onPageStarted, url: " + url);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.i(LOG_TAG, "onPageFinished, url: " + url);

                if (isCodeReceived) return;
                Uri uri = Uri.parse(url);
                String code =  uri.getQueryParameter("code");
                String error = uri.getQueryParameter("error");
                boolean canCloseDialog = false;
                int activityResultCode = RESULT_CANCELED;
                if (error == null) {
                    if (code != null) {
                        String state = uri.getQueryParameter("state");
                        if (state != null && state.equals(Constants.OAUTH_STATE)) {
                            isCodeReceived = true;
                            new RedditRestClient(RedditAuthorizationActivity.this)
                                    .beginRetrievingNewUserAccessTokenAndChangingUser(code);
                            activityResultCode = RESULT_OK;
                        } else {
                            Toast.makeText(RedditAuthorizationActivity.this,
                                    "Authorization error: unexpected state '" +
                                    (state == null ? "null" : state) + "'", Toast.LENGTH_LONG)
                                    .show();
                        }
                        canCloseDialog = true;
                    }
                } else {
                    Toast.makeText(RedditAuthorizationActivity.this,
                            "Authorization error: " + error, Toast.LENGTH_LONG).show();
                    if (error.equals("access_denied")) canCloseDialog = true;
                }
                RedditAuthorizationActivity.this.setResult(activityResultCode);
                if (canCloseDialog) RedditAuthorizationActivity.this.finish();
            }
        });
    }
}
