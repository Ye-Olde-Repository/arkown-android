package com.roylaurie.arkown.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.android.provider.CommandCategoryContentValueAdapter;
import com.roylaurie.arkown.android.provider.CommandContentValueAdapter;
import com.roylaurie.arkown.android.provider.ServerContentValueAdapter;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.server.Server;


public final class User {
	
    private ContentResolver mContentResolver = null;
	private ArrayList<Server> mServerList = null;
	private ArrayList<Category> mCommandCategoryList = null;
	
	public User() {
	    initializeServers();
	    initializeCommandCategories();
	}
	
	private ContentResolver getContentResolver() {
	    if (mContentResolver != null) {
	        return mContentResolver;
	    }
	    
	    mContentResolver = Application.getInstance().getContext().getContentResolver();
	    return mContentResolver;
	}
	
    private void initializeServers() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ArkownContentProvider.SERVER_CONTENT_URI, null, null, null, null);
        mServerList = ServerContentValueAdapter.fromCursor(cursor);
        cursor.close();
    }	
	
    private void initializeCommandCategories() {
        Category category = null;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = null;
        
        // fill category list
        cursor = contentResolver.query(
            ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI, null, null, null, null
        );
        
        mCommandCategoryList = CommandCategoryContentValueAdapter.fromCursor(cursor);
        cursor.close();
        
        // fill command list in each category
        for (int i = 0, n = mCommandCategoryList.size(); i < n; ++i) {
            category = mCommandCategoryList.get(i);
            cursor = contentResolver.query(
                ArkownContentProvider.COMMAND_CONTENT_URI,
                null,
                ArkownContentProvider.CommandColumns._CATEGORY_ID + " = ?",
                new String[] { Long.toString(category.getApplicationDatabaseId()) },
                null
            );
            
            category.setCommands(CommandContentValueAdapter.fromCursor(cursor));
            cursor.close();
        }
    }    
    
	public ArrayList<Server> getServers() {
		return mServerList;
	}
	
	public Server getServer(long id) {
	    for (int i = 0, n = mServerList.size(); i < n; ++i) {
	        if (mServerList.get(i).getApplicationDatabaseId() == id) {
	            return mServerList.get(i);
	        }
	    }
	    
	    throw new IndexOutOfBoundsException("Server ID `" + id +"` does not exist.");
	}
	

    
    public Server saveServer(Server server) {
        ContentValues values = ServerContentValueAdapter.toValues(server);
        int index = mServerList.indexOf(server);
        long id = -1;
        
        if (index == -1) {
            id = insertServer(values);
            server.setApplicationDatabaseId(id);
            mServerList.add(server); 
        } else {
            id = mServerList.get(index).getApplicationDatabaseId();
            server.setApplicationDatabaseId(id);
            values.put(ArkownContentProvider.ServerColumns._ID, id);
            updateServer(values);
        }

        Collections.sort((List<Server>)mServerList);
        
        return server;
    }
    
    public Server readServer(long id) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
            Uri.withAppendedPath(ArkownContentProvider.SERVER_CONTENT_URI,  Long.toString(id)),
            null,
            null,
            null,
            null
        );
        
        Server server = ServerContentValueAdapter.fromCursor(cursor).get(0);
        cursor.close();
      
        return server;
    }
    
    public void removeServer(Server server) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = null;
        int index = mServerList.indexOf(server);

        if (index == -1) {
            throw new IllegalArgumentException("Server does not exist.");
        }
        
        server = mServerList.get(index);
        uri = Uri.parse(ArkownContentProvider.SERVER_CONTENT_URI.toString() + "/" + server.getApplicationDatabaseId());
        
        contentResolver.delete(uri, null, null);
        mServerList.remove(index);
        Collections.sort((List<Server>)mServerList);
    }
    
    private long insertServer(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        values.remove(ArkownContentProvider.ServerColumns._ID);
        Uri uri = contentResolver.insert(ArkownContentProvider.SERVER_CONTENT_URI, values);
        long id = Long.parseLong(uri.getPathSegments().get(1)); // .../server/#
        
        return id;
    }

    private void updateServer(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse(
                ArkownContentProvider.SERVER_CONTENT_URI.toString()
                + "/" + values.getAsString(ArkownContentProvider.ServerColumns._ID)
        );
        
        contentResolver.update(
                uri,
                values,
                null,
                null
        );
    }

    public ArrayList<Category> getCommandCategories() {
        if (mCommandCategoryList == null) {
            initializeCommandCategories(); 
        }
        
        return mCommandCategoryList;
    }
    
    public void removeCommandCategory(Category category) {
        ContentResolver contentResolver = getContentResolver();
        int index = mCommandCategoryList.indexOf(category);
        Uri uri = null;
        
        if (index == -1) {
            throw new IllegalArgumentException("Command Category does not exist.");
        }
        
        category = mCommandCategoryList.get(index);
        uri = Uri.withAppendedPath(
            ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI,
            Long.toString(category.getApplicationDatabaseId())
        );

        contentResolver.delete(uri, null, null);
        mCommandCategoryList.remove(index);
        Collections.sort((List<Category>)mCommandCategoryList);
    }

    public Category saveCommandCategory(Category commandCategory) {
        ContentValues values = CommandCategoryContentValueAdapter.toValues(commandCategory);
        int index = mCommandCategoryList.indexOf(commandCategory);
        long commandId = 0;
        long categoryId = 0;
        
        if (index == -1) {
            categoryId = insertCommandCategory(values);
            commandCategory.setApplicationDatabaseId(categoryId);
            mCommandCategoryList.add(commandCategory); 
        } else {
            categoryId = mCommandCategoryList.get(index).getApplicationDatabaseId();
            commandCategory.setApplicationDatabaseId(categoryId);
            values.put(ArkownContentProvider.CommandCategoryColumns._ID, categoryId);
            updateCommandCategory(values);
        }
        
        Collections.sort((List<Category>)mCommandCategoryList);        
        
        // save commands
        for (Command command : commandCategory.getCommands()) {    
            command.setCategory(commandCategory);
            commandId = command.getApplicationDatabaseId();
            try {
                if (commandId < 1) { // insert
                    commandId = insertCommand(CommandContentValueAdapter.toValues(command));
                    command.setApplicationDatabaseId(commandId);
                } else {
                    updateCommand(CommandContentValueAdapter.toValues(command));
                }
            } catch (RuntimeException e) {
                throw e;
            }
        }
        
        Collections.sort((List<Command>)commandCategory.getCommands());        
        pruneCommands(commandCategory);
        
        return commandCategory;
    }

    public Category getCommandCategory(long id) {
        for (int i = 0, n = mCommandCategoryList.size(); i < n; ++i) {
            if (mCommandCategoryList.get(i).getApplicationDatabaseId() == id) {
                return mCommandCategoryList.get(i);
            }
        }
        
        throw new IndexOutOfBoundsException("Command Category ID `" + id +"` does not exist.");
    }
    
    public Category readCommandCategory(long id) {
        ContentResolver contentResolver = getContentResolver();
        Category category = null;
        Cursor cursor = null;
        
        // fill category list
        cursor = contentResolver.query(
            Uri.withAppendedPath(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI, Long.toString(id)),
            null,
            null,
            null,
            null
        );
        
        try {
            category = CommandCategoryContentValueAdapter.fromCursor(cursor).get(0);
        } catch(IndexOutOfBoundsException e) {
            cursor.close();
            throw e;
        }
        
        cursor.close();
        
        cursor = contentResolver.query(
            ArkownContentProvider.COMMAND_CONTENT_URI,
            null,
            ArkownContentProvider.CommandColumns.CATEGORY_ID + " = ?",
            new String[] { Long.toString(category.getApplicationDatabaseId()) },
            null
        );
        
        category.setCommands(CommandContentValueAdapter.fromCursor(cursor));
        cursor.close();
        
        return category;
    }
    
    public Command readCommand(long id) {
        Command command = null;
        long categoryId = 0;
        ArrayList<Command> commands = null;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
            Uri.withAppendedPath(ArkownContentProvider.COMMAND_CONTENT_URI, Long.toString(id)),
            null,
            null,
            null,
            null
        ); 

        commands = CommandContentValueAdapter.fromCursor(cursor);
        cursor.close();
        command = commands.get(0);
        categoryId = command.getCategoryApplicationDatabaseId();
        
        for (Category category : mCommandCategoryList) {
            if (category.getApplicationDatabaseId() == categoryId) {
                command.setCategory(category);
                break;
            }
        }
        
        return command;
    }
    
    private long insertCommandCategory(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        values.remove(ArkownContentProvider.CommandCategoryColumns._ID);
        Uri uri = contentResolver.insert(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI, values);
        long id = Long.parseLong(uri.getPathSegments().get(1));
        
        return id;
    }    
    
    private void updateCommandCategory(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse(
                ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI.toString()
                + "/" + values.getAsString(ArkownContentProvider.CommandCategoryColumns._ID)
        );
        
        contentResolver.update(uri, values, null, null);
    }    
    
    public Command getCommand(long id) {
        for (Category category : mCommandCategoryList) {
            for (Command command : category.getCommands()) {
                if (command.getApplicationDatabaseId() == id) {
                    return command;
                }
            }
        }
        
        throw new IndexOutOfBoundsException("Command ID `" + id +"` does not exist.");
    }    
    
    private long insertCommand(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        values.remove(ArkownContentProvider.CommandColumns._ID);
        Uri uri = contentResolver.insert(ArkownContentProvider.COMMAND_CONTENT_URI, values);
        long id = Integer.parseInt(uri.getPathSegments().get(1));
        
        return id;
    }    
    
    private void updateCommand(ContentValues values) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse(
                ArkownContentProvider.COMMAND_CONTENT_URI.toString()
                + "/" + values.getAsString(ArkownContentProvider.CommandColumns._ID)
        );
        
        contentResolver.update(uri, values, null, null);
    }
    
    private void pruneCommands(Category category) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri =  ArkownContentProvider.COMMAND_CONTENT_URI;
        String selectionArgs[] = new String[category.getCommands().size() + 1]; // + category id
        String selection = ArkownContentProvider.CommandColumns._CATEGORY_ID + " = ? AND "
        + ArkownContentProvider.CommandColumns._ID + " NOT IN (";

        selectionArgs[0] = Long.toString(category.getApplicationDatabaseId());
        
        int i = 1;
        for (Command command : category.getCommands()) {
            selectionArgs[i] = Long.toString(command.getApplicationDatabaseId());
            selection += ( ++i < selectionArgs.length ? "?," : "?" );
        }
        
        selection += ")";
        contentResolver.delete(uri, selection, selectionArgs);       
    }
}
