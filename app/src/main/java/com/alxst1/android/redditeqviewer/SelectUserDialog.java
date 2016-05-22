package com.alxst1.android.redditeqviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alexander on 5/2/2016.
 */
public class SelectUserDialog extends DialogFragment {
    private static final String LOG_TAG = SelectUserDialog.class.getSimpleName();
    public interface SelectUserDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }
    SelectUserDialogListener mListener;

    UserListAdapter mUserListAdapter;
    Cursor mCursorUsers;
    String mLoggedInUserName;
    int mSelectedUserCursorPosition = Integer.MIN_VALUE;
    Set<Integer> mRemoveUserCursorPositions = new HashSet<>();
    List<Integer> mVisibleUserCursorPositions = new ArrayList<>();

    public enum Result {
        NoChanges,
        UserChangedToLoggedOn,
        UserChangedToAnonymous,
        NewUser
    }
    Result mResult = Result.NoChanges;

    private final int POSITION_NOT_SET = Integer.MIN_VALUE;
    private final int POSITION_ANONYMOUS = -2;
    private final int POSITION_NEW_USER = -1;

    private static final int COL_ID = 0;
    private static final int COL_USER_NAME = 1;
    private static final int COL_ACCESS_TOKEN = 2;
    private static final int COL_REFRESH_TOKEN = 3;
    private static final String[] USER_COLUMNS = {
            RedditContract.UserEntry._ID,
            RedditContract.UserEntry.COLUMN_NAME,
            RedditContract.UserEntry.COLUMN_ACCESS_TOKEN,
            RedditContract.UserEntry.COLUMN_REFRESH_TOKEN
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mCursorUsers = getContext().getContentResolver()
                .query(RedditContract.UserEntry.CONTENT_URI, USER_COLUMNS, null, null, null);
        Util.printCursorToLog(LOG_TAG, mCursorUsers);

        mLoggedInUserName = Util.getUserName(getContext());
        setVisibleUserCursorPositions();
        setLoggedInUserCursorPosition();

        mUserListAdapter = new UserListAdapter(mOnUserNameClick, mOnUserRemoveClick);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_user_dialog_title)
                .setAdapter(mUserListAdapter, null)
                .setPositiveButton(R.string.button_caption_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        commitUserSelectionChanges();
                        mListener.onDialogPositiveClick(SelectUserDialog.this);
                    }
                })
                .setNegativeButton(R.string.button_caption_cancel,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(SelectUserDialog.this);
                    }
                });
        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCursorUsers != null && !mCursorUsers.isClosed())
            mCursorUsers.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SelectUserDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SelectUserDialogListener");
        }
    }

    public Result getResult() {
        return mResult;
    }

    private void setVisibleUserCursorPositions() {
        mVisibleUserCursorPositions = new ArrayList<>();
        int cursorPosition = 0;
        if (mCursorUsers != null && mCursorUsers.moveToFirst()) {
            do {
                if (!mRemoveUserCursorPositions.contains(cursorPosition)) {
                    mVisibleUserCursorPositions.add(cursorPosition);
                } else if (cursorPosition == mSelectedUserCursorPosition) {
                    mSelectedUserCursorPosition = POSITION_ANONYMOUS;
                }
                cursorPosition++;
            }while (mCursorUsers.moveToNext());
        }
    }

    private void setLoggedInUserCursorPosition() {
        if (mLoggedInUserName == null) {
            //if there are users but no logged on user then set anonymous
            mSelectedUserCursorPosition = POSITION_ANONYMOUS;
        } else if (mCursorUsers != null && mCursorUsers.moveToFirst()) {
            int pos = 0;
            do {
                if (mLoggedInUserName.equals(mCursorUsers.getString(COL_USER_NAME))) {
                    mSelectedUserCursorPosition = pos;
                    break;
                }
                pos++;
            } while (mCursorUsers.moveToNext());
        }
    }

    private void commitUserSelectionChanges() {

        final int INVALID_ROW_ID = -1;

        int selectedUserId = INVALID_ROW_ID;
        String selUserName = null;
        String selUserAccessToken = null;
        String selUserRefreshToken = null;

        if (mSelectedUserCursorPosition >= 0 &&
                mCursorUsers.moveToPosition(mSelectedUserCursorPosition)) {
            selectedUserId = mCursorUsers.getInt(COL_ID);
            selUserName = mCursorUsers.getString(COL_USER_NAME);
            selUserAccessToken = mCursorUsers.getString(COL_ACCESS_TOKEN);
            selUserRefreshToken = mCursorUsers.getString(COL_REFRESH_TOKEN);
        }

        //Create array of row IDs for users to be removed from database
        int removeUsersCount = mRemoveUserCursorPositions.size();
        int[] removeIds = null;
        if (removeUsersCount > 0) {
            removeIds = new int[removeUsersCount];
            int i = 0;
            for (int cursorPos : mRemoveUserCursorPositions) {
                removeIds[i] = mCursorUsers.moveToPosition(cursorPos) ?
                        mCursorUsers.getInt(COL_ID) : INVALID_ROW_ID;
                //Handle case when user's radio was selected and then '-' was pressed
                //--> user selection should be canceled
                if (selectedUserId != INVALID_ROW_ID && selectedUserId == removeIds[i])
                    selectedUserId = INVALID_ROW_ID;
                i++;
            }
        }

        //Don't need cursor anymore, can be closed
        mCursorUsers.close();

        //Delete users (which were chosen to be removed) from database
        Context c = getContext();
        if (removeIds != null) {
            for (int id : removeIds) {
                c.getContentResolver()
                        .delete(RedditContract.UserEntry.CONTENT_URI, "_id=" + id, null );
            }
        }

        //Update user information in shared preferences and set value for dialog results
        mResult = Result.NoChanges;
        if (selectedUserId == INVALID_ROW_ID) {
            //*** Either new or anonymous user selected
            //***
            //Set nulls for user name, access and refresh tokens in shared prefs
            Util.resetUser(c);
            if (mSelectedUserCursorPosition == POSITION_NEW_USER)
                mResult = Result.NewUser;
            //Was anonymous user and anonymous user is selected again
            else if (selUserName == null && mLoggedInUserName == null) {
                mResult = Result.NoChanges;
            }
            else {
                mResult = Result.UserChangedToAnonymous;
                Util.restoreAnonymousUserAccessToken(c);
            }
        } else if (!selUserName.equals(mLoggedInUserName)) {
            //*** Logged in user selected different from who is already logged in
            //***
            //Restore logged on user
            Util.setUser(c, selUserName, selUserAccessToken, selUserRefreshToken);
            mResult = Result.UserChangedToLoggedOn;
        }
    }

    private View.OnClickListener mOnUserNameClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedUserCursorPosition = (int)v.getTag();
                mUserListAdapter.notifyDataSetChanged();
            }
    };

    private View.OnClickListener mOnUserRemoveClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int cursorPosition = (int)v.getTag();
            if (cursorPosition == POSITION_NOT_SET) return;
            mRemoveUserCursorPositions.add(cursorPosition);
            setVisibleUserCursorPositions();
            mUserListAdapter.notifyDataSetChanged();
        }
    };

    private class UserListAdapter extends BaseAdapter {
        View.OnClickListener mUserNameClickListener;
        View.OnClickListener mUserRemoveClickListener;
        UserListAdapter(View.OnClickListener userClickLsnr, View.OnClickListener userRemoveLsnr) {
            mUserNameClickListener = userClickLsnr;
            mUserRemoveClickListener = userRemoveLsnr;
        }
        @Override
        public int getCount() {
            int count = mVisibleUserCursorPositions.size();
            count++; //'Anonymous'
            count++; //'New user'
            return count;
        }
        @Override
        public String getItem(int position) { return null; /*not used*/}
        @Override
        public long getItemId(int position) { return 0; /*not used*/}
        @Override
        public View getView(int listPosition, View view, ViewGroup container) {
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.user_list_item,
                        container, false);
            }
            RadioButton userRadio = (RadioButton)view.findViewById(R.id.userNameRadio);
            ImageButton removeBtn = (ImageButton)view.findViewById(R.id.userNameRemove);
            userRadio.setTag(POSITION_NOT_SET);
            removeBtn.setTag(POSITION_NOT_SET);
            userRadio.setOnClickListener(mUserNameClickListener);
            removeBtn.setOnClickListener(mUserRemoveClickListener);
            int listItemsCount = getCount();
            if (listPosition < mVisibleUserCursorPositions.size()) { //Users saved in database
                int cursorPosition = mVisibleUserCursorPositions.get(listPosition);
                mCursorUsers.moveToPosition(cursorPosition);
                String userName = mCursorUsers.getString(COL_USER_NAME);
                userRadio.setText(userName);
                userRadio.setChecked(mSelectedUserCursorPosition == cursorPosition);
                userRadio.setTag(cursorPosition);
                removeBtn.setTag(cursorPosition);
                removeBtn.setVisibility(View.VISIBLE); //allow to remove user from database
            } else if (listPosition - listItemsCount == POSITION_ANONYMOUS) { //'Anonymous'
                userRadio.setText(getString(R.string.anonymous));
                userRadio.setTag(POSITION_ANONYMOUS);
                userRadio.setChecked(mSelectedUserCursorPosition == POSITION_ANONYMOUS);
                removeBtn.setVisibility(View.INVISIBLE); //cannot remove anonymous
            } else if (listPosition - listItemsCount == POSITION_NEW_USER) { //'New user'
                userRadio.setText(getString(R.string.new_user));
                userRadio.setTag(POSITION_NEW_USER);
                userRadio.setChecked(mSelectedUserCursorPosition == POSITION_NEW_USER);
                removeBtn.setVisibility(View.INVISIBLE); //cannot remove new user
            }
            return view;
        }
    }
}
