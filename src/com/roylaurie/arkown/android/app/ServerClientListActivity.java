/**
 * 
 */
package com.roylaurie.arkown.android.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.ServerPuller;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.command.ClientCommand;
import com.roylaurie.arkown.android.app.R;
import com.roylaurie.arkown.server.Client;
import com.roylaurie.arkown.server.Server;
import com.roylaurie.arkown.server.Server.ServerMaps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * @author rlaurie
 *
 */
public class ServerClientListActivity extends ListActivity {
    private final int DIALOG_COMMAND_OPTIONS = 1;
    
    private Server mServer = null;
    private Client mSelectedClient;
    private ClientCommand mSelectedCommand;
    private Handler mHandler = new Handler();
    
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
        
        int serverId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
        ListView view = getListView();
        
        mServer = Application.getInstance().getSessionUser().getServer(serverId);

        registerForContextMenu(view);

        refreshView(false);
        mHandler.postDelayed(mRefreshRunnable, Application.getInstance().getPullInterval() * 1000);
        
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
        ArrayList<HashMap<String,String>> clientMapList = new ArrayList<HashMap<String,String>>();
        Client client = null;
        HashMap<String,String> clientMap = null;
  
        ServerPuller.pull(mServer, forcePull);
        
        for (Iterator<Client> it = mServer.getClients().iterator(); it.hasNext(); ) {
            client = (Client)it.next();
            clientMap = client.getHashMap();
            clientMapList.add(clientMap);
        }

        setListAdapter(new SimpleAdapter(
                this,
                clientMapList,
                R.layout.client_list_item,
                new String[] {"name", "score"},
                new int[] {
                        R.id.clientName,
                        R.id.playerScore
                }
        ));        
                
        return;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server_client_list_activity, menu);
        return true;        
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        SubMenu subMenu;
        boolean hasValidCommands = false;
        
        // add command categories
        int categoryIndex = -1;
        for (Category category : categories) {
            ++categoryIndex;
            
            // filter against category engine/product
            if (category.getEngine() != mServer.getEngine()
                    || ( category.hasProductFilter() && category.getProduct() != mServer.getProduct() )) {
                continue;
            }

            
            for (Command command : category.getCommands()) {
                if (command.getTarget() == Command.Target.CLIENT) {
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
                
                if (command.getTarget() != Command.Target.CLIENT) {
                    continue;
                }
                
                subMenu.add(categoryIndex, commandIndex, Menu.NONE, command.getName());
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        
        if (info != null) {
            mSelectedClient = mServer.getClients().get((int)info.id);
            return super.onContextItemSelected(item);
        }  
        
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        Command command = categories.get(item.getGroupId()).getCommands().get(item.getItemId());
        mSelectedCommand = new ClientCommand(command, mServer, mSelectedClient);

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
    
    public void sendCommand(String option) {
        String response;

        try {
            response = mServer.sendCommand(mSelectedCommand.getCommandString(option));
            if (response.length() > 0) {
                Toast.makeText(this, response, Toast.LENGTH_LONG).show();
            }
            ServerPuller.pull(mServer, true);
            refreshView(false);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }   
    }
    
    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog = null;
        AlertDialog.Builder builder = null;
        String[] items = null;

        switch (dialogId) {
        case DIALOG_COMMAND_OPTIONS:
            switch (mSelectedCommand.getOptionType()) {
            case MAP_LIST:
                items = ((ServerMaps)mServer).getAvailableMapTokens().toArray(new String[0]);
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
                        sendCommand(((ServerMaps)mServer).getAvailableMapTokens().get(which));
                        break;
                    case CUSTOM_LIST:
                        sendCommand(mSelectedCommand.getOptions().get(which));
                        break;
                    }

                    removeDialog(DIALOG_COMMAND_OPTIONS);
                    mSelectedCommand = null;
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mSelectedCommand = null;
                    ServerClientListActivity.this.removeDialog(DIALOG_COMMAND_OPTIONS);
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

