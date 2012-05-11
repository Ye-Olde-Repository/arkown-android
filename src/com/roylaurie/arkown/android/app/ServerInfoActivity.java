/**
 * 
 */
package com.roylaurie.arkown.android.app;

import java.util.ArrayList;
import java.util.Map;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.ServerPuller;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.android.Application.Issue;
import com.roylaurie.arkown.android.app.R;
import com.roylaurie.arkown.server.Server;
import com.roylaurie.arkown.server.json.ServerJson;
import com.roylaurie.modeljson.exception.JsonApiVersionException;
import com.roylaurie.modeljson.exception.JsonException;
import com.roylaurie.modeljson.exception.JsonNotFoundException;
import com.roylaurie.modeljson.exception.JsonPermissionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author rlaurie
 *
 */
public final class ServerInfoActivity extends Activity {
    private Server mServer = null;
    private Handler mHandler = new Handler();
    
    public static final int DIALOG_REMOVE_SERVER = 1;
    public static final int DIALOG_DELETE_SERVER = 2;
    
    private Runnable mRefreshRunnable = new Runnable() {
        public void run() {
            refreshView(false);
            mHandler.postDelayed(mRefreshRunnable, Application.getInstance().getPullInterval() * 1000);
        }
    };     
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_info_activity);
        Intent intent = getIntent();
        int serverId = Integer.parseInt(intent.getData().getPathSegments().get(1));
        
        mServer = Application.getInstance().getSessionUser().getServer(serverId);
        refreshView(false);
        mHandler.postDelayed(mRefreshRunnable, Application.getInstance().getPullInterval() * 1000);
        
        if (intent.getAction().equals(Intent.ACTION_DELETE)) {
            showDialog(DIALOG_REMOVE_SERVER);
        }
        
        return;
    }
    
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mRefreshRunnable); // stop pulling
    }
    
    public void onRestart() {
        super.onRestart();
        
        refreshView(false);
        mHandler.postDelayed(mRefreshRunnable, Application.getInstance().getPullInterval() * 1000);
    }    
    
    public void refreshView(boolean forcePull) {
        if (Application.getInstance().hasIssues()) {
            startActivity(new Intent(this, IssueActivity.class));
        }
        
        ServerPuller.pull(mServer, forcePull);
        
        Map<String,String> serverMap = mServer.getHashMap();
        
        ((TextView)findViewById(R.id.serverProduct)).setText(serverMap.get("product"));
        ((TextView)findViewById(R.id.serverName)).setText(serverMap.get("name"));
        ((TextView)findViewById(R.id.serverHost)).setText(serverMap.get("host"));
        ((TextView)findViewById(R.id.serverResponseTime)).setText(serverMap.get("responseTime"));
        ((TextView)findViewById(R.id.serverPlayerRatio)).setText(serverMap.get("clientRatio"));
        ((TextView)findViewById(R.id.serverMapToken)).setText(serverMap.get("mapToken"));
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server_info_activity, menu);
        return true;        
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.edit_server:
            Intent intent = new Intent(this, EditServerActivity.class);
            intent.setData(Uri.withAppendedPath(
                ArkownContentProvider.SERVER_CONTENT_URI, Long.toString(mServer.getApplicationDatabaseId())
            ));
            
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_SERVER);            
            return true;
            
        case R.id.remove_server:
                showDialog(DIALOG_REMOVE_SERVER);
                return true;
                
        case R.id.refresh:
            refreshView(true);
            return true;
            
        case R.id.help:
            startActivity(new Intent(Intent.ACTION_VIEW, Application.HELP_URI));
            return true;               
            
        case R.id.quit:
            Application.getInstance().requestFinish();
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        
        switch (id) {
        case DIALOG_REMOVE_SERVER:     
            dialog = createRemoveServerDialog();
            break;
            
        case DIALOG_DELETE_SERVER:
            dialog = createDeleteServerDialog();
            break;
        }
        
        return dialog;
    }
    
    private Dialog createRemoveServerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.alert_dialog_checkbox, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        ((TextView)view.findViewById(R.id.message)).setText(
            "Do you want to remove server:"
        );
        
        ((TextView)view.findViewById(R.id.name)).setText(
            mServer.getHost()
        );        
        
        ((TextView)view.findViewById(R.id.checkboxMessage)).setText(
            "Delete from API server?"
        );
        
        if (mServer.getDatabaseId() == 0 || !mServer.getCredentials().valid()) { // hide delete capabilities from non-admins
            view.findViewById(R.id.checkbox).setVisibility(View.GONE);
            view.findViewById(R.id.checkboxMessage).setVisibility(View.GONE);
        }
        
        builder.setView(view)
        .setTitle("Remove server?")
        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (((CheckBox)view.findViewById(R.id.checkbox)).isChecked()) {
                    ServerInfoActivity.this.showDialog(DIALOG_DELETE_SERVER);
                } else {
                    removeServer();
                }
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (getIntent().getAction().equals(Intent.ACTION_DELETE)) {
                    finish();
                } else {
                    removeDialog(DIALOG_REMOVE_SERVER);
                }           
            }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (getIntent().getAction().equals(Intent.ACTION_DELETE)) {
                    finish();
                } else {
                    removeDialog(DIALOG_REMOVE_SERVER);
                }
            }
        });

        return builder.create();
    }
    
    private Dialog createDeleteServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder.setTitle("Delete server?")
        .setMessage("Warning: All users and commands for this server will be permanently lost.")
        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    deleteServer();
                } catch (JsonPermissionException e) {
                    Toast.makeText(
                        ServerInfoActivity.this,
                        "You do not have permission to delete this server from API server.",
                        Toast.LENGTH_LONG
                    ).show();
                } catch (JsonException e) {
                    Toast.makeText(
                        ServerInfoActivity.this,
                        "Unable to delete server from API server.",
                        Toast.LENGTH_LONG
                    ).show();     
                }
                   
                refreshView(false); // if an issue arose from this request, refresh will handle it
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showDialog(DIALOG_REMOVE_SERVER);
            }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showDialog(DIALOG_REMOVE_SERVER);
            }
        });
        
        return builder.create();
    }
    
    private void removeServer() {
        Application.getInstance().getSessionUser().removeServer(mServer);
        finish();
    }   
    
    private void deleteServer() throws JsonException {
        ServerJson json = new ServerJson();
        try {
            json.delete(mServer);
        } catch (JsonNotFoundException e) {
            // do nothing
        } catch (JsonApiVersionException e) {
            Application.getInstance().registerIssue(Issue.UPGRADE);
            throw e;
        } 
        
        removeServer();
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        User user = Application.getInstance().getSessionUser();
        ArrayList<Server> servers = user.getServers();
        int index = 0;
       
        switch (requestCode) {
        case Application.REQUEST_CODE_EDIT_SERVER:
            if (resultCode == RESULT_CANCELED) {
                index = servers.indexOf(mServer);
                mServer = user.readServer(mServer.getApplicationDatabaseId());
                servers.set(index, mServer);
            }
            
            refreshView(true); // force just in case host/ip/port/gametype has changed
            return;
        }
        
        return;
    }        
}
