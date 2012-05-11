/**
 * 
 */
package com.roylaurie.arkown.android;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.roylaurie.arkown.android.User;
/**
 * @author rlaurie
 *
 */
public final class Application {
    private User mUser = null;
    private Context mContext = null;
    private boolean mIsFinishing = false;
    private int mPullIntervalSeconds = DEFAULT_PULL_INTERVAL; 
    private LinkedList<Issue> mIssueList = new LinkedList<Issue>();
    
    public final static Uri HELP_URI = Uri.parse("http://www.roylaurie.com/ArkOwn_Guide");
    public final static Uri MARKET_DETAILS_URI = Uri.parse("market://details?id=com.roylaurie.arkown.android.app");
    //public final static Uri MARKET_DETAILS_URI = Uri.parse("market://details?id=com.roylaurie.arkown.mobile.android.app");

    public final static int REQUEST_CODE_EDIT_SERVER = 101;
    public final static int REQUEST_CODE_EDIT_COMMAND_CATEGORY = 103;
    public final static int REQUEST_CODE_EDIT_COMMAND = 104;
    public final static int REQUEST_CODE_PROXY_TERMS = 105;
    
    public static final int DEFAULT_PULL_INTERVAL = 30; // seconds       
    
    private static Application sInstance = null;
    
    public static enum Issue {
        UPGRADE;
    }
    
 	private Application(Context context) {
 	    mContext = context;
	}
	
	public static Application getInstance() {
		return Application.sInstance;
	}
	
	public static Application initialize(Context context) {
	    if (sInstance != null) {
	        throw new IllegalStateException("Already initialized.");
	    }
	    
	    sInstance = new Application(context);
	    return sInstance;
	}
	

    /**
     * Retrieves the interval in seconds that servers are re-queried and refreshed.
     * 
     * @return int
     */
    public int getPullInterval() {
        return mPullIntervalSeconds;
    }

    /**
     * Sets the interval in seconds that servers are re-queried and refreshed.
     * 
     * @param int
     */
    public void setPullInterval(int pullIntervalSeconds) {
        mPullIntervalSeconds = pullIntervalSeconds;
    }	
	
	public User getSessionUser() {
	    if (mUser == null) {
	        mUser = new User();
	    }
	    
		return mUser;
	}

    /**
     * @return the contentResolver
     */
    public Context getContext() {
        return mContext;
    }
    
    /**
     * Registers an issue with the application that must be resolved.
     * Duplicates are ignored.
     * 
     * @param Issue issue
     */
    public void registerIssue(Issue issue) {
        if (!mIssueList.contains(issue)) {
            mIssueList.add(issue);
        }
    }
    
    /**
     * Removes an issue from the registered list.
     *
     * @param Issue issue
     */
    public void unregisterIssue(Issue issue) {
        mIssueList.remove(issue);
    }
    
    /**
     * Checks to see if any outstanding application-wide issues exist and starts the IssueActivity if any do.
     */
    public boolean hasIssues() {
        return ( !mIssueList.isEmpty() );
    }
    
    public List<Issue> getIssues() {
        return Collections.unmodifiableList(mIssueList);
    }
    
    public void requestFinish() {
        mIsFinishing = true;
    }
    
    public void markFinished() {
        mIsFinishing = false;
    }
    
    public boolean isFinishing() {
        return mIsFinishing;
    }
}
