/**
 * Inserts and edits commandCategorys from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
package com.roylaurie.arkown.android.app;

import java.util.ArrayList;

import com.roylaurie.arkown.android.Application;
import com.roylaurie.arkown.android.User;
import com.roylaurie.arkown.android.provider.ArkownContentProvider;
import com.roylaurie.arkown.android.provider.ArkownContentProvider.ColumnException;
import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.command.Command.OptionType;
import com.roylaurie.arkown.command.Command.Target;
import com.roylaurie.arkown.android.app.R;

import android.app.Activity;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Inserts and edits commandCategorys from the local database.
 * 
 * @author Roy Laurie <roy.laurie@roylaurie.com> RAL
 * @copyright 2011 Roy Laurie Software
 */
public final class EditCommandActivity extends Activity implements OnClickListener {
    private final static int OPTION_ID_BASE = 100;
    //private final static String OPTION_TAG = "optionTag";
    
    private Category mCategory = null;
    private Command mCommand = null;
    private int mLastOptionId = OPTION_ID_BASE - 1;
    
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    private OnItemSelectedListener mOptionTypeItemClickListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            showCustomListWidgets(( OptionType.values()[(int)id] == OptionType.CUSTOM_LIST ));
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };
    
    private OnClickListener mAddOptionOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout optionsLayout = (LinearLayout)findViewById(R.id.options);
            View optionView = layoutInflater.inflate(R.layout.edit_command_activity_option_item, null);
            
            ++mLastOptionId;
            optionView.findViewById(R.id.optionValue).setId(mLastOptionId);
            optionView.findViewById(R.id.removeOptionButton).setOnClickListener(mRemoveOptionOnClickListener);
            
            optionsLayout.addView(optionView);
            
        } 
    };    
    
    private OnClickListener mRemoveOptionOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            View layout = (View)v.getParent();
            mCommand.getOptions().remove(layout.getTag());
            layout.setVisibility(View.GONE);
        } 
    };
    
    private OnClickListener mCancelOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        } 
    };    
    
    static {
        sUriMatcher.addURI(
                ArkownContentProvider.AUTHORITY, "command_category/#",
                ArkownContentProvider.COMMAND_CATEGORY_ID
        );
        sUriMatcher.addURI(ArkownContentProvider.AUTHORITY, "command/#", ArkownContentProvider.COMMAND_ID);        
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_command_activity);
        
        Uri uri = getIntent().getData();
        Spinner targetSpinner = (Spinner)findViewById(R.id.commandTarget);
        Spinner optionTypeSpinner = (Spinner)findViewById(R.id.commandOptionType);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> optionTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout optionsLayout = (LinearLayout)findViewById(R.id.options);
        View optionView = null;
        View view = null;
        int i = 0;
        int selectedTargetIndex = -1;
        int selectedOptionTypeIndex = -1;
        
        switch (sUriMatcher.match(uri)) {
        case ArkownContentProvider.COMMAND_CATEGORY_ID:
            int commandCategoryId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
            mCategory = Application.getInstance().getSessionUser().getCommandCategory(commandCategoryId);
            mCommand = new Command();
            mCommand.setTarget(Target.SERVER);
            mCategory.addCommand(mCommand);
            break;
            
        case ArkownContentProvider.COMMAND_ID:
            int commandId = Integer.parseInt(getIntent().getData().getPathSegments().get(1));
            mCommand = Application.getInstance().getSessionUser().getCommand(commandId);
            mCategory = mCommand.getCategory();

            ((TextView)findViewById(R.id.commandName)).setText(mCommand.getName());
            ((TextView)findViewById(R.id.commandRawCommand)).setText(mCommand.getRawCommandString());
            break;
            
        default:
            throw new IllegalArgumentException("Unknown URI `" + uri + "`.");
        }        

        ((ImageView)findViewById(R.id.addOptionButton)).setOnClickListener(mAddOptionOnClickListener);        
        ((Button)findViewById(R.id.saveButton)).setOnClickListener(this);  
        ((Button)findViewById(R.id.cancelButton)).setOnClickListener(mCancelOnClickListener);
        
        // available command targets
        i = 0;
        for (Target t : Target.values()) {
            arrayAdapter.add(t.toName());
            
            if (mCommand.getTarget() == t) {
                selectedTargetIndex = i;
            }
            
            ++i;
        }
        
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetSpinner.setAdapter(arrayAdapter);
        targetSpinner.setSelection(selectedTargetIndex);
        
        // available option types
        i = 0;
        for (OptionType optionType : OptionType.values()) {
            optionTypeAdapter.add(optionType.toName());
            
            if (mCommand.getOptionType() == optionType) {
                selectedOptionTypeIndex = i;
            }
            
            ++i;
        }
        
        optionTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionTypeSpinner.setAdapter(optionTypeAdapter);
        optionTypeSpinner.setOnItemSelectedListener(mOptionTypeItemClickListener);
        optionTypeSpinner.setSelection(selectedOptionTypeIndex);        
        
        if (mCommand.getOptionType() != OptionType.CUSTOM_LIST) {
            showCustomListWidgets(false);
        }
        
        mLastOptionId = OPTION_ID_BASE - 1;
        for (String option : mCommand.getOptions()) {
            ++mLastOptionId;
            optionView = layoutInflater.inflate(R.layout.edit_command_activity_option_item, null);
            
            view =  optionView.findViewById(R.id.optionValue);
            view.setId(mLastOptionId);
            ((EditText)view).setText(option);
         
            view = optionView.findViewById(R.id.removeOptionButton);
            view.setOnClickListener(mRemoveOptionOnClickListener);

            optionsLayout.addView(optionView);  
        }
        
        return;
    }
    
    public void showCustomListWidgets(boolean isVisible) {
        int visible = ( isVisible ? View.VISIBLE : View.GONE );
        
        findViewById(R.id.commandOptionsLabel).setVisibility(visible);
        findViewById(R.id.addOptionButton).setVisibility(visible);
        findViewById(R.id.options).setVisibility(visible);
    }
    
    public void onClick(View view) {
        User user  = Application.getInstance().getSessionUser();
        LinearLayout optionsLayout = (LinearLayout)findViewById(R.id.options);
        ArrayList<String> options = new ArrayList<String>();
        String s = null;
        EditText optionView = null;
        Target target = null;
        OptionType optionType = null;
        int i;

        s = ((EditText)findViewById(R.id.commandName)).getText().toString().trim();
        mCommand.setName(s);
        
        s = ((EditText)findViewById(R.id.commandRawCommand)).getText().toString().trim();
        mCommand.setRawCommandString(s);
       
        target = Target.fromName(((Spinner)findViewById(R.id.commandTarget)).getSelectedItem().toString().trim());
        mCommand.setTarget(target);
        
        optionType = OptionType.fromName(
            ((Spinner)findViewById(R.id.commandOptionType)).getSelectedItem().toString().trim()
        );
        mCommand.setOptionType(optionType);
        
        if (optionType == OptionType.CUSTOM_LIST) {
            // find options.
            for (i = OPTION_ID_BASE; i <= mLastOptionId; ++i) {
                optionView = (EditText)optionsLayout.findViewById(i);
                if (optionView == null || !optionView.isShown()) {
                    continue;
                }
    
                s = optionView.getText().toString().trim();
                if (TextUtils.isEmpty(s)) {
                    continue;
                }
                
                options.add(s);
            }
        }
            
        mCommand.setOptions(options);
        
        try {
            user.saveCommandCategory(mCategory);
        } catch (ColumnException e) {            
            if (e.getColumn() == ArkownContentProvider.CommandColumns.NAME) {
                findViewById(R.id.commandName).requestFocus();
            } else if (e.getColumn() == ArkownContentProvider.CommandColumns.RAW_COMMAND) {
                findViewById(R.id.commandRawCommand).requestFocus();
            }
            
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        
        finish();       
    }
}
