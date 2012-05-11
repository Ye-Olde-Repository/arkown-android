/**
 * 
 */
package com.roylaurie.arkown.android.app;

import com.roylaurie.arkown.android.app.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * @author rlaurie
 *
 */
public class HomeTabActivity extends TabActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.home_tab_activity);

        //Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab


        
        // server list tab
        intent = new Intent().setClass(this, ServerListActivity.class);
        spec = tabHost.newTabSpec("servers").setIndicator("Servers").setContent(intent);
        tabHost.addTab(spec);  
        
        intent = new Intent().setClass(this, CommandListActivity.class);
        spec = tabHost.newTabSpec("commands").setIndicator("Commands").setContent(intent);
        tabHost.addTab(spec);  
        
        tabHost.setCurrentTabByTag("servers");
    }
}
