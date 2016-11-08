/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/*
 * DsTuningProvider.java
 *
 * Defines a ContentProvider that gives access to Dolby Tuning database on the device.
 */

package com.dolby.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import com.dolby.api.DsTuningProviderContent;

public class DsTuningProvider extends ContentProvider
{
    private static final String TAG = "DsTuningProvider";

    /**
     * URI matcher constant for the URI of the entire list.
     *
     * @internal
     */
    private static final int ITEM_ALL = 1;
    
    /**
     * URI matcher constant for the URI of an individual item.
     *
     * @internal
     */
    private static final int ITEM_ID = 2;

    /**
     * Defines a helper object that matches content URIs to table-specific parameters.
     *
     * @internal
     */
    private static final UriMatcher sMatcher;

    /**
     * Stores the MIME types served by this provider.
     *
     * @internal
     */
    private static final SparseArray<String> sMimeTypes;

    /*
     * Initializes meta-data used by the content provider:
     * - UriMatcher that maps content URIs to codes
     * - MimeType array that returns the custom MIME type of a table
     */
    static{
        /**
            * Creates an object that associates content URIs with numeric codes.
            */
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        /*
            * Sets up an array that maps content URIs to MIME types.
            */
        sMimeTypes = new SparseArray<String>();
                
        /*
            * Adds a URI "match" entry that maps table content URIs to a numeric code.
            */
        sMatcher.addURI(DsTuningProviderContent.AUTHORITY, DsTuningProviderContent.TNAME, ITEM_ALL);
        
        /*
            * Adds a URI "match" entry that maps item of table content URIs to a numeric code.
            */
        sMatcher.addURI(DsTuningProviderContent.AUTHORITY, DsTuningProviderContent.TNAME+"/#", ITEM_ID);
        
        /*
            * Specifies a custom MIME type for the Ds Tuning URL table.
            */
        sMimeTypes.put(ITEM_ALL,"vnd.android.cursor.dir/"+DsTuningProviderContent.TNAME);
        
        /*
            * Specifies a custom MIME type for the item of Ds Tuning URL table.
            */
        sMimeTypes.put(ITEM_ALL,"vnd.android.cursor.item/"+DsTuningProviderContent.TNAME);
    }

    /**
     * Initializes the content provider. 
     */
    @Override
    public boolean onCreate()
    {
        //Todo...
        
        return true;
    }

    /**
     * Delete one or more rows in a table.
     * @see android.content.ContentProvider#delete(Uri, String, String[])
     * @param uri The content URI
     * @param selection The SQL WHERE string. Use "?" to mark places that should be substituted by
     * values in selectionArgs.
     * @param selectionArgs An array of values that are mapped to each "?" in selection. If no "?"
     * are used, set this to NULL.
     *
     * @return the number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        int count = 0;
        
        switch(sMatcher.match(uri))
        {
            case ITEM_ALL:
                //Todo...
                break;
            case ITEM_ID:
                String id = uri.getPathSegments().get(1);
                //Todo...
                break;
            default:
                throw new IllegalArgumentException("Unknown URI"+uri);
        }
        
        if(count > 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * Returns the mimeType associated with the Uri (query).
     * @see android.content.ContentProvider#getType(Uri)
     * @param uri the content URI to be checked
     * @return the corresponding MIMEtype
     */
    @Override
    public String getType(Uri uri)
    {
        return sMimeTypes.get(sMatcher.match(uri));
    }

    /**
     *
     * Insert a single row into a table
     * @see android.content.ContentProvider#insert(Uri, ContentValues)
     * @param uri the content URI of the table
     * @param values a {@link android.content.ContentValues} object containing the row to insert
     * @return the content URI of the new row
     */
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        long rowId = -1;

        if(sMatcher.match(uri) != ITEM_ALL)
        {
            throw new IllegalArgumentException("Unknown URI" + uri);
        }
        
        //Todo: call the insert method, and the rowId will be returned by the insert method...
        if(rowId > 0)
        {
            Uri noteUri = ContentUris.withAppendedId(DsTuningProviderContent.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        else
        {
            throw new IllegalArgumentException("Unknown URI" + uri);
        }
    }

    /**
     * Updates one or more rows in a table.
     * @see android.content.ContentProvider#update(Uri, ContentValues, String, String[])
     * @param uri The content URI for the table
     * @param values The values to use to update the row or rows. You only need to specify column
     * names for the columns you want to change. To clear the contents of a column, specify the
     * column name and NULL for its value.
     * @param selection An SQL WHERE clause (without the WHERE keyword) specifying the rows to
     * update. Use "?" to mark places that should be substituted by values in selectionArgs.
     * @param selectionArgs An array of values that are mapped in order to each "?" in selection.
     * If no "?" are used, set this to NULL.
     *
     * @return int The number of rows updated.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        int count = 0;

        switch(sMatcher.match(uri))
        {
            case ITEM_ALL:
                //Todo: call the update method...
                break;
            case ITEM_ID:
                String id = uri.getPathSegments().get(1);
                //Todo: call the update method...
                break;
            default:
                Log.d(TAG, "Unknown URI"+uri);
                throw new IllegalArgumentException("Unknown URI"+uri);
        }

        if(count > 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * Returns the result of querying the chosen table.
     * @see android.content.ContentProvider#query(Uri, String[], String, String[], String)
     * @param uri The content URI of the table
     * @param projection The names of the columns to return in the cursor
     * @param selection The selection clause for the query
     * @param selectionArgs An array of Strings containing search criteria
     * @param sortOrder A clause defining the order in which the retrieved rows should be sorted
     * @return The query results, as a {@link android.database.Cursor} of rows and columns
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder)
    {
        Cursor c = null;

        switch (sMatcher.match(uri))
        {
            case ITEM_ALL:
                //Todo: call the query method...
                Log.d(TAG, "query uri="+uri);
                break;
            case ITEM_ID:
                String id = uri.getPathSegments().get(1);
                //Todo: call the query method...
                Log.d(TAG,"query ITEM_ID id="+id+", uri="+uri);
                break;
            default:
                Log.d(TAG, "Unknown URI" + uri);
                throw new IllegalArgumentException("Unknown URI" + uri);
        }
        if(c != null)
        {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        else
        {
            Log.e(TAG, "DsTuningProvider.query: failed");
        }
        return c;
    }
}

