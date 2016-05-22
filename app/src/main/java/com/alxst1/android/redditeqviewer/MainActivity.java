package com.alxst1.android.redditeqviewer;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alxst1.android.redditeqviewer.RedditContract.AnonymSubscrEntry;
import com.alxst1.android.redditeqviewer.RedditContract.LinkEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditEntry;
import com.alxst1.android.redditeqviewer.RedditContract.SubredditSearchEntry;

public class MainActivity extends AppCompatActivity
        implements SelectUserDialog.SelectUserDialogListener,
        LoaderManager.LoaderCallbacks<Cursor>, PopupMenu.OnMenuItemClickListener  {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private SubredditListAdapter mSubrdtListAdapter;
    private LinkListAdapter mLinkListAdapter;
    private ListView mSubredditListView;
    private ListView mLinkListView;
    private EditText mSearchBox;
    private ImageButton mStartOrCancelSearch;
    private SwipeRefreshLayout mSwipeRefreshLayoutSubreddit;
    private SwipeRefreshLayout mSwipeRefreshLayoutLink;

    private static final int SUBREDDIT_LIST_LOADER = 0;
    private static final int LINK_LIST_LOADER = 1;
    private int mSubredditListPos = ListView.INVALID_POSITION;
    private int mLinkListPos = ListView.INVALID_POSITION;

    private static String mSearchString;
    private boolean mIsRefreshingSubreddits = false;
    private boolean mIsRefreshingLinks = false;

    private Boolean mIsLoaderInLoggedInUserMode = null;
    private Boolean mIsLoaderInSearchResultMode = null;

    //Data for list of subreddits on drawer
    static final int COL_SUBREDDIT_ROWID = 0;
    public static final int COL_SUBREDDIT_NAME = 1;
    public static final int COL_IS_SUBSCRIBER = 2;
    public static final String[] LOGGEDON_SUBREDDIT_LIST_COLUMNS = {
            SubredditEntry._ID,
            SubredditEntry.COLUMN_NAME,
            SubredditEntry.COLUMN_USER_IS_SUBSCRIBER
    };
    private static final String[] LOGGEDON_SUBREDDIT_SEARCH_LIST_COLUMNS = {
            SubredditSearchEntry._ID,
            SubredditSearchEntry.COLUMN_NAME,
            /*
            * (select count(*) from subreddits where subreddits.name = search.name and
            * subreddits.user_is_subscriber = 1) as user_is_subscriber
            * */
            "(select count(*) from " + SubredditEntry.TABLE_NAME + " where " +
                    SubredditEntry.TABLE_NAME + "." + SubredditEntry.COLUMN_NAME + "=" +
                    SubredditSearchEntry.TABLE_NAME + "." + SubredditSearchEntry.COLUMN_NAME +
                    " and  " + SubredditEntry.TABLE_NAME + "." +
                    SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + "=1) as " +
                    SubredditEntry.COLUMN_USER_IS_SUBSCRIBER
    };
    private static final String[] ANONYM_SUBREDDIT_SEARCH_LIST_COLUMNS = {
            SubredditSearchEntry._ID,
            SubredditSearchEntry.COLUMN_NAME,
            /*
            * (select count(*) from subscr where subscr.name = search.name) as user_is_subscriber
            * */
            "(select count(*) from " + AnonymSubscrEntry.TABLE_NAME + " where " +
                    AnonymSubscrEntry.TABLE_NAME + "." + AnonymSubscrEntry.COLUMN_NAME + "=" +
                    SubredditSearchEntry.TABLE_NAME + "." + SubredditSearchEntry.COLUMN_NAME +
                    ") as " + SubredditEntry.COLUMN_USER_IS_SUBSCRIBER
    };
    public static final String SUBREDDITS_DEFAULT_SORT_ORDER =
            SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + " desc";

    //Data for list of links on main_activity layout
    //should match order of columns in RedditProvider.query(), under "CASE LINK"
    static final int COL_LINK_ROWID = 0;
    static final int COL_POSITION = 1;
    static final int COL_COUNT = 2;
    static final int COL_ID = 3;
    static final int COL_TITLE = 4;
    static final int COL_DOMAIN = 5;
    static final int COL_AUTHOR = 6;
    static final int COL_SCORE = 7;
    static final int COL_NUM_COMMENTS = 8;
    static final int COL_SUBREDDIT = 9;
    static final int COL_CREATED_UTC = 10;
    static final int COL_THUMBNAIL = 11;
    static final int COL_SAVED_POSITION = 12;
    static final int COL_MAX_SCORE = 13;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constants.NEW_USER_EVENT)) {
                updateNavigationViewHeader();
                new RedditRestClient(MainActivity.this).beginRetrievingMySubreddits();
            } else if (action.equals(Constants.SUBREDDITS_RETRIEVED_EVENT)) {
                refreshLinks();
            } else if (action.equals(Constants.MY_SUBREDDITS_RETRIEVED_EVENT)) {
                new RedditRestClient(MainActivity.this).beginRetrievingSubreddits(
                        RedditRestClient.SubrdtDisplayOrder.DEFAULT);
            } else if (action.equals(Constants.LINKS_RETRIEVED_EVENT)) {

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
            toggle.syncState();
            //Add drawer listener for testing purpose to be able to call
            //test function for creating test users for testing 'Select User' dialog.
            drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                }
                @Override
                public void onDrawerOpened(View drawerView) {
                    //Util.createUserSelectionDialogTestData(this);
                }
                @Override
                public void onDrawerClosed(View drawerView) {
                }
                @Override
                public void onDrawerStateChanged(int newState) {
                }
            });
        }

        mSearchBox = (EditText)findViewById(R.id.search_box);
        mStartOrCancelSearch = (ImageButton)findViewById(R.id.start_search);

        //Register broadcast receiver for receiving notification when user name is retrieved from
        //Reddit.com to be able to update user name in navigation drawer header
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.NEW_USER_EVENT);
        filter.addAction(Constants.SUBREDDITS_RETRIEVED_EVENT);
        filter.addAction(Constants.MY_SUBREDDITS_RETRIEVED_EVENT);
        filter.addAction(Constants.LINKS_RETRIEVED_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        //Set up cursor adapter for list of subreddits on navigation drawer
        mSubredditListView = (ListView)findViewById(R.id.subredditList);
        mSubrdtListAdapter = new SubredditListAdapter(this, null, 0);
        if (mSubredditListView != null)
            mSubredditListView.setAdapter(mSubrdtListAdapter);
        getLoaderManager().initLoader(SUBREDDIT_LIST_LOADER, null, this);

        //Set up cursor adapter for list of links on main_activity layout
        mLinkListView = (ListView)findViewById(R.id.link_list);
        mLinkListAdapter = new LinkListAdapter(this, mLinkListView, null, 0);
        if (mLinkListView != null) {
            mLinkListView.setAdapter(mLinkListAdapter);
            mLinkListView.setDividerHeight(4);
            mLinkListView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mLinkListAdapter.getGestureDetector().onTouchEvent(event);
                    return mLinkListView.onTouchEvent(event);
                }
            });
        }
        getLoaderManager().initLoader(LINK_LIST_LOADER, null, this);

        //On navigation drawer change user name and image for log in/out button depending on user's
        //logon status
        updateNavigationViewHeader();

        //Set up 'Swipe-refresh' layout listeners / color scheme for refreshing indicator and
        //action on swipe-refreshSubreddits
        mSwipeRefreshLayoutSubreddit =
                (SwipeRefreshLayout)findViewById(R.id.swipe_refresh_layout_subreddit);
        if (mSwipeRefreshLayoutSubreddit != null ) {
            mSwipeRefreshLayoutSubreddit.setColorSchemeResources(R.color.colorAccent2,
                    R.color.colorAccent, R.color.colorPrimary);
            mSwipeRefreshLayoutSubreddit.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshSubreddits();
                }
            });
        }

        mSwipeRefreshLayoutLink = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout_link);
        if (mSwipeRefreshLayoutLink != null ) {
            mSwipeRefreshLayoutLink.setColorSchemeResources(R.color.colorAccent2,
                    R.color.colorAccent, R.color.colorPrimary);
            mSwipeRefreshLayoutLink.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshLinks();
                }
            });
        }

        //Set up listeners and action for 'Search' text box and 'Start Search' buttons
        //('Search' button on onscreen keyboard and '>' button to the right of 'Search' text box)
        mSearchBox.setOnEditorActionListener(mOnEditorActionListener);

        mStartOrCancelSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsLoaderInSearchResultMode != null && mIsLoaderInSearchResultMode) {
                    //Quite search results mode
                    setSubredditLoaderModeAndRestartLoaderIfRequired(false, null);
                    updateSearchUI(false);
                } else { //Start new search
                    mOnEditorActionListener.onEditorAction(mSearchBox,
                            EditorInfo.IME_ACTION_SEARCH, null);
                }
            }
        });

        refreshSubredditsIfRequired();
    }

    private ListView getLinkListView() {
        return mLinkListView;
    }
        /**
         * Listener responsible for closing onscreen keyboard and starting search once user clicked
         * on 'Start Search' button (on onscreen keyboard)
         */
    TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide virtual keyboard
                InputMethodManager imm =
                        (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);

                mSearchString = v.getText().toString();
                beginSearchForSubreddit();
                handled = true;
            }
            return handled;
        }
    };

    public static String getSearchString() {
        return mSearchString;
    }

    private void updateSearchUI(boolean isSearchResultsMode) {
        if (isSearchResultsMode) {
            mStartOrCancelSearch.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_highlight_off_black_24dp));
            mSearchBox.selectAll();

        } else {
            mStartOrCancelSearch.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_keyboard_arrow_right_black_24dp));
            mSearchBox.setSelection(0);
            mSearchString = null;
        }
    }

    /**
     * Begin searching for subreddits which names start with search string
     */
    private void beginSearchForSubreddit() {
        if (mSearchString == null || mSearchString.length() == 0) {
            Toast.makeText(MainActivity.this, getString(R.string.empty_search_string),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        getContentResolver().delete(SubredditSearchEntry.CONTENT_URI, null, null);
        new RedditRestClient(this).beginSearchRedditNames(mSearchString,
                new RedditRestClient.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        //Show toast with number of matches found
                        int numMatches = Util.getCount(MainActivity.this,
                                SubredditSearchEntry.CONTENT_URI, null, null);
                        Toast.makeText(MainActivity.this,
                                String.format(getString(R.string.search_success_message),
                                        numMatches), Toast.LENGTH_SHORT).show();
                        //Switch loader to show search results
                        setSubredditLoaderModeAndRestartLoaderIfRequired(true, null);
                        updateSearchUI(true);
                    }
                    @Override
                    public void onFailure(int errorCode) {
                        //Show toast that search failed
                        Toast.makeText(MainActivity.this,
                                String.format(getString(R.string.search_failure_message),
                                        errorCode), Toast.LENGTH_SHORT).show();
                        //Switch loader to show regular list of subreddits
                        setSubredditLoaderModeAndRestartLoaderIfRequired(false, null);
                        updateSearchUI(false);
                    }
                });
    }

    private void setSubredditLoaderModeAndRestartLoaderIfRequired(Boolean isSearchResultsMode,
                                                                  Boolean isLoggedInUserMode) {
        boolean isRestartRequired = false;
        if (isSearchResultsMode != null && mIsLoaderInSearchResultMode != isSearchResultsMode) {
            mIsLoaderInSearchResultMode = isSearchResultsMode;
            isRestartRequired = true;
        }
        if (isLoggedInUserMode != null && (mIsLoaderInLoggedInUserMode == null ||
                mIsLoaderInLoggedInUserMode != isLoggedInUserMode)) {
            mIsLoaderInLoggedInUserMode = isLoggedInUserMode;
            isRestartRequired = true;
        }
        if (isRestartRequired) {
            getLoaderManager().restartLoader(SUBREDDIT_LIST_LOADER, null, MainActivity.this);
        }
    }

    /**
     * Override from 'FragmentActivity'
     * If drawer is open and user pressed 'Back' it will close drawer instead of exiting activity
     */
    @Override
    public void onBackPressed() {
        if (mIsLoaderInSearchResultMode != null && mIsLoaderInSearchResultMode) {
            //Switch loader to show regular list of subreddits
            setSubredditLoaderModeAndRestartLoaderIfRequired(false, null);
            updateSearchUI(false);
        } else {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }

    private void refreshSubredditsIfRequired() {
        //Retrieve list of subreddits when list is empty
        Util.getCount(this, SubredditEntry.CONTENT_URI, null, null);
        if (0 >= Util.getCount(this, SubredditEntry.CONTENT_URI, null, null)) {
            refreshSubreddits();
        }
    }

    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == SUBREDDIT_LIST_LOADER) {
            String[] projection = null;
            String sortOrder = null;
            Uri uri = null;

            boolean isLoggedIn = Util.isLoggedIn(this);
            if (mIsLoaderInLoggedInUserMode != null)
                isLoggedIn = mIsLoaderInLoggedInUserMode;
            boolean isSearchResults = false;
            if (mIsLoaderInSearchResultMode != null)
                isSearchResults = mIsLoaderInSearchResultMode;

            if (isLoggedIn) {
                projection = LOGGEDON_SUBREDDIT_LIST_COLUMNS;
                sortOrder = SUBREDDITS_DEFAULT_SORT_ORDER;
                uri = SubredditEntry.CONTENT_URI;
            } else {
                uri = SubredditEntry.buildUriWithSubpath(RedditContract.SUBPATH_ANONYMOUS);
            }

            if (isSearchResults) {
                uri = SubredditSearchEntry.CONTENT_URI;
                sortOrder = null;
                if (isLoggedIn) {
                    projection = LOGGEDON_SUBREDDIT_SEARCH_LIST_COLUMNS;
                } else {
                    projection = ANONYM_SUBREDDIT_SEARCH_LIST_COLUMNS;
                }
            }
            return new CursorLoader(this, uri, projection, null /*String selection*/,
                    null /*String[] selectionArgs*/, sortOrder);
        } else if (i == LINK_LIST_LOADER) {
            return new CursorLoader(this,
                    LinkEntry.buildUriWithSubpath(RedditContract.SUB_PATH_LINKS_TOP_N), null,
                    null /*String selection*/,
                    null /*String[] selectionArgs*/, null);
        }
        return null;
    }


    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Util.printCursorToLog(LOG_TAG, data);
        int id = loader.getId();
        if (id == SUBREDDIT_LIST_LOADER) {
            mSubrdtListAdapter.swapCursor(data);
            if (mSubredditListPos != ListView.INVALID_POSITION) {
                mSubredditListView.smoothScrollToPosition(mSubredditListPos);
            }
            //Hide looping-circle indicator
            mIsRefreshingSubreddits = false;
            updateRefreshingUIsubreddits();
        } else if (id == LINK_LIST_LOADER) {
            mLinkListAdapter.swapCursor(data);
            if (mLinkListPos != ListView.INVALID_POSITION) {
                mLinkListView.smoothScrollToPosition(mSubredditListPos);
            }
            mIsRefreshingLinks = false;
            updateRefreshingUIlinks();
        }
    }

    //Override method from LoaderManager.LoaderCallbacks<Cursor>
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int id = loader.getId();
        if (id == SUBREDDIT_LIST_LOADER) {
            mSubrdtListAdapter.swapCursor(null);
            mIsRefreshingSubreddits = false;
            updateRefreshingUIsubreddits();
        } else if (id == LINK_LIST_LOADER) {
            mLinkListAdapter.swapCursor(null);
            mIsRefreshingLinks = false;
            updateRefreshingUIlinks();
        }
    }

    /**
     * Change user name and image for log in/out button depending on user's logon status
     */
    private void updateNavigationViewHeader() {
        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
        if (nv == null) return;
        TextView tvUserName = (TextView)nv.findViewById(R.id.loginOrUserName);
        ImageButton btnLogInOrOut = (ImageButton)nv.findViewById(R.id.logInOrOut);
        String userName = Util.getUserName(this);
        if (userName == null) {
            tvUserName.setText(getString(R.string.log_in));
            btnLogInOrOut.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.login));
        } else {
            tvUserName.setText(userName);
            btnLogInOrOut.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.logout));
        }
    }

    //'Activity' override for inflating menu from xml
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return true;
    }

    //'Activity' override for inflating menu from xml
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
        } else if (id == R.id.action_refresh_links) {
            refreshLinks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * In response to click on 'LogIn/LogOut' button on navigation drawer, opens dialog
     * for selecting user
     * @param v - not used
     */
    public void onLogInOrOutButtonPressed(View v) {
        DialogFragment dialog = new SelectUserDialog();
        dialog.show(getSupportFragmentManager(), "SelectUserDialog");
    }

    /**
     * In response to click on button 'Action Overflow' (3 vertical dots) on navigation drawer,
     * pops up menu with one item 'Settings'
     * @param v
     */
    public void onDrawerSettingsButtonPressed(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.drawer_actions);
        popup.show();
    }

    /**
     * Override from PopupMenu.OnMenuItemClickListener. Handles click on 'Refresh' item in
     * navigation drawer action overflow menu.
     * @param item
     * @return
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshSubreddits();
                return true;
            default:
                return false;
        }
    }

    /**
     * Enables 'looping circle' refreshing indicator and initiate retrieving list of user's
     * subreddits from Reddit.com
     */
    private void refreshSubreddits() {
        if (mIsRefreshingSubreddits)
            return;
        mIsRefreshingSubreddits = true;
        updateRefreshingUIsubreddits();

        if (mIsLoaderInSearchResultMode != null && mIsLoaderInSearchResultMode) {
            beginSearchForSubreddit();
        } else {
            Util.deleteSubredditsFromDb(this);
            if (Util.isLoggedIn(this)) {
                new RedditRestClient(this).beginRetrievingMySubreddits();
            } else { //Anonymous
                new RedditRestClient(this).beginRetrievingSubreddits(
                        RedditRestClient.SubrdtDisplayOrder.DEFAULT);
            }

        }
    }

    private void refreshLinks() {
        if (mIsRefreshingLinks)
            return;
        mIsRefreshingLinks = true;
        updateRefreshingUIlinks();

        Util.deleteLinksFromDb(this);
        Util.deleteCurrLinksFromDb(this);

        ContentResolver cr = getContentResolver();
        Cursor cur;
        if (Util.isLoggedIn(this)) {
            cur = cr.query(SubredditEntry.CONTENT_URI, new String[] {SubredditEntry.COLUMN_NAME},
                    SubredditEntry.COLUMN_USER_IS_SUBSCRIBER + "=?", new String[] {"1"}, null);
        } else {
            cur = cr.query(AnonymSubscrEntry.CONTENT_URI,
                    new String[] {AnonymSubscrEntry.COLUMN_NAME}, null, null, null);
        }

        if (cur == null || cur.getCount() == 0 || !cur.moveToFirst()) {
            mIsRefreshingLinks = false;
            updateRefreshingUIlinks();
            if (cur != null)
                cur.close();
        } else {
            RedditRestClient rrc = new RedditRestClient(this);
            do {
                String subreddit = cur.getString(0);
                rrc.beginRetrievingLinks(subreddit, RedditRestClient.LinkDisplayOrder.TOP);
                //break;
            } while (cur.moveToNext());
        }
        if (cur != null && !cur.isClosed())
            cur.close();
    }

    /**
     * Enabled or disabled 'looping circle' refreshing indicator
     */
    private void updateRefreshingUIsubreddits() {
        mSwipeRefreshLayoutSubreddit.setRefreshing(mIsRefreshingSubreddits);
    }

    private void updateRefreshingUIlinks() {
        mSwipeRefreshLayoutLink.setRefreshing(mIsRefreshingLinks);
    }

    /**
     * Handle click on 'OK' button on 'Select User' dialog
     * @param dialog
     */
    public void onDialogPositiveClick(DialogFragment dialog) {
        SelectUserDialog.Result res = ((SelectUserDialog)dialog).getResult();
        if (res == SelectUserDialog.Result.NoChanges) {
            return;
        }

        if (res == SelectUserDialog.Result.NewUser) {
            Intent intent = new Intent(this, RedditAuthorizationActivity.class);
            startActivityForResult(intent, RedditAuthorizationActivity.REQUEST_CODE_AUTHORIZATION);
            return;
        }

        updateNavigationViewHeader();
        updateSearchUI(false);
        setSubredditLoaderModeAndRestartLoaderIfRequired(false,
                res == SelectUserDialog.Result.UserChangedToLoggedOn);
        Util.deleteSubredditsFromDb(this);

        if (res == SelectUserDialog.Result.UserChangedToLoggedOn) {
            new RedditRestClient(this).beginRetrievingUserName(null);
        } else { //UserChangedToAnonymous
            new RedditRestClient(this).beginRetrievingSubreddits(
                    RedditRestClient.SubrdtDisplayOrder.DEFAULT);
        }
    }

    /**
     * Handles results from Authorization activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == RedditAuthorizationActivity.REQUEST_CODE_AUTHORIZATION) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, getString(R.string.authoriz_failed),
                        Toast.LENGTH_LONG).show();
            } else {
                updateNavigationViewHeader();
                updateSearchUI(false);
                setSubredditLoaderModeAndRestartLoaderIfRequired(false, true);
                Util.deleteSubredditsFromDb(this);
            }
        }
    }

    /**
     * Handle click on 'Cancel' button on 'Select User' dialog
     * @param dialog
     */
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

    /**
     * Override from 'Activity'. Unregister receiver which is used for updating navigation drawer
     * header with user name of logged on user once it is retrieved from Reddit.com
     */
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
