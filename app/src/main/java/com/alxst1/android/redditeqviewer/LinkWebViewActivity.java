package com.alxst1.android.redditeqviewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class LinkWebViewActivity extends AppCompatActivity {
    private static final String LOG_TAG = RedditAuthorizationActivity.class.getSimpleName();

    String mUrl;
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_web_view);

        String linkTitle = getIntent().getStringExtra(Constants.EXTRA_LINK_TITLE);
        mUrl = getIntent().getStringExtra(Constants.EXTRA_LINK_URL);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (actionBar != null)
            actionBar.setSubtitle(linkTitle);

        mWebView = (WebView) findViewById(R.id.webView);
        if (mWebView == null) return;
        mWebView.loadUrl(mUrl);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                Log.i(LOG_TAG, "shouldOverrideUrlLoading, url: " + url);
                return true;
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
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            mWebView.loadUrl(mUrl);
            return true;
        } else if (id == R.id.action_open_external) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mUrl));
            startActivity(intent);
            return true;
        } else if (id == R.id.action_copy_url) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.cliboard_label), mUrl);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.clipboard_text, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.home) {

        }
        return super.onOptionsItemSelected(item);
    }

    //'Activity' override for inflating menu from xml
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.links_web_view, menu);
        return true;
    }

    public void onBackPressed() {
        super.onBackPressed();
    }
}
