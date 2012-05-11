/**
 * 
 */
package com.roylaurie.arkown.android.app;

import java.util.ArrayList;
import java.util.Map;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.android.app.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

/**
 * @author rlaurie
 *
 */
public class CommandListActivity extends ExpandableListActivity implements OnChildClickListener {    
    public static final int DIALOG_REMOVE_CATEGORY = 1;
    public static final int DIALOG_REMOVE_COMMAND = 2;
    
    private ExpandableListAdapter mAdapter;
    private Category mSelectedCategory;
    private Command mSelectedCommand;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getExpandableListView());
        getExpandableListView().setOnChildClickListener(this);        
        
        refreshView();
    }
    
    public void refreshView() {
        // each primary index in groupData corresponds to the primary index in childData with the list of children 
        ArrayList<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        ArrayList<ArrayList<Map<String, String>>> childData = new ArrayList<ArrayList<Map<String, String>>>();
        ArrayList<Category> categoryList = Application.getInstance().getSessionUser().getCommandCategories();
        ArrayList<Map<String, String>> children = null;
        ArrayList<Command> commands = null;
        Category category = null;
        int selectedCategoryIndex = -1;

        for (int i = 0, n = categoryList.size(); i < n; ++i) {
            category = categoryList.get(i);
            children = new ArrayList<Map<String, String>>();
            commands = category.getCommands();
            
            for (int j = 0, jn = commands.size(); j < jn; ++j) {
                children.add(commands.get(j).getHashMap());
            }
            
            if (mSelectedCategory != null && category == mSelectedCategory) {
                selectedCategoryIndex = i;
            }
            
            groupData.add(category.getHashMap());            
            childData.add(children);
        }
        
        // Set up our adapter
        mAdapter = new SimpleExpandableListAdapter(
            this,
            groupData,
            android.R.layout.simple_expandable_list_item_2,
            new String[] { Category.MAP_NAME, Category.MAP_ENGINE_PRODUCT },
            new int[] { android.R.id.text1, android.R.id.text2 },
            childData,
            android.R.layout.simple_expandable_list_item_2,
            new String[] { Command.MAP_NAME, Command.MAP_TARGET },
            new int[] { android.R.id.text1, android.R.id.text2 }
        );
        
        setListAdapter(mAdapter);
        
        if (selectedCategoryIndex != -1) {        
            final int finalSelectedCategoryIndex = selectedCategoryIndex;
            
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getExpandableListView().expandGroup(finalSelectedCategoryIndex);
                }
            });
        }        
        
        mSelectedCategory = null;
        mSelectedCommand = null;
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        refreshView();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.command_list_activity_menu, menu);
        return true;        
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.add_command_category:
            Intent intent = new Intent(this, EditCommandCategoryActivity.class);
            intent.setData(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI);
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_COMMAND_CATEGORY);
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
   
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        
        switch (type) {
        case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
            mSelectedCategory = categories.get(group);
            menu.setHeaderTitle(mSelectedCategory.getName());
            inflater.inflate(R.menu.command_list_category_context_menu, menu);
            break;
            
        case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
            mSelectedCategory = categories.get(group);
            mSelectedCommand = mSelectedCategory.getCommands().get(child);
            menu.setHeaderTitle(mSelectedCommand.getName());
            inflater.inflate(R.menu.command_list_command_context_menu, menu);            
            break;            
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        Application application = Application.getInstance();
        long id;
        Intent intent;
        
        switch (item.getItemId()) {
        case R.id.edit_category:
            id = categories.get(group).getApplicationDatabaseId();
            intent = new Intent(this, EditCommandCategoryActivity.class).setData(
                Uri.withAppendedPath(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI, Long.toString(id))
            );
            
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_COMMAND_CATEGORY);
            return true;
            
        case R.id.remove_category:
            mSelectedCategory = categories.get(group);
            showDialog(DIALOG_REMOVE_CATEGORY);
            break;
            
        case R.id.add_command:
            mSelectedCategory = application.getSessionUser().getCommandCategories().get(group);
            id = mSelectedCategory.getApplicationDatabaseId();
            intent = new Intent(this, EditCommandActivity.class).setData(
                Uri.withAppendedPath(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI, Long.toString(id))
            );
            
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_COMMAND);
            return true;           
            
        case R.id.edit_command:
            mSelectedCategory = categories.get(group);
            mSelectedCommand = mSelectedCategory.getCommands().get(child);
            id = mSelectedCommand.getApplicationDatabaseId();
            intent = new Intent(this, EditCommandActivity.class).setData(
                Uri.withAppendedPath(ArkownContentProvider.COMMAND_CONTENT_URI, Long.toString(id))
            );
            
            startActivityForResult(intent, Application.REQUEST_CODE_EDIT_COMMAND);
            return true;  
            
        case R.id.remove_command:
            mSelectedCategory = categories.get(group);
            mSelectedCommand = mSelectedCategory.getCommands().get(child);
            showDialog(DIALOG_REMOVE_COMMAND);
            return true;   
        }
        
        return super.onContextItemSelected(item);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        User user = Application.getInstance().getSessionUser();
        ArrayList<Category> categories = null;
        ArrayList<Command> commands = null;
        int index = 0;
        
        switch (requestCode) {
        case Application.REQUEST_CODE_EDIT_COMMAND_CATEGORY:
            categories =  user.getCommandCategories();
            
            if (resultCode == RESULT_CANCELED && mSelectedCategory != null) {
                index = categories.indexOf(mSelectedCategory);
                mSelectedCategory = user.readCommandCategory(mSelectedCategory.getApplicationDatabaseId());
                categories.set(index, mSelectedCategory);
            }
            break;
            
        case Application.REQUEST_CODE_EDIT_COMMAND:
            if (resultCode == RESULT_CANCELED) {
                if (mSelectedCommand == null) { // remove unwritten object
                    commands = mSelectedCategory.getCommands();
                    
                    for (Command command : commands) {
                        if (command.getApplicationDatabaseId() < 1) {
                            commands.remove(command);
                            break;
                        }
                    }
                } else { // reload from db
                    commands = mSelectedCommand.getCategory().getCommands();
                    index = commands.indexOf(mSelectedCommand);
                    mSelectedCommand = user.readCommand(mSelectedCommand.getApplicationDatabaseId());
                    commands.set(index, mSelectedCommand);
                }
            }
            break;
        }
        
        refreshView();
    }       

    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);;
        
        switch (id) {
        case DIALOG_REMOVE_CATEGORY:     
            builder.setMessage("Delete category:\n" + mSelectedCategory.getName() + "?")
           .setCancelable(false)
           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   removeCategory();
                   mSelectedCategory = null;
               }
           })
           .setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                    mSelectedCategory = null;
                    dialog.cancel();
               }
           });
           break;
           
        case DIALOG_REMOVE_COMMAND:     
            builder.setMessage("Delete command:\n" + mSelectedCommand.getName() + "?")
           .setCancelable(false)
           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   removeCommand();
                   mSelectedCommand = null;
               }
           })
           .setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   mSelectedCommand = null;
                   dialog.cancel();
               }
           });
           break;
           
        default:
            throw new IndexOutOfBoundsException("Unknown dialog ID.");
        }
        
        dialog = builder.create();
        return dialog;
    }
    
    public void removeCategory() {
        Application.getInstance().getSessionUser().removeCommandCategory(mSelectedCategory);
        mSelectedCategory = null;
        refreshView();
    }    
 
    public void removeCommand() {
        Category category = mSelectedCommand.getCategory();
        category.removeCommand(mSelectedCommand);
        Application.getInstance().getSessionUser().saveCommandCategory(category);
        mSelectedCommand = null;
        refreshView();
    }
    
    public boolean onChildClick(ExpandableListView expandableListView, View view, int groupIndex, int childIndex,
            long id) {
        ArrayList<Category> categories = Application.getInstance().getSessionUser().getCommandCategories();
        mSelectedCategory = categories.get(groupIndex);
        mSelectedCommand = mSelectedCategory.getCommands().get(childIndex);
        
        long commandId = mSelectedCommand.getApplicationDatabaseId();
        Intent intent = new Intent(this, EditCommandActivity.class).setData(
            Uri.withAppendedPath(ArkownContentProvider.COMMAND_CONTENT_URI, Long.toString(commandId))
        );
        
        startActivityForResult(intent, Application.REQUEST_CODE_EDIT_COMMAND);
        return true;
    }
}