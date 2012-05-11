/**
 * 
 */
package com.roylaurie.arkown.android.provider;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;

import com.roylaurie.arkown.command.Category;
import com.roylaurie.arkown.engine.Engine;
import com.roylaurie.arkown.engine.EngineType;


/**
 * @author rlaurie
 *
 */
public final class CommandCategoryContentValueAdapter implements ContentValueAdapter<Category> {
    private static final int NUM_VALUES = 2;
    
    public CommandCategoryContentValueAdapter() {}

    @Override
    public ContentValues toContentValues(Category category) {
        return toValues(category);
    }

    @Override
    public Category toObject(ContentValues values) {
        return toServer(values);
    }
    
    public static ContentValues buildValuesTemplate() {
        ContentValues values = new ContentValues(NUM_VALUES);
        
        values.put(ArkownContentProvider.CommandCategoryColumns._ID, "");
        values.put(ArkownContentProvider.CommandCategoryColumns.ID, "");
        values.put(ArkownContentProvider.CommandCategoryColumns.NAME, "");
        values.put(ArkownContentProvider.CommandCategoryColumns.ENGINE_TYPE, "");
        values.put(ArkownContentProvider.CommandCategoryColumns.PRODUCT, "");
      

        return values;
    }    
    
    public static Category toServer(ContentValues values) {
        Category category = new Category();
        String productToken = values.getAsString(ArkownContentProvider.CommandCategoryColumns.PRODUCT);
        Engine engine = EngineType.valueOf(
            values.getAsString(ArkownContentProvider.ServerColumns.ENGINE_TYPE)
        ).getEngine();        
        
        category.setApplicationDatabaseId(values.getAsInteger(ArkownContentProvider.CommandCategoryColumns._ID));   
        category.setDatabaseId(values.getAsInteger(ArkownContentProvider.CommandCategoryColumns.ID));  
        category.setName(values.getAsString(ArkownContentProvider.CommandCategoryColumns.NAME));  
        category.setEngine(engine);
        
        if (productToken.length() > 0) {
            category.setProduct(engine.productValueOf((productToken)));
        }
        
        return category;
    }
    
    public static ContentValues toValues(Category category) {
        ContentValues values = buildValuesTemplate();
        values.put(ArkownContentProvider.CommandCategoryColumns._ID, category.getApplicationDatabaseId());
        values.put(ArkownContentProvider.CommandCategoryColumns.ID, category.getDatabaseId());
        values.put(ArkownContentProvider.CommandCategoryColumns.NAME, category.getName());
        values.put(ArkownContentProvider.CommandCategoryColumns.ENGINE_TYPE, category.getEngine().getType().toString());
        
        if (category.hasProductFilter()) {
            values.put(ArkownContentProvider.CommandCategoryColumns.PRODUCT, category.getProduct().toString());
        }
        
        return values;        
    }
    
    public static ArrayList<Category> fromCursor(Cursor cursor) {
        ArrayList<Category> results = new ArrayList<Category>();
        Category category = null;
        String productToken = null;
        
        if (cursor == null || !cursor.moveToFirst()) {
            return results;
        }
        
        int applicationIdIndex = cursor.getColumnIndex(ArkownContentProvider.CommandCategoryColumns._ID);
        int idIndex = cursor.getColumnIndex(ArkownContentProvider.CommandCategoryColumns.ID);
        int nameIndex = cursor.getColumnIndex(ArkownContentProvider.CommandCategoryColumns.NAME);
        int engineIndex = cursor.getColumnIndex(ArkownContentProvider.CommandCategoryColumns.ENGINE_TYPE);
        int productIndex = cursor.getColumnIndex(ArkownContentProvider.CommandCategoryColumns.PRODUCT);

        do {
            category = new Category();
            category.setApplicationDatabaseId(cursor.getInt(applicationIdIndex));
            category.setDatabaseId(cursor.getInt(idIndex));
            category.setName(cursor.getString(nameIndex));
            category.setEngine(EngineType.valueOf(cursor.getString(engineIndex)).getEngine());
            
            // product
            productToken = cursor.getString(productIndex);
            if (productToken.length() > 0) {
                category.setProduct(category.getEngine().productValueOf(productToken));
            }
            
            results.add(category);
        } while (cursor.moveToNext());
        
        return results;
    }
}
