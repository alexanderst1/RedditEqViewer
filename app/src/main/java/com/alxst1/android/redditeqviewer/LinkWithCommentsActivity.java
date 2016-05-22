package com.alxst1.android.redditeqviewer;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkWithCommentsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = LinkWithCommentsActivity.class.getSimpleName();
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private String mSubreddit;
    private int mLinkCount;
    private int mLinkPosition;
    private Cursor mCursorLinks;

    private static final int LINKS_PAGER_LOADER = 0;

    static final int COL_LINK_ROWID = 0;
    static final int COL_POSITION = 1;
    static final int COL_ID = 2;
    static final int COL_TITLE = 3;
    static final int COL_DOMAIN = 4;
    static final int COL_AUTHOR = 5;
    static final int COL_SCORE = 6;
    static final int COL_NUM_COMMENTS = 7;
    static final int COL_POST_HINT = 8;
    static final int COL_STICKIED = 9;
    static final int COL_OVER_18 = 10;
    static final int COL_AUTHOR_FL_TEXT = 11;
    static final int COL_SLFTXT_HTML = 12;
    static final int COL_SLFTXT = 13;
    static final int COL_CREATED_UTC = 14;
    static final int COL_URL = 15;
    static final int COL_IMG_PORT = 16;
    static final int COL_IMG_PORT_WIDTH = 17;
    static final int COL_IMG_PORT_HEIGHT = 18;
    static final int COL_IMG_LAND = 19;
    static final int COL_IMG_LAND_WIDTH = 20;
    static final int COL_IMG_LAND_HEIGHT = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_with_comments);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mSubreddit = intent.getStringExtra(Constants.EXTRA_SUBREDDIT_NAME);
        mLinkCount = intent.getIntExtra(Constants.EXTRA_LINK_COUNT, 0);
        mLinkPosition = intent.getIntExtra(Constants.EXTRA_LINK_POSITION, 0);

        getLoaderManager().initLoader(LINKS_PAGER_LOADER, null, this);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        //Will set adapter when cursor finished loading data (in cursor loader onfinish event)
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.setCurrentItem(mLinkPosition);
        }
    }

    public void onBackPressed() {
        int position = mViewPager.getCurrentItem();
        if (position >= 0 && position < mLinkCount) {
            Util.updateSubredditPositionInDb(LinkWithCommentsActivity.this, mSubreddit,
                    position);
        }
        super.onBackPressed();
    }

    public void onLinkClick (View view) {
        if (mCursorLinks == null || mCursorLinks.isClosed() || !mCursorLinks.moveToPosition(mViewPager.getCurrentItem()))
            return;
        String url = mCursorLinks.getString(COL_URL);
        if (!url.startsWith(Constants.HTTP_PREFIX))
            return;
        Intent intent = new Intent(this, LinkWebViewActivity.class);
        intent.putExtra(Constants.EXTRA_LINK_TITLE, mCursorLinks.getString(COL_TITLE));
        intent.putExtra(Constants.EXTRA_LINK_URL, mCursorLinks.getString(COL_URL));
        startActivity(intent);
    }

    private void refreshComments() {
        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_link_with_comments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_refresh_comments) {
            refreshComments();
        }
        return super.onOptionsItemSelected(item);
    }

    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == LINKS_PAGER_LOADER) {
            return new CursorLoader(this,
                    RedditContract.LinkEntry.buildUriWithSubpath(mSubreddit),
                    null /*String[] projection*/,
                    null /*String selection*/,
                    null /*String[] selectionArgs*/,
                    null /*sort order*/);
        }
        return null;
    }

    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LINKS_PAGER_LOADER) {
            mCursorLinks = data;
            if (mViewPager != null) {
                mSectionsPagerAdapter.notifyDataSetChanged();
            }
        }
    }

    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LINKS_PAGER_LOADER) {
            mCursorLinks = null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        static Cursor mCursor;
        static Context mContext;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int position, Bundle bundle, Cursor cursor,
                                                      Context context) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = (Bundle) bundle.clone();
            args.putInt(Constants.EXTRA_LINK_POSITION, position);
            fragment.setArguments(args);
            mCursor = cursor;
            mContext = context;
            return fragment;
        }

        private void bindView(View rootView) {
            int pos = getArguments().getInt(Constants.EXTRA_LINK_POSITION);

            if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(pos))
                return;

            int cnt = getArguments().getInt(Constants.EXTRA_LINK_COUNT);
            String subreddit = getArguments().getString(Constants.EXTRA_SUBREDDIT_NAME);

            ((TextView) rootView.findViewById(R.id.pager_subreddit)).setText(
                    Util.getSubredditNameWithR(mContext, subreddit));

            ((TextView) rootView.findViewById(R.id.pager_domain))
                    .setText(mCursor.getString(COL_DOMAIN));
            ((TextView) rootView.findViewById(R.id.pager_title))//Just in case remove HTML entities
                    .setText(Html.fromHtml(mCursor.getString(COL_TITLE)));
            ((TextView) rootView.findViewById(R.id.pager_author))
                    .setText(Util.bold(mCursor.getString(COL_AUTHOR)));
            ((TextView) rootView.findViewById(R.id.pager_when))
                    .setText(Util.getRelativeLocalTimeFromUTCtime(mCursor.getInt(COL_CREATED_UTC)));
            ((TextView) rootView.findViewById(R.id.pager_position))
                    .setText(Util.getLinkPositionString(mContext, pos, cnt));

            ImageView imgView = (ImageView) rootView.findViewById(R.id.pager_img);
            int imgWidth;
            int imgHeight;
            String imgUrl;
            if (mContext.getResources().getConfiguration()
                    .orientation == Configuration.ORIENTATION_PORTRAIT) {
                imgWidth = mCursor.getInt(COL_IMG_PORT_WIDTH);
                imgHeight = mCursor.getInt(COL_IMG_PORT_HEIGHT);
                imgUrl = mCursor.getString(COL_IMG_PORT);
            } else {
                imgWidth = mCursor.getInt(COL_IMG_LAND_WIDTH);
                imgHeight = mCursor.getInt(COL_IMG_LAND_HEIGHT);
                imgUrl = mCursor.getString(COL_IMG_LAND);
            }
            if (imgUrl != null && !imgUrl.isEmpty() &&
                    imgUrl.startsWith(Constants.HTTP_PREFIX)) {
                int screenWidth = Util.getDisplaySize(mContext).x;
                imgView.setVisibility(View.VISIBLE);
                RequestCreator rc = Picasso.with(mContext).load(imgUrl);
                if (screenWidth > 0) {
                    rc = rc.resize(screenWidth, screenWidth * imgHeight / imgWidth);
                    // rc = rc.centerCrop();
                }
                rc.into(imgView);
            } else {
                imgView.setVisibility(View.GONE);
            }
            TextView slfDescView = (TextView) rootView.findViewById(R.id.pager_slftxt);
            String slfText = mCursor.getString(COL_SLFTXT);
            //Will not use html, many tags show up as plain text. Need something more
            //sophisticated than Html.fromHtml...
            String slfHtml = null;
            //slfHtml = mCursor.getString(COL_SLFTXT_HTML);
            //slfHtml.replaceAll("&lt;.*?div.*?&gt;", "");
            if (slfHtml != null && !slfHtml.isEmpty() && !slfHtml.equals("null")) {
                slfDescView.setVisibility(View.VISIBLE);
                slfDescView.setText(Html.fromHtml(slfHtml));
            } else if (slfText != null && !slfText.isEmpty() && !slfText.equals("null")) {
                slfDescView.setVisibility(View.VISIBLE);
                slfDescView.setText(Html.fromHtml(slfText)); //Just in case remove HTML entities
            } else {
                slfDescView.setVisibility(View.GONE);
            }

            int numComment = mCursor.getInt(COL_NUM_COMMENTS);
            ((TextView) rootView.findViewById(R.id.pager_num_comments))
                    .setText(String.format(mContext.getString(R.string.num_commments),
                            numComment));
            ((TextView) rootView.findViewById(R.id.pager_score))
                    .setText(Util.bold(mCursor.getString(COL_SCORE)));
            final TextView cmntTree = (TextView) rootView.findViewById(R.id.comment_tree);
            if (numComment > 0) {
                cmntTree.setText(mContext.getString(R.string.loading_comments));
            }

            String linkId = mCursor.getString(COL_ID);
            bindCommentsToView(cmntTree, subreddit, linkId);
        }

        private static void bindCommentsToView(final TextView cmntTree,
                                        String subreddit, String linkId) {
            new RedditRestClient(mContext).beginRetrievingComments(subreddit, linkId,
                    new RedditRestClient.JsonResultHandler() {
                        @Override
                        public void onSuccess(JSONObject response) {
                        }
                        public void onSuccess(JSONArray response) {
                            try {
                                SpannableStringBuilder text = new SpannableStringBuilder();
                                JSONArray comments = response.getJSONObject(1).getJSONObject("data")
                                        .getJSONArray("children");
                                for (int i = 0; i < comments.length(); i++) {
                                    JSONObject cmnt = comments.getJSONObject(i).getJSONObject("data");
                                    text.append(getComment(2, cmnt));
                                }
                                text.append("\n\n");
                                cmntTree.setText(text);
                            } catch (JSONException e) {
                                Log.e(LOG_TAG, e.getMessage(), e);
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onFailure(int errorCode) {
                        }
                        private CharSequence getComment(int level, JSONObject comment) {
                            SpannableStringBuilder text = new SpannableStringBuilder();
                            try {
                                if (!comment.isNull("author")) {
                                    text.append(Util.underline(
                                            Util.bold(comment.getString("author") + " "),
                                            Util.italic(Util.getRelativeLocalTimeFromUTCtime(
                                            (long) comment.getInt("created_utc")) + " "),
                                            Util.bold(comment.getString("score"))));
                                    text.append('\n');
                                    text.append(Html.fromHtml(comment.getString("body")));
                                    text.append('\n');
                                    if (!comment.isNull("replies") &&
                                            (comment.get("replies") instanceof JSONObject)) {
                                        JSONObject replies = comment.getJSONObject("replies");
                                        if (!replies.isNull("data")) {
                                            JSONObject data = replies.getJSONObject("data");
                                            if (!data.isNull("children")) {
                                                JSONArray children = data.getJSONArray("children");
                                                for (int i = 0; i < children.length(); i++) {
                                                    text.append(getComment(level + 1,
                                                            children.getJSONObject(i)
                                                                    .getJSONObject("data")));
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e(LOG_TAG, e.getMessage(), e);
                                e.printStackTrace();
                            }
                            return Util.indent(level * 4, text);
                            //return Util.indentDrawable(mContext, level * 4, text);
                        }
                    });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_link_with_comments, container,
                    false);
            bindView(rootView);
            return rootView;
        }
    }
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position, getIntent().getExtras(), mCursorLinks,
                    LinkWithCommentsActivity.this);
        }
        @Override
        public int getCount() {
            return mLinkCount;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}
