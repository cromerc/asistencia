package cl.cromer.ubb.attendance;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.RUT;
import cl.cromer.ubb.attendance.DBSchema.DBStudents;

public class ImportPhotos extends AppCompatActivity {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    // Background async
    private ReadFile readFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_photos);

        // Receive the info about the photo zip
        Intent subjectListIntent = getIntent();
        final int type = subjectListIntent.getIntExtra(StaticVariables.IMPORT_PHOTO_ZIP_FILE_TYPE, 1);
        final String path = subjectListIntent.getStringExtra(StaticVariables.IMPORT_PHOTO_ZIP_FILE);

        // Create a progress dialog for slow devices
        progress = new Progress();
        progress.show(this, 1);
        progress.setCancelable(false);

        // Load the SQLite database
        sqlParser = new SQLParser(this);
        thread = new Thread(new Runnable() {
            public void run() {
                ubbDB = sqlParser.getWritableDatabase();
                threadHandler.post(new Runnable() {
                    public void run() {
                        databaseLoaded(type, path);
                    }
                });
            }
        });
        thread.start();
        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                thread.interrupt();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We need to get rid of the progressbar if it's showing
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        // Kill the other background thread before we start the new thread
        if (readFile != null) {
            readFile.cancel(true);
        }
    }

    private void databaseLoaded(int type, String path) {
        // The database has finished loading, so read the file
        readFile = new ReadFile(getApplicationContext(), type, path);
        readFile.execute();

        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                readFile.cancel(true);
                finish();
            }
        });
    }

    private class ReadFile extends AsyncTask<Void, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        private int type;
        private String path;

        protected ReadFile(Context context, int type, String path) {
            this.context = context;
            this.type = type;
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Let's make sure the CPU doesn't go to sleep
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }

            InputStream inputStream = null;
            if (type == 0) {
                ContentResolver contentResolver = getContentResolver();
                try {
                    inputStream = contentResolver.openInputStream(Uri.parse(path));
                }
                catch (FileNotFoundException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                        Log.d("ImportPhotos", e.getMessage());
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            //File directory = getExternalFilesDir(null);
            File directory = getFilesDir();

            ZipFile zipFile = new ZipFile();
            if (inputStream != null) {
                try {
                    if (!zipFile.unzip(inputStream, directory)) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                            Log.wtf("ImportPhotos", "Something strange happened in ZipFile.");
                        }
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                    inputStream.close();
                }
                catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                        Log.d("ImportPhotos", e.getMessage());
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
            else {
                try {
                    if (!zipFile.unzip(new FileInputStream(path), directory)) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                            Log.wtf("ImportPhotos", "Something strange happened in ZipFile.");
                        }
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
                catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                        Log.d("ImportPhotos", e.getMessage());
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            // Update all the name columns
            normalizeNameColumns(false);

            for (File file : directory.listFiles()) {
                String[] temp = file.toString().split("/");
                String fileName = temp[temp.length - 1];

                String run = null;
                if (RUT.isValidRut(RUT.cleanRut(fileName.substring(0, fileName.length() - 4)))) {
                    run = RUT.cleanRut(fileName.substring(0, fileName.length() - 4));
                }

                if (run == null) {
                    // Search by name
                    String nameTemp = StringFixer.normalizer(fileName.substring(0, fileName.length() - 4));
                    String[] names = nameTemp.split("(?=\\p{Lu})");

                    String where;
                    if (names.length < 3) {
                        if (BuildConfig.DEBUG) {
                            Log.wtf("ImportPhotos", "The file name is wrong");
                        }
                        continue;
                    }
                    else if (names.length == 3) {
                        // First name, last name
                        where = DBStudents.COLUMN_FIRST_NAME_NORM + " LIKE \"" + names[1] + "\" AND " + DBStudents.COLUMN_FIRST_LAST_NAME_NORM + " LIKE \"" + names[2] + "\"";
                    }
                    else if (names.length == 4) {
                        // First name, last names
                        where = DBStudents.COLUMN_FIRST_NAME_NORM + " LIKE \"" + names[1] + "\" AND " + DBStudents.COLUMN_FIRST_LAST_NAME_NORM + " LIKE \"" + names[2] + "\" AND " + DBStudents.COLUMN_SECOND_LAST_NAME_NORM + " LIKE \"" + names[3] + "\"";
                    }
                    else {
                        if (BuildConfig.DEBUG) {
                            Log.wtf("ImportPhotos", "The file name is wrong");
                        }
                        continue;
                    }

                    Cursor cursor = ubbDB.query(
                        DBStudents.TABLE_NAME,
                        new String[] {DBStudents.COLUMN_ID},
                        where,
                        null,
                        null,
                        null,
                        null,
                        "1");

                    if (cursor.getCount() > 0) {
                        // Found a student, let's load his image
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        if (activityManager.getMemoryClass() <= 32) {
                            options.inSampleSize = 2; // Shrink quality
                        }
                        else {
                            options.inSampleSize = 1; // Full quality
                        }
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        FileInputStream fileInputStream = null;
                        try {
                            fileInputStream = new FileInputStream(file);
                        }
                        catch (FileNotFoundException e) {
                            if (BuildConfig.DEBUG) {
                                e.printStackTrace();
                                Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                                Log.d("ImportPhotos", e.getMessage());
                            }
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, options);
                        byte[] imgByte = bitmapToByte(bitmap);
                        bitmap.recycle();

                        ContentValues values = new ContentValues();
                        values.put(DBStudents.COLUMN_PHOTO, imgByte);
                        ubbDB.update(DBStudents.TABLE_NAME, values, where, null);
                    }
                    cursor.close();
                }
                else {
                    // Search by run
                    Cursor cursor = ubbDB.query(
                        DBStudents.TABLE_NAME,
                        new String[] {DBStudents.COLUMN_ID},
                        DBStudents.COLUMN_RUN + "=\"" + run + "\"",
                        null,
                        null,
                        null,
                        null,
                        "1");

                    if (cursor.getCount() > 0) {
                        // Found a student, let's load his image
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        if (activityManager.getMemoryClass() <= 32) {
                            options.inSampleSize = 2; // Shrink quality
                        }
                        else {
                            options.inSampleSize = 1; // Full quality
                        }
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        FileInputStream fileInputStream = null;
                        try {
                            fileInputStream = new FileInputStream(file);
                        }
                        catch (FileNotFoundException e) {
                            if (BuildConfig.DEBUG) {
                                e.printStackTrace();
                                Toast.makeText(context, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                                Log.d("ImportPhotos", e.getMessage());
                            }
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, options);
                        byte[] imgByte = bitmapToByte(bitmap);
                        bitmap.recycle();

                        ContentValues values = new ContentValues();
                        values.put(DBStudents.COLUMN_PHOTO, imgByte);
                        ubbDB.update(DBStudents.TABLE_NAME, values, DBStudents.COLUMN_RUN + "=" + run, null);
                    }
                    cursor.close();
                }

                if (file.exists()) {
                    if (!file.delete()) {
                        if (BuildConfig.DEBUG) {
                            Log.wtf("ImportPhotos", "The file couldn't be deleted?");
                        }
                    }
                }
            }

            if (!directory.delete()) {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportPhotos", "The directory could not be deleted?");
                }
            }

            ubbDB.close();
            sqlParser.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            // Release the kraken errr wakelock
            progress.dismiss();
            wakeLock.release();
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private byte[] bitmapToByte(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    // This function is necessary because SQLite in android can't ignore accented characters...
    private void normalizeNameColumns(boolean redo) {
        String redoString = null;
        if (!redo) {
            redoString = DBStudents.COLUMN_FIRST_NAME_NORM + " IS NULL OR " + DBStudents.COLUMN_FIRST_NAME_NORM + "=\"\"";
        }

        Cursor cursor = ubbDB.query(
            DBStudents.TABLE_NAME,
            DBStudents.ALL_COLUMNS,
            redoString,
            null,
            null,
            null,
            null,
            null);

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(DBStudents.COLUMN_ID));
                String firstName = StringFixer.normalizer(cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_FIRST_NAME)));
                String secondName = StringFixer.normalizer(cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_SECOND_NAME)));
                String firstLastName = StringFixer.normalizer(cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_FIRST_LAST_NAME)));
                String secondLastName = StringFixer.normalizer(cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_SECOND_LAST_NAME)));

                ContentValues values = new ContentValues();
                values.put(DBStudents.COLUMN_FIRST_NAME_NORM, firstName);
                values.put(DBStudents.COLUMN_SECOND_NAME_NORM, secondName);
                values.put(DBStudents.COLUMN_FIRST_LAST_NAME_NORM, firstLastName);
                values.put(DBStudents.COLUMN_SECOND_LAST_NAME_NORM, secondLastName);
                ubbDB.update(DBStudents.TABLE_NAME, values, DBStudents.COLUMN_ID + "=" + id, null);
            }
        }
        cursor.close();
    }
}
