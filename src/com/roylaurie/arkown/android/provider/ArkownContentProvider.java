/**
 * 
 */
package com.roylaurie.arkown.android.provider;

import java.util.HashMap;

import com.roylaurie.arkown.android.Application;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 *
 */
public final class ArkownContentProvider extends ContentProvider {  
    public static final String AUTHORITY = "com.roylaurie.arkown.android.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://com.roylaurie.arkown.android.provider");
    public static final String TAG = "ArkownContentProvider";
    
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    public static final int SERVERS = 1;
    public static final int SERVER_ID = 2;
    public static final int COMMAND_CATEGORIES = 3;
    public static final int COMMAND_CATEGORY_ID = 4;    
    public static final int COMMANDS = 5;
    public static final int COMMAND_ID = 6;    
    
    public static final String DATABASE_NAME = "arkown.db";
    public static final int DATABASE_VERSION = 2;
    
    // server constants
    public static final String SERVER_TABLE_NAME = "server";
    public static final Uri SERVER_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "server");
    public static final String SERVER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.roylaurie.arkown.server";
    public static final String SERVER_ITEM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.roylaurie.arkown.server";  
    public static final String SERVER_DEFAULT_ORDER = "local_id DESC";
        
    // command category constants
    public static final String COMMAND_CATEGORY_TABLE_NAME = "command_category";
    public static final Uri COMMAND_CATEGORY_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "command_category");
    public static final String COMMAND_CATEGORY_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.roylaurie.arkown.command_category";
    public static final String COMMAND_CATEGORY_ITEM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.roylaurie.arkown.command_category";  
    public static final String COMMAND_CATEGORY_DEFAULT_ORDER = "name ASC";
    
    // command constants
    public static final String COMMAND_TABLE_NAME = "command";
    public static final Uri COMMAND_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "command");
    public static final String COMMAND_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.roylaurie.arkown.command";
    public static final String COMMAND_ITEM_CONTENT_TYPE = "vnd.android.cursor.item/vnd.roylaurie.arkown.command";  
    public static final String COMMAND_DEFAULT_ORDER = "name ASC";    
    
    // projection maps
    private static HashMap<String, String> sServerProjectionMap = new HashMap<String, String>();
    private static HashMap<String, String> sCommandCategoryProjectionMap = new HashMap<String, String>();
    private static HashMap<String, String> sCommandProjectionMap = new HashMap<String, String>();
    
    public class ColumnException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        private String mColumn = null;
        
        public ColumnException(String column, String message) {
            super(message);
            mColumn = column;
        }
        
        public String getColumn() {
            return mColumn;
        }
    }
    
    public static final class ServerColumns implements BaseColumns {
        private ServerColumns() {}
        public static final String _ID = "local_id";
        public static final String ID = "id";
        public static final String HOSTNAME = "hostname";
        public static final String PORT = "port";
        public static final String ENGINE_TYPE = "engine_type";
        public static final String CREDENTIAL_USERNAME = "credential_username";
        public static final String CREDENTIAL_PASSWORD = "credential_password";
        public static final String IS_QUERY_PROXY_ALLOWED = "is_query_proxy_allowed";
    }
    
    public static final class CommandCategoryColumns implements BaseColumns {
        private CommandCategoryColumns() {}
        public static final String _ID = "local_id";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String ENGINE_TYPE = "engine_type";
        public static final String PRODUCT = "product";
    }
    
    public static final class CommandColumns implements BaseColumns {
        private CommandColumns() {}
        public static final String _ID = "local_id";
        public static final String ID = "id";
        public static final String _CATEGORY_ID = "local_category_id";
        public static final String CATEGORY_ID = "category_id";
        public static final String NAME = "name";
        public static final String RAW_COMMAND = "raw_command";
        public static final String TARGET = "target";
        public static final String OPTION_TYPE="option_type";
        public static final String OPTION_CSV = "option_csv";
    }    
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            
            if (!db.isReadOnly()) {
                db.execSQL("PRAGMA foreign_keys=ON;");
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            
            // create server table
            db.execSQL("CREATE TABLE " + SERVER_TABLE_NAME + " ("
                + ServerColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ServerColumns.ID + " INTEGER,"
                + ServerColumns.HOSTNAME + " TEXT,"
                + ServerColumns.PORT + " INTEGER,"
                + ServerColumns.ENGINE_TYPE + " TEXT,"
                + ServerColumns.CREDENTIAL_USERNAME + " TEXT,"
                + ServerColumns.CREDENTIAL_PASSWORD + " TEXT,"
                + ServerColumns.IS_QUERY_PROXY_ALLOWED + " INTEGER"
                + ");"
            );
            
            // create command category table
            db.execSQL("CREATE TABLE " + COMMAND_CATEGORY_TABLE_NAME + " ("
                + CommandCategoryColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CommandCategoryColumns.ID + " INTEGER,"
                + CommandCategoryColumns.NAME + " TEXT,"
                + CommandCategoryColumns.ENGINE_TYPE + " TEXT,"
                + CommandCategoryColumns.PRODUCT + " TEXT"
                + ");"
            );
            
            // create command table
            db.execSQL("CREATE TABLE " + COMMAND_TABLE_NAME + " ("
                + CommandColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CommandColumns.ID + " INTEGER,"
                + CommandColumns._CATEGORY_ID + " INTEGER,"
                + CommandColumns.CATEGORY_ID + " INTEGER,"
                + CommandColumns.NAME + " TEXT,"
                + CommandColumns.RAW_COMMAND + " TEXT,"
                + CommandColumns.TARGET + " TEXT,"
                + CommandColumns.OPTION_TYPE + " TEXT,"
                + CommandColumns.OPTION_CSV + " TEXT,"
                + "FOREIGN KEY (" + CommandColumns._CATEGORY_ID + ")"
                + " REFERENCES " + COMMAND_CATEGORY_TABLE_NAME + " (" + CommandCategoryColumns._ID + ")"
                + " ON DELETE CASCADE"
                + ");"
            );        
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            
            db.execSQL("DROP TABLE IF EXISTS " + SERVER_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + COMMAND_CATEGORY_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + COMMAND_TABLE_NAME);
            
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;
    
    /*
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        Application.initialize(getContext());
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }    
    
    /*
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case SERVERS:
            return SERVER_CONTENT_TYPE;
        case SERVER_ID:
            return SERVER_ITEM_CONTENT_TYPE;
        case COMMAND_CATEGORIES:
            return COMMAND_CATEGORY_CONTENT_TYPE;
        case COMMAND_CATEGORY_ID:
            return COMMAND_CATEGORY_ITEM_CONTENT_TYPE;
        case COMMANDS:
            return COMMAND_CONTENT_TYPE;
        case COMMAND_ID:
            return COMMAND_ITEM_CONTENT_TYPE;            
        }
        
        throw new IllegalArgumentException("Unknown URI `" + uri + "`.");
    }    

    private void validateString(ContentValues values, String columnKey, String name) {
        if (!values.containsKey(columnKey) || values.getAsString(columnKey).length() <= 0) {
            throw new ColumnException(columnKey, name + " is required.");
        }
    }
    
    private void validateInteger(ContentValues values, String columnKey, String name) {
        if (!values.containsKey(columnKey) || values.getAsInteger(columnKey) <= 0) {
            throw new ColumnException(columnKey, name + " is required.");
        }
    }
    
    private void validateBoolean(ContentValues values, String columnKey, String name) {
        if (!values.containsKey(columnKey) ||
                ( values.getAsInteger(columnKey) != 0 && values.getAsInteger(columnKey) != 1)
        ){
            throw new ColumnException(columnKey, name + " is required.");
        }
    }        
    
    private void validateValues(int uriType, ContentValues values) {
        switch (uriType) {
        case SERVERS:
        case SERVER_ID:
            validateString(values, ServerColumns.HOSTNAME, "Hostname");
            validateInteger(values, ServerColumns.PORT, "Port");
            validateString(values, ServerColumns.ENGINE_TYPE, "Engine");

            if (!values.containsKey(ServerColumns.CREDENTIAL_USERNAME)) {
                values.put(ServerColumns.CREDENTIAL_USERNAME, "");
            }            
            if (!values.containsKey(ServerColumns.CREDENTIAL_PASSWORD)) {
                values.put(ServerColumns.CREDENTIAL_PASSWORD, "");
            }
            validateBoolean(values, ServerColumns.IS_QUERY_PROXY_ALLOWED, "Public");
            break;
            
        case COMMANDS:
        case COMMAND_ID:
            validateString(values, CommandColumns.NAME, "Name");
            validateInteger(values, CommandColumns._CATEGORY_ID, "Category");
            validateString(values, CommandColumns.RAW_COMMAND, "Command");
            validateString(values, CommandColumns.TARGET, "Target");
            validateString(values, CommandColumns.OPTION_TYPE, "Option Type");
            break;
            
        case COMMAND_CATEGORIES:
        case COMMAND_CATEGORY_ID:
            validateString(values, CommandCategoryColumns.NAME, "Name");
            validateString(values, CommandCategoryColumns.ENGINE_TYPE, "Engine");
            
            if (!values.containsKey(CommandCategoryColumns.PRODUCT)) {
                values.put(CommandCategoryColumns.PRODUCT, "");
            } 
            break;
        }
        
        return;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        String tableName, hackColumn;
        Uri contentUri = null;
        int uriType = sUriMatcher.match(uri); // valdiates uri as well

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        validateValues(uriType, values);
       
        switch (uriType) {
        case SERVERS:
            tableName = SERVER_TABLE_NAME;
            hackColumn = ServerColumns.HOSTNAME;
            contentUri = SERVER_CONTENT_URI;
            break;
            
        case COMMAND_CATEGORIES:
            tableName = COMMAND_CATEGORY_TABLE_NAME;
            hackColumn = CommandCategoryColumns.NAME;
            contentUri = COMMAND_CATEGORY_CONTENT_URI;            
            break;
            
        case COMMANDS:
            tableName = COMMAND_TABLE_NAME;
            hackColumn = CommandColumns.NAME;
            contentUri = COMMAND_CONTENT_URI;            
            break;  
            
        default:
            throw new IllegalArgumentException("Unknown URI `" + uri +"`.");
                
        }        

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(tableName, hackColumn, values);
        if (rowId <= 0) {
            throw new SQLException("Failed to insert row into `" + uri + "`.");
        }
        
        Uri recordUri  = ContentUris.withAppendedId(contentUri, rowId);
        getContext().getContentResolver().notifyChange(recordUri, null);
        
        return recordUri;
    }

    /*
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int uriType = sUriMatcher.match(uri); // validates uri as well
        int count = -1; // use -1 to determine switch matching
        String id = null;
        String tableName = null;
        String idColumn = null;
        
        validateValues(uriType, values);
        
        switch (uriType) {
        case SERVERS:
            count = db.update(SERVER_TABLE_NAME, values, selection, selectionArgs);
            break;

        case SERVER_ID:
            tableName = SERVER_TABLE_NAME;
            idColumn = ServerColumns._ID;
            break;          
            
        case COMMAND_CATEGORIES:
            count = db.update(COMMAND_CATEGORY_TABLE_NAME, values, selection, selectionArgs);
            break;
           
        case COMMAND_CATEGORY_ID:
            tableName = COMMAND_CATEGORY_TABLE_NAME;
            idColumn = CommandCategoryColumns._ID;
            break;                 
            
        case COMMANDS:
            count = db.update(COMMAND_TABLE_NAME, values, selection, selectionArgs);
            break;
            
        case COMMAND_ID:
            tableName = COMMAND_TABLE_NAME;
            idColumn = CommandColumns._ID;
            break;     

        default:
            throw new IllegalArgumentException("Unknown URI `" + uri + "`.");
        }
        
        // handle update of single records from switch
        if (count == -1) {
            id = uri.getPathSegments().get(1);
            count = db.update(
                    tableName,
                    values,
                    idColumn + "=" + id
                    + ( !TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "" ),
                    selectionArgs
            );
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }    

    /* 
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String id;
        
        switch (sUriMatcher.match(uri)) {
        case SERVERS:
            count = db.delete(SERVER_TABLE_NAME, selection, selectionArgs);
            break;

        case SERVER_ID:
            id = uri.getPathSegments().get(1);
            count = db.delete(
                    SERVER_TABLE_NAME,
                    ServerColumns._ID + "=" + id
                    + ( !TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "" ),
                    selectionArgs
            );
            break;
            
        case COMMAND_CATEGORIES:
            count = db.delete(COMMAND_CATEGORY_TABLE_NAME, selection, selectionArgs);
            break;

        case COMMAND_CATEGORY_ID:
            id = uri.getPathSegments().get(1);
            count = db.delete(
                    COMMAND_CATEGORY_TABLE_NAME,
                    CommandCategoryColumns._ID + "=" + id
                    + ( !TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "" ),
                    selectionArgs
            );
            break;            

        case COMMANDS:
            count = db.delete(COMMAND_TABLE_NAME, selection, selectionArgs);
            break;

        case COMMAND_ID:
            id = uri.getPathSegments().get(1);
            count = db.delete(
                    COMMAND_TABLE_NAME,
                    CommandColumns._ID + "=" + id
                    + ( !TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "" ),
                    selectionArgs
            );
            break;            
            
        default:
            throw new IllegalArgumentException("Unknown URI `" + uri + "`.");
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }    
    
    /* 
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = sortOrder;
        
        switch (sUriMatcher.match(uri)) {
        case SERVER_ID:
            qb.appendWhere(ServerColumns._ID + "=" + uri.getPathSegments().get(1));
            // pass thru ...
        case SERVERS:
            qb.setTables(SERVER_TABLE_NAME);
            qb.setProjectionMap(sServerProjectionMap);
            
            if (TextUtils.isEmpty(orderBy)) {
                orderBy = SERVER_DEFAULT_ORDER;
            }
            break;
            
        case COMMAND_CATEGORY_ID:
            qb.appendWhere(CommandCategoryColumns._ID + "=" + uri.getPathSegments().get(1));
            // pass thru ...
        case COMMAND_CATEGORIES:
            qb.setTables(COMMAND_CATEGORY_TABLE_NAME);
            qb.setProjectionMap(sCommandCategoryProjectionMap);
            
            if (TextUtils.isEmpty(orderBy)) {
                orderBy = COMMAND_CATEGORY_DEFAULT_ORDER;
            }
            break;
            
        case COMMAND_ID:
            qb.appendWhere(CommandColumns._ID + "=" + uri.getPathSegments().get(1));
            // pass thru ...
        case COMMANDS:
            qb.setTables(COMMAND_TABLE_NAME);
            qb.setProjectionMap(sCommandProjectionMap);
            
            if (TextUtils.isEmpty(orderBy)) {
                orderBy = COMMAND_DEFAULT_ORDER;
            }
            break;            
            
        default:
            throw new IllegalArgumentException("Unknown URI `" + uri + "`.");
        }
        
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }    
    
    static {
        // uri matcher
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "server", SERVERS);
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "server/#", SERVER_ID);
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "command_category", COMMAND_CATEGORIES);
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "command_category/#", COMMAND_CATEGORY_ID);
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "command", COMMANDS);
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "command/#", COMMAND_ID);
        
        // server projection map
        sServerProjectionMap.put(ServerColumns._ID, ServerColumns._ID);
        sServerProjectionMap.put(ServerColumns.ID, ServerColumns.ID);
        sServerProjectionMap.put(ServerColumns.HOSTNAME, ServerColumns.HOSTNAME);
        sServerProjectionMap.put(ServerColumns.PORT, ServerColumns.PORT);
        sServerProjectionMap.put(ServerColumns.ENGINE_TYPE, ServerColumns.ENGINE_TYPE);
        sServerProjectionMap.put(ServerColumns.CREDENTIAL_USERNAME, ServerColumns.CREDENTIAL_USERNAME);
        sServerProjectionMap.put(ServerColumns.CREDENTIAL_PASSWORD, ServerColumns.CREDENTIAL_PASSWORD);
        sServerProjectionMap.put(ServerColumns.IS_QUERY_PROXY_ALLOWED, ServerColumns.IS_QUERY_PROXY_ALLOWED);
        
        // command category projection map
        sCommandCategoryProjectionMap.put(CommandCategoryColumns._ID, CommandCategoryColumns._ID);
        sCommandCategoryProjectionMap.put(CommandCategoryColumns.ID, CommandCategoryColumns.ID);
        sCommandCategoryProjectionMap.put(CommandCategoryColumns.NAME, CommandCategoryColumns.NAME);
        sCommandCategoryProjectionMap.put(CommandCategoryColumns.ENGINE_TYPE, CommandCategoryColumns.ENGINE_TYPE);
        sCommandCategoryProjectionMap.put(CommandCategoryColumns.PRODUCT, CommandCategoryColumns.PRODUCT);
        
        // command projection map
        sCommandProjectionMap.put(CommandColumns._ID, CommandColumns._ID);
        sCommandProjectionMap.put(CommandColumns.ID, CommandColumns.ID);
        sCommandProjectionMap.put(CommandColumns.NAME, CommandColumns.NAME);   
        sCommandProjectionMap.put(CommandColumns._CATEGORY_ID, CommandColumns._CATEGORY_ID);
        sCommandProjectionMap.put(CommandColumns.CATEGORY_ID, CommandColumns.CATEGORY_ID);
        sCommandProjectionMap.put(CommandColumns.RAW_COMMAND, CommandColumns.RAW_COMMAND);
        sCommandProjectionMap.put(CommandColumns.TARGET, CommandColumns.TARGET);
        sCommandProjectionMap.put(CommandColumns.OPTION_TYPE, CommandColumns.OPTION_TYPE);
        sCommandProjectionMap.put(CommandColumns.OPTION_CSV, CommandColumns.OPTION_CSV);
    }
}
