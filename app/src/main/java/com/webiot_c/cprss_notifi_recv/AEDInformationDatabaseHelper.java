package com.webiot_c.cprss_notifi_recv;

import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class AEDInformationDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;

    private static final String DATABASE_NAME = "AEDInfos.db";
    private static final String TABLE_NAME = "aedinfos";
    private static final String COLUMN_NAME_ID = "_id";
    private static final String COLUMN_NAME_ADEID = "aedinfo";
    private static final String COLUMN_NAME_LATITUDE = "lat";
    private static final String COLUMN_NAME_LONGITUDE = "lon";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_ADEID + " TEXT," +
                    COLUMN_NAME_LATITUDE + " REAL, " +
                    COLUMN_NAME_LONGITUDE + " REAL) ";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    AEDInformationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(
                SQL_DELETE_ENTRIES
        );
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void truncate(){
        getWritableDatabase().delete(TABLE_NAME, null, null);
    }


    public AEDInformation[] getAEDInformationsFromDatabase(){

        Cursor cursor = getReadableDatabase().query(
                TABLE_NAME,
                new String[] {COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE},
                null, null, null, null, null
        );

        cursor.moveToFirst();

        ArrayList<AEDInformation> aeds = new ArrayList<>();

        for(int i = 0; i < cursor.getCount(); i++){
            aeds.add(new AEDInformation(
                    cursor.getString(0),
                    cursor.getDouble(1),
                    cursor.getDouble(2)
            ));
            cursor.moveToNext();
        }

        cursor.close();

        return aeds.toArray(new AEDInformation[aeds.size()]);
    }

    public void saveData(AEDInformation aedInfo){

        String query = String.format("INSERT INTO %s ( %s, %s, %s) VALUES ('%s', '%s', '%s')",
                TABLE_NAME,
                COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE,
                aedInfo.getAed_id(), aedInfo.getLatitude(), aedInfo.getLongitude());

        getWritableDatabase().execSQL(query);

    }

    public void deleteData(String aedid){

        String query = String.format("DELETE FROM %s WHERE %s = '%s'",
                TABLE_NAME,
                COLUMN_NAME_ADEID, aedid);

        getWritableDatabase().execSQL(query);

    }
}
