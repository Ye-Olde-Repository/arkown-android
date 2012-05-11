package com.roylaurie.arkown.android.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.ServerPuller;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.command.ServerCommand;
import com.roylaurie.arkown.android.app.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.roylaurie.arkown.server.*;
import com.roylaurie.arkown.server.Server.ServerMaps;

public final class ServerListActivity extends ListActivity implements OnItemClickListener {     
    //private final int DIALOG_PROGRESS = 2;
    private final int DIALOG_COMMAND_OPTIONS = 3;
    
    private Handler mHandler = new Handler();
    private Server mSelectedServer;
    private ServerCommand mSelectedCommand;
    
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
        ListView view = getListView();

        view.setTextFilterEnabled(true);
        view.setOnItemClickListener(this);
        registerForContextMenu(view);

        refreshView(true);
        mHandler.postDelayed(mRefreshRunnable, Application.getInstance().getPullInterval() * 1000);        
        
        return;
    }
    
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mRefreshRunnable); // stop pulling
    }
    
    public void onRestart() {
        super.onRestart();
        
        Application application = Application.getInstance();
        
        if (application.isFinishing()) {
            application.markFinished();
            finish();
            return;
        }
        
        refreshView(false);
        mHandler.postDelayed(mRefreshRunnable, application.getPullInterval() * 1000);
    }
    
    
    public void refreshView(boolean forcePull) {
        Application application = Application.getInstance();
        User user = application.getSessionUser();
        Collection<Server> servers = user.getServers();
        Server server = null;
        HashMap<String, String> serverMap = null;
        ArrayList<Map<String, String>> serverMapList = new ArrayList<Map<String, String>>();
        
        for (Iterator<Server> it = servers.iterator(); it.hasNext(); ) {
            server = (Server)it.next();
            ServerPuller.pull(server, forcePull);
            serverMap = server.getHashMap();
            
            if (server.getMaxClients() < 1) { // the first pull failed
                serverMap.put("name", server.getHostname() + ":" + server.getPort());
                serverMap.put("mapToken", "unknown");                   
            }
            
            serverMapList.add(serverMap);
        }

        setListAdapter(new SimpleAdapter(
            this,
            serverMapList,
            R.layout.server_list_item,
            new String[] {"name", "mapToken", "responseTime", "clientRatio"},
            new int[] {
                R.id.serverName,
                R.id.serverMapToken,
                R.id.serverResponseTime,
                R.id.serverPlayerRatio,
            }
        ));        
        
        if (Application.getInstance().hasIssues()) {
            startActivity(new Intent(this, IssueActivity.class));
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server_list_activity_menu, menu);
        return true;        
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_server:
                Intent intent = new Intent(this, EditServerActivity.class);
                intent.setData(ArkownContentProvider.SERVER_CONTENT_URI);
                startActivityForResult(intent, Application.REQUEST_CODE_EDIT_SERVER);
                return true;
                
            case R.id.refresh:
                refreshView(true);
                return true;
                
            case R.id.help:
                startActivity(new Intent(Intent.ACTION_VIEW, Application.HELP_URI));
                return true;                
                
            case R.id.quit:
                finish();
                return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        User user = Application.getInstance().getSessionUser();
        ArrayList<Server> servers = user.getServers();
        int index = 0;
        
        switch (requestCode) {
        case Application.REQUEST_CODE_EDIT_SERVER:
            if (resultCode == RESULT_CANCELED) {
                if (mSelectedServer != null) { // server insert
                    index = servers.indexOf(mSelectedServer);
                    mSelectedServer = user.readServer(mSelectedServer.getApplicationDatabaseId());
                    servers.set(index, mSelectedServer);
                }
            }
            
            if (mSelectedServer != null) { // server edit, force pull
                ServerPuller.pull(mSelectedServer, true);
            }
            
            mSelectedServer = null;
            break;
        }      
       
        refreshView(false);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        long serverId = Application.getInstance().getSessionUser().getServers().get(position).getApplicationDatabaseId();
        Intent intent = new Intent(
            Intent.ACTION_VIEW,
            Uri.withAppendedPath(ArkownContentProvider.SERVER_CONTENT_URI, Long.toString(serverId)),
            this,
            ServerTabActivity.class
        );
        startActivity(intent);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        int serverIndex = (int)((AdapterContextMenuInfo)menuInfo).id;
        ArrayList<Server> servers = Application.getInstance().getSessionUser().getServers();
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        MenuInflater inflater = getMenuInflater();
        SubMenu subMenu = null;
        boolean hasValidCommands = false;

        mSelectedServer = servers.get(serverIndex);
        
        inflater.inflate(R.menu.server_list_activity_context_menu, menu);
        
        // add command categories
        int categoryIndex = -1;
        for (Category category : categories) {
            ++categoryIndex;

            // filter against category engine/product
            if (category.getEngine() != mSelectedServer.getEngine()
                    || ( category.hasProductFilter() && category.getProduct() != mSelectedServer.getProduct() )) {
                continue;
            }            
            
            for (Command command : category.getCommands()) {
                if (command.getTarget() == Command.Target.SERVER) {
                    hasValidCommands = true;
                    break;
                }
            }
            if (!hasValidCommands) {
                continue;
            }            
            
            subMenu = menu.addSubMenu(category.getName() + "  >");
            
            // add commands for category
            int commandIndex = -1;
            for (Command command : category.getCommands()) {
                ++commandIndex;
                
                if (command.getTarget() != Command.Target.SERVER) {
                    continue;
                }
                
                subMenu.add(categoryIndex, commandIndex, Menu.NONE, command.getName());
            }
        }
    }
    
    public void sendCommand(String option) {
        String response;
        
        try {
            response = mSelectedServer.sendCommand(mSelectedCommand.getCommandString(option));
            if (response.length() > 0) {
                Toast.makeText(this, response, Toast.LENGTH_LONG).show();
            }
            ServerPuller.pull(mSelectedServer, true);
            refreshView(false);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } 
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        
        if (info == null) { // handle rcon commands and return
            ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
            Command command = categories.get(item.getGroupId()).getCommands().get(item.getItemId());
            mSelectedCommand = new ServerCommand(command, mSelectedServer);
            
            switch (mSelectedCommand.getOptionType()) {
            case MAP_LIST:
            case CUSTOM_LIST:
                showDialog(DIALOG_COMMAND_OPTIONS);
                break;
                
            default:
                sendCommand(null);
            }
            
            return true;
        }
        
        Intent intent = null;
        
        switch (item.getItemId()) {
        case R.id.edit_server:
            intent = new Intent(this, EditServerActivity.class).setData(
                Uri.withAppendedPath(
                    ArkownContentProvider.SERVER_CONTENT_URI,
                    Long.toString(mSelectedServer.getApplicationDatabaseId())
                )
            );
            
            mSelectedServer = null;
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_SERVER);
            return true;
            
        case R.id.remove_server:
            intent = new Intent(
                Intent.ACTION_DELETE,
                Uri.withAppendedPath(
                    ArkownContentProvider.SERVER_CONTENT_URI,
                    Long.toString(mSelectedServer.getApplicationDatabaseId())
                ),
                this,
                ServerTabActivity.class
            );
            
            mSelectedServer = null;
            startActivity(intent);
            return true;
        }
        
        return super.onContextItemSelected(item);
    }    
    
    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog = null;
        AlertDialog.Builder builder = null;
        
        
        switch (dialogId) {
        case DIALOG_COMMAND_OPTIONS: 
            String[] items = null;
            switch (mSelectedCommand.getOptionType()) {
            case MAP_LIST:
                items = ((ServerMaps)mSelectedServer).getAvailableMapTokens().toArray(new String[0]);
                break;
            case CUSTOM_LIST:
                items = mSelectedCommand.getOptions().toArray(new String[0]);
                break;
            }
           
            builder = new AlertDialog.Builder(this);
            builder.setTitle("Select an option");
            builder.setCancelable(true);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (mSelectedCommand.getOptionType()) {
                    case MAP_LIST:
                        sendCommand(((ServerMaps)mSelectedServer).getAvailableMapTokens().get(which));
                        break;
                    case CUSTOM_LIST:
                        sendCommand(mSelectedCommand.getOptions().get(which));
                        break;
                    }

                    removeDialog(DIALOG_COMMAND_OPTIONS);
                    mSelectedServer = null;
                    mSelectedCommand = null;
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mSelectedServer = null;
                    mSelectedCommand = null;
                    removeDialog(DIALOG_COMMAND_OPTIONS);
                }
            });
            
           dialog = builder.create();
           break;           

        default:
            throw new IndexOutOfBoundsException("Unknown dialog ID.");
        }
        
        
        return dialog;
    }
}