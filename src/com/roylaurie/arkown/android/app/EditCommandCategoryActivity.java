/**
 * Inserts and edits commandCategorys from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
package com.roylaurie.arkown.android.app;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.android.provider.ArkownContentProvider.ColumnException;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.engine.EngineProductList;
import com.roylaurie.arkown.engine.EngineProductList.EngineProductPair;
import com.roylaurie.arkown.android.app.R;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Inserts and edits commandCategorys from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
public final class EditCommandCategoryActivity extends Activity implements OnClickListener {
    private Category mCategory = null;
    
    private OnClickListener mCancelOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        } 
    };     
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_command_category_activity);
        Uri uri = getIntent().getData();
        Spinner engineSpinner = (Spinner)findViewById(R.id.categoryEngineProduct);
        ArrayAdapter<String> engineArray = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        int selectedIndex = 0;
        int i = 0;
        
        
        ((Button)findViewById(R.id.saveButton)).setOnClickListener(this);
        ((Button)findViewById(R.id.cancelButton)).setOnClickListener(mCancelOnClickListener);

        if (uri.equals(ArkownContentProvider.COMMAND_CATEGORY_CONTENT_URI)) {
            mCategory = new Category();
        } else {
            int commandCategoryId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
            mCategory = Application.getInstance().getSessionUser().getCommandCategory(commandCategoryId);
            
            ((TextView)findViewById(R.id.categoryName)).setText(mCategory.getName());
        }        

        // available engines / products
        i = 0;
        selectedIndex = 0;
        for (EngineProductPair pair : EngineProductList.getInstance().getEngineProductList()) {
            engineArray.add(pair.toString());
            
            if (mCategory.getEngine() == pair.getEngine()) {
                if (pair.isProduct()) {
                    if (mCategory.getProduct() == pair.getProduct()) {
                        selectedIndex = i;
                    }
                } else { 
                    selectedIndex = i;
                }
            }        
            
            ++i;
        }        
        
        engineArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        engineSpinner.setAdapter(engineArray);        
        engineSpinner.setSelection(selectedIndex);        
        
        return;
    }
    
    public void onClick(View view) {
        User user  = Application.getInstance().getSessionUser();      
        String v = null;
        int index = 0;
        

        v = ((EditText)findViewById(R.id.categoryName)).getText().toString();
        mCategory.setName(v);

        // engine / product
        index = (int)((Spinner)findViewById(R.id.categoryEngineProduct)).getSelectedItemId();
        EngineProductPair pair = EngineProductList.getInstance().getEngineProductList().get(index);
        mCategory.setEngine(pair.getEngine());
        mCategory.setProduct(pair.getProduct()); // allow null to passed on

        try {
            user.saveCommandCategory(mCategory);
        } catch (ColumnException e) {            
            if (e.getColumn() == ArkownContentProvider.CommandCategoryColumns.NAME) {
                findViewById(R.id.categoryName).requestFocus();
            }
            
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        
        finish();       
    }
}
