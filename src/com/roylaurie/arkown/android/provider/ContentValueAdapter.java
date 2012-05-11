package com.roylaurie.arkown.android.provider;

import android.content.ContentValues;

public interface ContentValueAdapter<O> {
    ContentValues toContentValues(O object);
    O toObject(ContentValues values);
}
