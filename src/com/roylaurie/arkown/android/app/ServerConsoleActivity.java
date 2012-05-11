/**
 * 
 */
package com.roylaurie.arkown.android.app;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.app.R;
import com.roylaurie.arkown.server.Server;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

/**
 * @author rlaurie
 *
 */
public class ServerConsoleActivity extends Activity implements OnClickListener, OnEditorActionListener {
    private Server mServer = null;
    private String mOutputText = "";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_console_activity);
        
        int serverId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
        EditText inputView = (EditText)findViewById(R.id.consoleInput);
        
        mServer = Application.getInstance().getSessionUser().getServer(serverId);

        refreshView(false);
        
       ((Button)findViewById(R.id.sendButton)).setOnClickListener(this);
       inputView.setOnEditorActionListener(this);
       inputView.setImeOptions(EditorInfo.IME_ACTION_SEND);
       inputView.setImeActionLabel("send", EditorInfo.IME_ACTION_SEND);
        
       return;
    }
    
    public void refreshView(boolean forcePull) {
        ((TextView)findViewById(R.id.consoleOutput)).setText(mOutputText);
    }
    
    @Override
    public void onClick(View view) {
        EditText inputView = (EditText)findViewById(R.id.consoleInput);
        String inputText = inputView.getText().toString().trim();
        
        mOutputText += "> " + inputText + "\n";
        
        try {
            String result = mServer.sendCommand(inputText);
            if (result.length() != 0) {
                mOutputText +=  result.trim() + "\n";
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            mOutputText += e.getMessage() + "\n";
        }
        
        inputView.clearComposingText();
        inputView.setText("");
        ((ScrollView)findViewById(R.id.consoleOutputScroller)).fullScroll(ScrollView.FOCUS_DOWN);
        refreshView(false);
        inputView.requestFocus();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
            return true;
        }
        
        onClick(((Button)findViewById(R.id.sendButton)));
        return true;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.server_console_activity, menu);
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
}
