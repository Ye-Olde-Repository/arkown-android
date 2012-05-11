/**
 * Inserts and edits servers from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
package com.roylaurie.arkown.android.app;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.android.provider.ArkownContentProvider.ColumnException;
import com.roylaurie.arkown.engine.Engine;
import com.roylaurie.arkown.engine.EngineProductList;
import com.roylaurie.arkown.engine.Connection.ConnectionException;
import com.roylaurie.arkown.engine.Connection.Credentials;
import com.roylaurie.arkown.engine.Engine.Capability;
import com.roylaurie.arkown.engine.Engine.Product;
import com.roylaurie.arkown.engine.EngineProductList.EngineProductPair;
import com.roylaurie.arkown.engine.EngineType;
import com.roylaurie.arkown.engine.Query.QueryException;
import com.roylaurie.arkown.android.Application.Issue;
import com.roylaurie.arkown.android.app.R;
import com.roylaurie.arkown.server.Server;
import com.roylaurie.arkown.server.json.ServerJson;
import com.roylaurie.modeljson.exception.JsonException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Inserts and edits servers from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
public class EditServerActivity extends Activity implements OnClickListener {
    private Server mServer = null;
    private boolean mIsPortDefault = true;
    
    private OnClickListener mCancelOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        } 
    };      
    
    private OnClickListener mProxyClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).isChecked()) {
                ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setChecked(false);
                startActivityForResult(
                    new Intent(EditServerActivity.this, ProxyAgreementActivity.class),
                    Application.REQUEST_CODE_PROXY_TERMS
                );
            }
        } 
    };     
    
    /**
     * Toggle administration options display when credentials have been filled out (partially or complete).
     */
    private TextWatcher mCredentialsChangeListener = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable edit) {
            updateCredentials();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    };     
    
    /**
     * Register whether the port is should still be an engine default or not.
     */
    private TextWatcher mPortChangeListener = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable edit) {
            Engine engine = getSelectedEngine();
            String portStr = edit.toString();
            int port = 0;
            
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) { }            
            
            if (port < 1) { // reset default status if empty
                mIsPortDefault = true;
            } else if (engine == null || port != engine.getDefaultQueryPort()) {
                mIsPortDefault = false;
            }
            
            return;
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };
    
    private OnItemSelectedListener mEngineTypeItemClickListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateEngine();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            updateEngine();
        }
    };    
    
    private void updateCredentials() {
        boolean isAdmin = (
                ((EditText)findViewById(R.id.serverCredentialUsername)).getText().toString().length() > 1
                || ((EditText)findViewById(R.id.serverCredentialPassword)).getText().toString().length() > 1
            );
            
        findViewById(R.id.serverQueryProxyAllowedLabel).setVisibility((isAdmin ? View.VISIBLE : View.GONE));
        findViewById(R.id.serverQueryProxyAllowed).setVisibility((isAdmin ? View.VISIBLE : View.GONE));        
    }
    
    private void updateEngine() {
        Engine engine = getSelectedEngine();
        
        // set default port if port untouched
        if (mIsPortDefault) {
            ((EditText)findViewById(R.id.serverPort)).setText(Integer.toString(engine.getDefaultQueryPort()));
        }
        
        // display/hide username credential
        if (!engine.hasCapability(Capability.CREDENTIAL_USERNAME)) {
            findViewById(R.id.serverCredentialUsernameLabel).setVisibility(View.GONE);
            findViewById(R.id.serverCredentialUsername).setVisibility(View.GONE);
        } else {
            findViewById(R.id.serverCredentialUsernameLabel).setVisibility(View.VISIBLE);
            findViewById(R.id.serverCredentialUsername).setVisibility(View.VISIBLE);
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_server_activity);
        Uri uri = getIntent().getData();
        TextView portView = ((TextView)findViewById(R.id.serverPort));
        Spinner engineSpinner = (Spinner)findViewById(R.id.serverEngine);
        ArrayAdapter<String> engineArray = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        Credentials credentials = null;
        EngineType serverEngineType = null;
        Enum<? extends Product> product = null;
        int i = 0;
        int selectedIndex = 0;

        if (uri.equals(ArkownContentProvider.SERVER_CONTENT_URI)) {
            mServer = null;
            ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setChecked(false);
        } else {
            int serverId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
            mServer = Application.getInstance().getSessionUser().getServer(serverId);
            credentials = mServer.getCredentials();
            
            ((TextView)findViewById(R.id.serverHostname)).setText(mServer.getHostname());
            portView.setText(Integer.toString(mServer.getPort()));
            
            if (mServer.getEngine().hasCapability(Capability.CREDENTIAL_USERNAME)) {
                ((TextView)findViewById(R.id.serverCredentialUsername)).setText(credentials.getPassword());
            }
            
            ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setChecked(mServer.isQueryProxyAllowed());
            
            ((TextView)findViewById(R.id.serverCredentialPassword)).setText(credentials.getPassword());
            mIsPortDefault = false;
            
            serverEngineType = mServer.getEngine().getType();
            product = mServer.getProduct();
        }        
        
        // engine view
        i = 0;
        for (EngineProductPair pair : EngineProductList.getInstance().getEngineProductList()) {
            engineArray.add(pair.toString());
            
            if (serverEngineType == pair.getEngine().getType()) {
                if (pair.isProduct()) {
                    if (product == pair.getProduct()) {
                        selectedIndex = i;
                    }
                } else { 
                    selectedIndex = i;
                }
            }        
            
            ++i;
        }
        
        engineArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        engineSpinner.setOnItemSelectedListener(mEngineTypeItemClickListener);
        engineSpinner.setAdapter(engineArray);        
        engineSpinner.setSelection(selectedIndex);
        
        portView.addTextChangedListener(mPortChangeListener);
        
        ((Button)findViewById(R.id.saveButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.cancelButton)).setOnClickListener(mCancelOnClickListener);
        ((EditText)findViewById(R.id.serverCredentialUsername)).addTextChangedListener(mCredentialsChangeListener);
        ((EditText)findViewById(R.id.serverCredentialPassword)).addTextChangedListener(mCredentialsChangeListener);
        ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setOnClickListener(mProxyClickListener);
        
        updateEngine();
        updateCredentials();
       
        return;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case Application.REQUEST_CODE_PROXY_TERMS:
            if (resultCode != ProxyAgreementActivity.RESULT_AGREED) {
                ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setChecked(false);
            } else {
                ((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).setChecked(true);
            }
            break;
        }
    }
    
    private Engine getSelectedEngine() {
        int index = (int)((Spinner)findViewById(R.id.serverEngine)).getSelectedItemId();
        return EngineProductList.getInstance().getEngineProductList().get(index).getEngine();
    }
    
    private int getSelectedPort() {
        int port = 0;
        String portStr = ((EditText)findViewById(R.id.serverPort)).getText().toString();
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            if (portStr.length() > 1) { // then this isn't blank (0), it's invalid (-1)
                port = -1;
            }
        }

        return port;
    }
    
    public void onClick(View view) {
        User user  = Application.getInstance().getSessionUser();      
        String v = null;
        Engine engine = getSelectedEngine();
        int port = getSelectedPort();
        Credentials credentials = null;
        String password = null;
        
        if (mServer == null) { // new server - factory it
            mServer = Server.factory(engine);
        } else if (engine != mServer.getEngine()) { // engine changed. factory new server and copy
            Server original = mServer;
            mServer = Server.factory(engine);
            mServer.setApplicationDatabaseId(original.getApplicationDatabaseId());
            mServer.setDatabaseId(original.getDatabaseId());
        }  
        
        credentials = mServer.getCredentials();
        
        v = ((EditText)findViewById(R.id.serverHostname)).getText().toString();
        mServer.setHostname(v);

        if (port < 1) {
            findViewById(R.id.serverPort).requestFocus();
            
            if (port == 0) {
                Toast.makeText(this, "Invalid port.", Toast.LENGTH_LONG).show();
            } else { // port == -1
                Toast.makeText(this, "Port is required. port.", Toast.LENGTH_LONG).show();
            }
            return;            
        }
        
        mServer.setPort(port);
        
        password =  ((EditText)findViewById(R.id.serverCredentialPassword)).getText().toString();
        credentials.setPassword(password);        
        
        if (engine.hasCapability(Capability.CREDENTIAL_USERNAME)) {
            v =  ((EditText)findViewById(R.id.serverCredentialUsername)).getText().toString();
            
            // both the user and the pass are required - complain if one or the other is missing
            if (v.length() == 0 && password.length() > 0) {
                findViewById(R.id.serverCredentialUsername).requestFocus();
                Toast.makeText(this, "Username is required.", Toast.LENGTH_LONG).show();
                return;
            } else if (password.length() == 0 && v.length() > 0) {
                findViewById(R.id.serverCredentialPassword).requestFocus();
                Toast.makeText(this, "Password is required.", Toast.LENGTH_LONG).show();
                return;
            }
            
            credentials.setUsername(v);            
        } else {
            credentials.setUsername(null);
        }
        
        if (credentials.valid()) { // setup admin config 
            mServer.setQueryProxyAllowed(((CheckBox)findViewById(R.id.serverQueryProxyAllowed)).isChecked());
        } else { // no admin priviliges - clear admin config
            mServer.setQueryProxyAllowed(true);
        }
        
        if (mServer.needsProxyPull() && !mServer.getCredentials().valid()) {
            mServer.setQueryProxyAllowed(true); // set for posterity
        }
        
        // test credentials if provided to ensure validity, credentials are cleared if wrong
        if (!mServer.validateCredentials()) {
            Toast.makeText(this, "Invalid server administration credentials.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ensure that server is queryable (either directly or by proxy) before allowing a save.
        try {
            mServer.pull();
        } catch (ConnectionException e) {
            if (mServer.needsProxyPull() && e.getCause() instanceof JsonException) {
                switch(((JsonException)e.getCause()).getErrorCode()) {
                case API_VERSION:
                    Application.getInstance().registerIssue(Issue.UPGRADE);
                    startActivity(new Intent(this, IssueActivity.class));
                    break;
                    
                default:
                    Toast.makeText(this, "Unable to connect to API server.", Toast.LENGTH_LONG).show();
                } 
            } else {            
                Toast.makeText(this, "Unable to connect to server.", Toast.LENGTH_LONG).show();
            }
            
            return;
        } catch (QueryException e) {
            if (mServer.needsProxyPull() && e.getCause() instanceof JsonException) {
                switch(((JsonException)e.getCause()).getErrorCode()) {
                case NOT_FOUND:
                    Toast.makeText(
                        this,
                        "A server administrator must make this server publically available.",
                        Toast.LENGTH_LONG
                    ).show();
                    break;

                default:
                    Toast.makeText(this, "Unable to query server.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Unable to query server.", Toast.LENGTH_LONG).show();
            }
                
            return;
        }
        
        try {
            user.saveServer(mServer);
        } catch (ColumnException e) {            
            if (e.getColumn() == ArkownContentProvider.ServerColumns.HOSTNAME) {
                findViewById(R.id.serverHostname).requestFocus();
            } else if (e.getColumn() == ArkownContentProvider.ServerColumns.PORT) {
                findViewById(R.id.serverPort).requestFocus();
            } else if (e.getColumn() == ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME) {
                findViewById(R.id.serverCredentialUsername).requestFocus();
            } else if (e.getColumn() == ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD) {
                findViewById(R.id.serverCredentialPassword).requestFocus();
            }
            
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (mServer.isQueryProxyAllowed() && mServer.getCredentials().valid()) {  // write if admin
            ServerJson json = new ServerJson();
            try {
                json.write(mServer);
            } catch (JsonException e) {
                switch(e.getErrorCode()) {
                case DUPLICATE:
                    Server original = (Server)e.getRemoteObject();
                    
                    // copy duplicate information over. let the user review before saving
                    mServer.setDatabaseId(original.getDatabaseId());                   
                    break;
                    
                case PERMISSION:
                    Toast.makeText(this, "You don't have update permissions on the API server", Toast.LENGTH_LONG).show();
                    return;
                    
                case NOT_FOUND: // attempt to find the new database id for this server or re-insert
                    mServer.setDatabaseId(0);
                    try {
                        json.write(mServer);
                    } catch (JsonException e2) {
                        switch(e.getErrorCode()) {
                        case DUPLICATE:
                            Server original2 = (Server)e2.getRemoteObject();
                            mServer.setDatabaseId(original2.getDatabaseId());
                            break;

                        case PERMISSION:
                            Toast.makeText(this, "You don't have update permissions on the API server", Toast.LENGTH_LONG).show();
                            return;                            
                            
                        default:
                            Toast.makeText(this, "Unable to update the API server.", Toast.LENGTH_LONG).show();
                            return;                        
                        }
                    }                    
                    break;
                default:
                    Toast.makeText(this, "Unable to update the API server.", Toast.LENGTH_LONG).show();
                    return;                        
                }
            }
            
            // save server with database id updated
            try {
                user.saveServer(mServer);
            } catch (ColumnException e) {            
                if (e.getColumn() == ArkownContentProvider.ServerColumns.HOSTNAME) {
                    findViewById(R.id.serverHostname).requestFocus();
                } else if (e.getColumn() == ArkownContentProvider.ServerColumns.PORT) {
                    findViewById(R.id.serverPort).requestFocus();
                } else if (e.getColumn() == ArkownContentProvider.ServerColumns.CREDENTIAL_USERNAME) {
                    findViewById(R.id.serverCredentialUsername).requestFocus();
                } else if (e.getColumn() == ArkownContentProvider.ServerColumns.CREDENTIAL_PASSWORD) {
                    findViewById(R.id.serverCredentialPassword).requestFocus();
                }
                
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }            
        }

        Toast.makeText(this, "Saved.", Toast.LENGTH_LONG).show();
        finish();      
    }
}
