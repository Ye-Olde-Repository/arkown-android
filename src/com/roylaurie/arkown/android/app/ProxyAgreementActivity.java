/**
 * Copyright (C) 2011 Roy Laurie Software <http://www.roylaurie.com>
 */
package com.roylaurie.arkown.android.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Displays the proxy usage terms and conditioins of use.
 * Returns the result RESULT_AGREED or View.RESULT_CANCELED.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 */
public final class ProxyAgreementActivity extends Activity implements OnClickListener {
    public static final int RESULT_AGREED = 1001;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxy_agreement_activity);
        
        findViewById(R.id.agreeButton).setOnClickListener(this);
        findViewById(R.id.cancelButton).setOnClickListener(this);
        findViewById(R.id.cancelButton).requestFocus();
    }

    @Override
    public void onClick(View v) {
        int result = RESULT_CANCELED;
        
        if (v.getId() == R.id.agreeButton) {
            result = RESULT_AGREED;
        }
        
        setResult(result);
        finish();
    }
}
