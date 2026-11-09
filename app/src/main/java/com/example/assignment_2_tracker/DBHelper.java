package com.example.assignment_2_tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "stepbystep.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "activities";
    private static final String COL_ID = "id";
    private static final String COL_START = "start_time";
    private static final String COL_DURATION = "duration_sec";
    private static final String COL_STEPS = "steps";
    private static final String COL_SHAKES = "shakes";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_START + " INTEGER NOT NULL, "
                + COL_DURATION + " INTEGER NOT NULL, "
                + COL_STEPS + " INTEGER NOT NULL, "
                + COL_SHAKES + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void  insertActivity(long startTime, int steps, int shakes, int durationSec) {
        ContentValues values = new ContentValues();
        values.put(COL_START, startTime);
        values.put(COL_DURATION, durationSec);
        values.put(COL_STEPS, steps);
        values.put(COL_SHAKES, shakes);

        getWritableDatabase().insert(TABLE, null, values);
    }

    public List<ActivityItem> getAllActivities() {
        List<ActivityItem> list = new ArrayList<>();

        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null,
                COL_START + " ASC, " + COL_ID + " ASC")) {
            double maxStepsPerMin = 0;
            double maxShakesPerMin = 0;
            while (c.moveToNext()) {
                ActivityItem item = new ActivityItem(
                        c.getInt(c.getColumnIndexOrThrow(COL_ID)),
                        c.getLong(c.getColumnIndexOrThrow(COL_START)),
                        c.getInt(c.getColumnIndexOrThrow(COL_DURATION)),
                        c.getInt(c.getColumnIndexOrThrow(COL_STEPS)),
                        c.getInt(c.getColumnIndexOrThrow(COL_SHAKES))
                );

                double sp = item.getStepsPerMin();
                double kp = item.getShakesPerMin();

                item.setStepRecord(sp > 0 && sp > maxStepsPerMin);
                item.setShakeRecord(kp > 0 && kp > maxShakesPerMin);

                maxStepsPerMin = Math.max(maxStepsPerMin, sp);
                maxShakesPerMin = Math.max(maxShakesPerMin, kp);
                list.add(item);
            }
        }
        Collections.reverse(list);   // ใหม่สุดอยู่บนสุด
        return list;
    }

    public int getTotalSteps() {
        return queryInt("SELECT SUM(" + COL_STEPS + ") FROM " + TABLE);
    }

    public int getTotalShakes() {
        return queryInt("SELECT SUM(" + COL_SHAKES + ") FROM " + TABLE);
    }

    private int queryInt(String sql) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }


    public int deleteActivity(int id) {
        return getWritableDatabase().delete(TABLE, COL_ID + " = ?",  new String[]{String.valueOf(id)});
    }
}