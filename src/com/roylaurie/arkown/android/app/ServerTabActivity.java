/**
 * 
 */
package com.roylaurie.arkown.android.app;

import com.roylaurie.arkown.android.app.R;

import android.app.TabActivity;
import android.content.Intent;
//import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * @author rlaurie
 *
 */
public class ServerTabActivity extends TabActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_tab_activity);

        //Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab
        Intent localIntent = getIntent();
        
        // server list tab
        intent = new Intent().setClass(this, ServerInfoActivity.class);
        intent.setAction(localIntent.getAction());
        intent.setData(localIntent.getData());
        spec = tabHost.newTabSpec("server").setIndicator("Server").setContent(intent);
        tabHost.addTab(spec);  
        
        intent = new Intent().setClass(this, ServerClientListActivity.class);
        intent.setAction(localIntent.getAction());
        intent.setData(localIntent.getData());
        spec = tabHost.newTabSpec("players").setIndicator("Players").setContent(intent);
        tabHost.addTab(spec);  
        
        intent = new Intent().setClass(this, ServerConsoleActivity.class);
        intent.setAction(localIntent.getAction());
        intent.setData(localIntent.getData());
        spec = tabHost.newTabSpec("console").setIndicator("Console").setContent(intent);
        tabHost.addTab(spec);          
        
        tabHost.setCurrentTabByTag("server");
    }
}
