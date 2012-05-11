/**
 * Copyright (C) 2011 Roy Laurie Software <http://www.roylaurie.com>
 */
package com.roylaurie.arkown.android.app;

import java.util.List;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.Application.Issue;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 *
 */
public final class IssueActivity extends Activity {
    private static final int DIALOG_UPGRADE = 1;
    private static final int DIALOG_AUTOMATIC_UPGRADE = 2;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Application application = Application.getInstance();
        List<Issue> issueList = application.getIssues();
        
        for (Issue issue : issueList) {
            switch (issue) {
            case UPGRADE:
                showDialog(DIALOG_UPGRADE);
                break;
            }
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_UPGRADE:     
            return createUpgradeDialog();
        case DIALOG_AUTOMATIC_UPGRADE:
            return createAutomaticUpgradeDialog();
        }
        
        throw new IllegalArgumentException("Invalid dialog id `" + id + "`.");
    }
    
    private Dialog createUpgradeDialog() {        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Application.getInstance().unregisterIssue(Issue.UPGRADE);
        
        builder.setMessage(
            "ArkOwn needs to be updated."
        )
        .setCancelable(false)
        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showDialog(DIALOG_AUTOMATIC_UPGRADE);
            }
        })
        .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                 dialog.cancel();
                 finish();
            }
        });
                
        return builder.create();        
    }
    
    private Dialog createAutomaticUpgradeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
            "Please configure ArkOwn to automatically update."
        )
        .setCancelable(false)
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(Intent.ACTION_VIEW, Application.MARKET_DETAILS_URI));
                finish();
            }
        });   
        
        return builder.create();
    }
}
