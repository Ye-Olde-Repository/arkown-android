/**
 * 
 */
package com.roylaurie.arkown.android;

import com.roylaurie.arkown.android.Application.Issue;
import com.roylaurie.arkown.engine.Connection.ConnectionException;
import com.roylaurie.arkown.engine.Query.QueryException;
import com.roylaurie.arkown.server.Server;
import com.roylaurie.modeljson.exception.JsonApiVersionException;

/**
 * @author rlaurie
 *
 */
public final class ServerPuller {
    private ServerPuller() {}
    
    public static void pull(Server server, boolean forcePull) {
        long nextPull = server.getLastPullTime() + (Application.getInstance().getPullInterval() * 1000);
        
        if (forcePull || nextPull <= System.currentTimeMillis()) {
            try {
                server.pull();
            } catch (QueryException e) {
                return;
            } catch (ConnectionException e) {
                if (e.getCause() != null && e.getCause() instanceof JsonApiVersionException) {
                    Application.getInstance().registerIssue(Issue.UPGRADE);
                }
                return;
            }
        }
        
        return;
    }
}
