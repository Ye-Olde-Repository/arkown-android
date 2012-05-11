/**
 * 
 */
package com.roylaurie.arkown.android.provider;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;

import com.roylaurie.arkown.engine.Engine;
import com.roylaurie.arkown.engine.EngineType;
import com.roylaurie.arkown.server.Server;

/**
 * @author rlaurie
 *
 */
public final class ServerContentValueAdapter implements ContentValueAdapter<Server> {
    public ServerContentValueAdapter() {
    }

    @Override
    public ContentValues toContentValues(Server server) {
        return toValues(server);
    }

    @Override
    public Server toObject(ContentValues values) {
        return toServer(values);
    }
    
    public static Server toServer(ContentValues values) {
        Engine engine = EngineType.valueOf(
            values.getAsString(ArkownContentProvider.ServerColumns.ENGINE_TYPE)
        ).getEngine();
        
        Server server = Server.factory(engine);

        server.setApplicationDatabaseId(values.getAsInteger(ArkownContentProvider.ServerColumns._ID));   
        server.setDatabaseId(values.getAsInteger(ArkownContentProvider.ServerColumns.ID));   
        server.setHostname(values.getAsString(ArkownContentProvider.ServerColumns.HOSTNAME));
        server.setPort(values.getAsInteger(ArkownContentProvider.ServerColumns.PORT));

        server.getCredentials().setUsername(
                values.getAsString(ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME)
        );   
        server.getCredentials().setPassword(
                values.getAsString(ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD)
        );   
        server.setQueryProxyAllowed(
            ( values.getAsInteger(ArkownContentProvider.ServerColumns.IS_QUERY_PROXY_ALLOWED) != 0 )
        );
        
        return server;
    }
    
    public static ContentValues toValues(Server server) {
        ContentValues values = new ContentValues(5);
        values.put(ArkownContentProvider.ServerColumns._ID, server.getApplicationDatabaseId());
        values.put(ArkownContentProvider.ServerColumns.ID, server.getDatabaseId());
        values.put(ArkownContentProvider.ServerColumns.HOSTNAME, server.getHostname());
        values.put(ArkownContentProvider.ServerColumns.PORT, server.getPort());
        values.put(
                ArkownContentProvider.ServerColumns.ENGINE_TYPE,
                server.getEngine().getType().toString()
        );
        values.put(ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME, server.getCredentials().getUsername());
        values.put(ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD, server.getCredentials().getPassword());
        values.put(ArkownContentProvider.ServerColumns.IS_QUERY_PROXY_ALLOWED, ( server.isQueryProxyAllowed() ? 1 : 0 )); 
        
        return values;        
    }
    
    public static ContentValues buildValuesTemplate() {
        ContentValues values = new ContentValues(5);
        
        values.put(ArkownContentProvider.ServerColumns._ID, "");
        values.put(ArkownContentProvider.ServerColumns.ID, "");
        values.put(ArkownContentProvider.ServerColumns.HOSTNAME, "");
        values.put(ArkownContentProvider.ServerColumns.PORT, "");
        values.put(ArkownContentProvider.ServerColumns.ENGINE_TYPE, "");
        values.put(ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME, "");
        values.put(ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD, "");        
        values.put(ArkownContentProvider.ServerColumns.IS_QUERY_PROXY_ALLOWED, 1);   
        
        return values;
    }
    
    public static ArrayList<Server> fromCursor(Cursor cursor) {
        ArrayList<Server> results = new ArrayList<Server>();
        Server server = null;
        Engine engine = null;
        
        if (cursor == null || !cursor.moveToFirst()) {
            return results;
        }
        
        int localIdIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns._ID);
        int idIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.ID);
        int hostnameIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.HOSTNAME);
        int portIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.PORT);
        int engineTypeIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.ENGINE_TYPE);
        int usernameIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME);
        int passwordIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD);
        int queryProxyAllowedIndex = cursor.getColumnIndex(ArkownContentProvider.ServerColumns.IS_QUERY_PROXY_ALLOWED);
        
        do {
            engine = EngineType.valueOf(cursor.getString(engineTypeIndex)).getEngine();
            server = Server.factory(engine);
                
            server.setApplicationDatabaseId(cursor.getInt(localIdIndex));
            server.setDatabaseId(cursor.getInt(idIndex));
            server.setHostname(cursor.getString(hostnameIndex));
            server.setPort(cursor.getInt(portIndex));
            server.getCredentials().setUsername(cursor.getString(usernameIndex));
            server.getCredentials().setPassword(cursor.getString(passwordIndex));
            server.setQueryProxyAllowed(( cursor.getInt(queryProxyAllowedIndex) != 0 ));
            results.add(server);
        } while (cursor.moveToNext());
        
        return results;
    }
}
