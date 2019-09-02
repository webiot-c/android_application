package com.webiot_c.cprss_notifi_recv.data_struct;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;

public class AEDInformationDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;

    private static final String DATABASE_NAME = "AEDInfos.db";
    private static final String TABLE_NAME = "aedinfos_ver2";

    private static final String COLUMN_NAME_ID = "_id";
    private static final String COLUMN_NAME_ADEID = "aedinfo";
    private static final String COLUMN_NAME_LATITUDE = "lat";
    private static final String COLUMN_NAME_LONGITUDE = "lon";
    private static final String COLUMN_NAME_RECEIVED_TIME = "time";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_ADEID + " TEXT," +
                    COLUMN_NAME_LATITUDE + " REAL, " +
                    COLUMN_NAME_LONGITUDE + " REAL, " +
                    COLUMN_NAME_RECEIVED_TIME + " INTEGER) ";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    private static AEDInformationDatabaseHelper aedInformationDatabaseHelper;

    private AEDInformationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    public static AEDInformationDatabaseHelper getInstance(Context context){
        if(aedInformationDatabaseHelper == null){
            if(context == null){
                throw new IllegalStateException("一番最初に作るときにはcontextが必要です!!!!!111");
            }
            aedInformationDatabaseHelper = new AEDInformationDatabaseHelper(context);
        }
        return aedInformationDatabaseHelper;
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

    public AEDInformation getAEDInformation(String aed_id){

        Cursor cursor = getReadableDatabase().query(
                TABLE_NAME,
                new String[] {COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE, COLUMN_NAME_RECEIVED_TIME},
                COLUMN_NAME_ADEID + "=?", new String[]{ aed_id },
                null, null, null
        );

        AEDInformation aed_info;

        cursor.moveToFirst();

        if(cursor.getCount() == 0) return null;

        aed_info = new AEDInformation(
                cursor.getString(0),
                cursor.getDouble(1),
                cursor.getDouble(2),
                new Date(cursor.getLong(3))
        );

        return aed_info;

    }

    public AEDInformation[] getAEDInformationsFromDatabase(){

        Cursor cursor = getReadableDatabase().query(
                TABLE_NAME,
                new String[] {COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE, COLUMN_NAME_RECEIVED_TIME},
                null ,null,
                null, null, null
        );

        cursor.moveToFirst();

        ArrayList<AEDInformation> aeds = new ArrayList<>();

        for(int i = 0; i < cursor.getCount(); i++){
            aeds.add(new AEDInformation(
                    cursor.getString(0),
                    cursor.getDouble(1),
                    cursor.getDouble(2),
                    new Date(cursor.getLong(3))
            ));
            cursor.moveToNext();
        }

        cursor.close();

        return aeds.toArray(new AEDInformation[aeds.size()]);
    }

    public boolean isAlreadyRegistred(String aedid){

        Cursor cursor = getReadableDatabase().query(
                TABLE_NAME,
                new String[] {COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE},
                COLUMN_NAME_ADEID + "=?", new String[]{aedid}, null, null, null
        );

        int dataCount = cursor.getCount();
        cursor.close();

        return dataCount >= 1;
    }

    public void saveData(AEDInformation aedInfo){

        String query = String.format("INSERT INTO %s ( %s, %s, %s, %s) VALUES ('%s', '%f', '%f', '%d')",
                TABLE_NAME,
                COLUMN_NAME_ADEID, COLUMN_NAME_LATITUDE, COLUMN_NAME_LONGITUDE, COLUMN_NAME_RECEIVED_TIME,
                aedInfo.getAed_id(), aedInfo.getLatitude(), aedInfo.getLongitude(), aedInfo.getReceivedDate().getTime());

        getWritableDatabase().execSQL(query);

    }

    /**
     * 受信日付は更新されないので注意
     * @param aedInfo
     */
    public void updateData(AEDInformation aedInfo){
        if(!isAlreadyRegistred(aedInfo.getAed_id()))
            throw new IllegalArgumentException("登録されていないAEDInformationをアップデートしようとしました。");

        String query = String.format("UPDATE %s SET %s=%f, %S=%f WHERE %s=\"%s\"",
                TABLE_NAME,
                COLUMN_NAME_LATITUDE, aedInfo.getLatitude(),
                COLUMN_NAME_LONGITUDE, aedInfo.getLongitude(),
                COLUMN_NAME_ADEID, aedInfo.getAed_id(),
                COLUMN_NAME_RECEIVED_TIME);

        getWritableDatabase().execSQL(query);
    }

    public void deleteData(String aedid){

        String query = String.format("DELETE FROM %s WHERE %s = '%s'",
                TABLE_NAME,
                COLUMN_NAME_ADEID, aedid);

        getWritableDatabase().execSQL(query);

    }
}
