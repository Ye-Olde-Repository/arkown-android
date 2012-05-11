/**
 * 
 */
package com.roylaurie.arkown.android.provider;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;

import com.roylaurie.arkown.command.Command;
import com.roylaurie.arkown.command.Command.Target;
import com.roylaurie.arkown.command.Command.OptionType;

/**
 * @author rlaurie
 *
 */
public final class CommandContentValueAdapter implements ContentValueAdapter<Command> {
    public CommandContentValueAdapter() {
    }

    @Override
    public ContentValues toContentValues(Command command) {
        return toValues(command);
    }

    @Override
    public Command toObject(ContentValues values) {
        return toCommand(values);
    }
    
    public static ContentValues buildValuesTemplate() {
        ContentValues values = new ContentValues(9);
        
        values.put(ArkownContentProvider.CommandColumns._ID, "");
        values.put(ArkownContentProvider.CommandColumns.ID, "");
        values.put(ArkownContentProvider.CommandColumns._CATEGORY_ID, "");
        values.put(ArkownContentProvider.CommandColumns.CATEGORY_ID, "");
        values.put(ArkownContentProvider.CommandColumns.NAME, "");
        values.put(ArkownContentProvider.CommandColumns.RAW_COMMAND, "");
        values.put(ArkownContentProvider.CommandColumns.TARGET, "");
        values.put(ArkownContentProvider.CommandColumns.OPTION_TYPE, "");
        values.put(ArkownContentProvider.CommandColumns.OPTION_CSV, "");

        return values;
    }        
    
    public static Command toCommand(ContentValues values) {
        Command command = new Command();
        command.setApplicationDatabaseId(values.getAsInteger(ArkownContentProvider.CommandColumns._ID));
        command.setDatabaseId(values.getAsInteger(ArkownContentProvider.CommandColumns.ID));
        command.setCategoryApplicationDatabaseId(values.getAsInteger(ArkownContentProvider.CommandColumns._CATEGORY_ID));
        command.setCategoryDatabaseId(values.getAsInteger(ArkownContentProvider.CommandColumns.CATEGORY_ID));
        command.setName(values.getAsString(ArkownContentProvider.CommandColumns.NAME));   
        command.setRawCommandString(values.getAsString(ArkownContentProvider.CommandColumns.RAW_COMMAND));  
        command.setTarget(Target.valueOf(values.getAsString(ArkownContentProvider.CommandColumns.TARGET)));  
        command.setOptionType(OptionType.valueOf(values.getAsString(ArkownContentProvider.CommandColumns.RAW_COMMAND))); 
        
        for (String option : values.getAsString(ArkownContentProvider.CommandColumns.OPTION_CSV).split(",")) {
            if (option.length() > 0) {
                command.addOption(option);
            }
        }

        
        return command;
    }
    
    public static ContentValues toValues(Command command) {
        ContentValues values = buildValuesTemplate();
        values.put(ArkownContentProvider.CommandColumns._ID, command.getApplicationDatabaseId());
        values.put(ArkownContentProvider.CommandColumns.ID, command.getDatabaseId());
        values.put(ArkownContentProvider.CommandColumns._CATEGORY_ID, command.getCategoryApplicationDatabaseId());
        values.put(ArkownContentProvider.CommandColumns.CATEGORY_ID, command.getCategoryDatabaseId());
        values.put(ArkownContentProvider.CommandColumns.NAME, command.getName());
        values.put(ArkownContentProvider.CommandColumns.RAW_COMMAND, command.getRawCommandString());
        values.put(ArkownContentProvider.CommandColumns.TARGET, command.getTarget().toString());
        values.put(ArkownContentProvider.CommandColumns.OPTION_TYPE, command.getOptionType().toString());
        
        String options = "";
        int i = 0;
        for (String option : command.getOptions()) {
            options += option;
            if ((i+1) < command.getOptions().size()) {
                options += ",";
            }
            
            ++i;
        }
        
        values.put(ArkownContentProvider.CommandColumns.OPTION_CSV, options);
        
        return values;        
    }
    
    public static ArrayList<Command> fromCursor(Cursor cursor) {
        ArrayList<Command> results = new ArrayList<Command>();
        Command command = null;
        
        if (cursor == null || !cursor.moveToFirst()) {
            return results;
        }
        
        int localIdIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns._ID);
        int idIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.ID);
        int localCategoryIdIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns._CATEGORY_ID);
        int categoryIdIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.CATEGORY_ID);
        int nameIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.NAME);
        int rawCommandIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.RAW_COMMAND);
        int targetIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.TARGET);
        int optionTypeIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.OPTION_TYPE);
        int optionCsvIndex = cursor.getColumnIndex(ArkownContentProvider.CommandColumns.OPTION_CSV);

        do {
            command = new Command();
            command.setApplicationDatabaseId(cursor.getInt(localIdIndex));
            command.setDatabaseId(cursor.getInt(idIndex));
            command.setCategoryApplicationDatabaseId(cursor.getInt(localCategoryIdIndex));
            command.setCategoryDatabaseId(cursor.getInt(categoryIdIndex));
            command.setName(cursor.getString(nameIndex));
            command.setRawCommandString(cursor.getString(rawCommandIndex));
            command.setTarget(Target.valueOf(cursor.getString(targetIndex).toUpperCase()));
            command.setOptionType(OptionType.valueOf(cursor.getString(optionTypeIndex).toUpperCase()));

            for (String option : cursor.getString(optionCsvIndex).split(",")) {
                if (option.length() > 0) {
                    command.addOption(option);
                }
            }            
            
            results.add(command);
        } while (cursor.moveToNext());
        
        return results;
    }
}
