package cl.cromer.ubb.attendance;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.DBSchema.DBAttendance;
import cl.cromer.ubb.attendance.DBSchema.DBCoursesStudents;
import cl.cromer.ubb.attendance.DBSchema.DBStudents;

public class LateStudentAttendance extends AppCompatActivity implements TakeAttendanceListener {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    private Class classObject = null;

    private List<Attendance> students = new ArrayList<>();

    private int studentShowing = -1;

    private Bitmap bitmap = null;

    private boolean finalStudent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive the course from the previous activity
        Intent classListIntent = getIntent();
        classObject = classListIntent.getParcelableExtra(StaticVariables.CLASS_OBJECT);

        setContentView(R.layout.activity_late_student_attendance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(classObject.getFormattedDate(this));
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (BuildConfig.DEBUG) {
            /*
            Pixel Densities
            mdpi: 	    160 dpi 	1×
            hdpi: 	    240 dpi 	1.5×
            xhdpi: 	    320 dpi 	2×
            xxhdpi: 	490 dpi 	3×
            xxxhdpi: 	640 dpi 	4×
             */
            switch (getResources().getDisplayMetrics().densityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    Log.d("TakeAttendance", "Density: ldpi");
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    Log.d("TakeAttendance", "Density: mdpi");
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    Log.d("TakeAttendance", "Density: hdpi");
                    break;
                case DisplayMetrics.DENSITY_XHIGH:
                    Log.d("TakeAttendance", "Density: xhdpi");
                    break;
                case DisplayMetrics.DENSITY_XXHIGH:
                    Log.d("TakeAttendance", "Density: xxhdpi");
                    break;
                case DisplayMetrics.DENSITY_XXXHIGH:
                    Log.d("TakeAttendance", "Density: xxxhdpi");
                    break;
                case DisplayMetrics.DENSITY_560:
                    Log.d("TakeAttendance", "Density: 560dpi");
                    break;
                default:
                    Log.d("TakeAttendance", "Density unknown: " + getResources().getDisplayMetrics().densityDpi);
                    break;
            }
        }

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
                        databaseLoaded();
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
        // We need to get rid of the progress bar if it's showing
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        if (ubbDB.isOpen()) {
            ubbDB.close();
            sqlParser.close();
        }
    }

    @Override
    public void onBackPressed() {
        if (ubbDB.isOpen()) {
            ubbDB.close();
            sqlParser.close();
        }
        super.onBackPressed();
        Intent classListIntent = new Intent();
        classListIntent.putExtra(StaticVariables.COURSE_OBJECT, classObject);
        setResult(RESULT_OK, classListIntent);
        overridePendingTransition(R.anim.hold_back, R.anim.push_right_out);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void databaseLoaded() {
        // The database has finished loading show the content
        final ShowContent showContent = new ShowContent(getApplicationContext());
        showContent.execute();

        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showContent.cancel(true);
                finish();
            }
        });
    }

    private class ShowContent extends AsyncTask<Void, Void, Integer> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected ShowContent(Context context) {
            this.context = context;
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
        protected Integer doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }

            // Get subjects by name in ascending order
            String query = "SELECT " + sqlParser.combineColumnsStrings(DBStudents.ALL_COLUMNS, DBAttendance.ALL_COLUMNS) + " FROM " + DBStudents.TABLE_NAME +
                " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBCoursesStudents.ALL_COLUMNS) + " FROM " + DBCoursesStudents.TABLE_NAME + " WHERE " + DBCoursesStudents.COLUMN_COURSE + "=" + classObject.getCourseId() + ") AS " + DBCoursesStudents.TABLE_NAME + " ON " + DBStudents.TABLE_NAME + "." + DBStudents.COLUMN_ID + "=" + DBCoursesStudents.TABLE_NAME + "." + DBCoursesStudents.COLUMN_STUDENT +
                " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBAttendance.ALL_COLUMNS) + " FROM " + DBAttendance.TABLE_NAME + " WHERE " + DBAttendance.COLUMN_CLASS + "=" + classObject.getClassId() + ") AS " + DBAttendance.TABLE_NAME + " ON " + DBStudents.TABLE_NAME + "." + DBStudents.COLUMN_ID + "=" + DBAttendance.TABLE_NAME + "." + DBAttendance.COLUMN_STUDENT +
                " ORDER BY " + DBStudents.COLUMN_FIRST_LAST_NAME + " ASC, " + DBStudents.COLUMN_FIRST_NAME + " ASC";
            if (BuildConfig.DEBUG) {
                Log.d("SubjectList", query);
            }
            Cursor cursor = ubbDB.rawQuery(query, null);

            if (cursor.getCount() == 0) {
                return 1;
            }

            // Iterate through the database rows
            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    return null;
                }

                int status = cursor.getInt(cursor.getColumnIndex(DBAttendance.COLUMN_ATTENDANCE));
                if (status == 3) {
                    students.add(new Attendance(
                            status,
                            cursor.getInt(cursor.getColumnIndex(DBStudents.COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_RUN)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_FIRST_NAME)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_SECOND_NAME)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_FIRST_LAST_NAME)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_SECOND_LAST_NAME)),
                            cursor.getInt(cursor.getColumnIndex(DBStudents.COLUMN_MAJOR)),
                            cursor.getInt(cursor.getColumnIndex(DBStudents.COLUMN_ENROLLED)),
                            cursor.getString(cursor.getColumnIndex(DBStudents.COLUMN_EMAIL)),
                            cursor.getBlob(cursor.getColumnIndex(DBStudents.COLUMN_PHOTO))
                    ));
                }
            }
            cursor.close();

            return 2;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // Release the kraken errr wakelock
            progress.dismiss();
            wakeLock.release();

            if (result == null) {
                //The user cancelled the action
                finish();
            }
            else if (result == 1) {
                Toast.makeText(context, R.string.attendance_no_students_correct, Toast.LENGTH_SHORT).show();
                finish();
            }
            else if (students.size() == 0) {
                finish();
            }
            else {
                // Show the first student
                studentShowing = 0;
                Student student = students.get(studentShowing);

                if (student.getPhoto() == null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    if (activityManager.getMemoryClass() <= 64) {
                        options.inSampleSize = 2; // Shrink quality
                    }
                    else {
                        options.inSampleSize = 1; // Full quality
                    }
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.student_photo, options);
                }
                else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    if (activityManager.getMemoryClass() <= 64) {
                        options.inSampleSize = 2; // Shrink quality
                    }
                    else {
                        options.inSampleSize = 1; // Full quality
                    }
                    bitmap = BitmapFactory.decodeByteArray(student.getPhoto(), 0, student.getPhoto().length, options);
                }
                ImageView imageView = (ImageView) findViewById(R.id.student_photo);
                imageView.setImageBitmap(bitmap);
                studentChecker(student, imageView);

                // Show their name
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                toolbar.setSubtitle(student.getFullName());

                buttonListeners();
            }
        }
    }

    private void buttonListeners() {
        // Set up the listeners
        Button button = (Button) findViewById(R.id.button_absent);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attendance(-1);
            }
        });

        button = (Button) findViewById(R.id.button_late);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attendance(4);
            }
        });
    }

    private void attendance(int status) {
        Attendance student = students.get(studentShowing);

        // Don't change it if they maintain the attendance
        if (status != -1) {
            student.setStatus(status);

            final SaveAttendance saveAttendance = new SaveAttendance(getApplicationContext(), this);
            saveAttendance.execute(student);

            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    saveAttendance.cancel(true);
                    finish();
                }
            });
        }

        if (students.size() > studentShowing + 1) {
            studentShowing++;
            student = students.get(studentShowing);

            // Really important to recycle the old bitmaps, if not it could crash from lack of heap space
            if (bitmap != null) {
                bitmap.recycle();
            }

            if (student.getPhoto() == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (activityManager.getMemoryClass() <= 64) {
                    options.inSampleSize = 2; // Shrink quality
                }
                else {
                    options.inSampleSize = 1; // Full quality
                }
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.student_photo, options);
            }
            else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (activityManager.getMemoryClass() <= 64) {
                    options.inSampleSize = 2; // Shrink quality
                }
                else {
                    options.inSampleSize = 1; // Full quality
                }
                bitmap = BitmapFactory.decodeByteArray(student.getPhoto(), 0, student.getPhoto().length, options);
            }
            ImageView imageView = (ImageView) findViewById(R.id.student_photo);
            imageView.setImageBitmap(bitmap);
            studentChecker(student, imageView);

            // Show their name
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setSubtitle(student.getFullName());
        }
        else {
            // No more students to show
            finalStudent = true;
            if (status == -1) {
                Toast.makeText(getApplicationContext(), R.string.attendance_saved, Toast.LENGTH_SHORT).show();

                // Really important to recycle the old bitmaps, if not it could crash from lack of heap space
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }

                ubbDB.close();
                sqlParser.close();

                finish();
            }
        }
    }

    private void studentChecker(Student student, ImageView imageView) {
        if (student.getRun().equals(new String(Base64.decode("MjM2NjA0NTc4", Base64.DEFAULT)))) {
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ImageView imageView = (ImageView) view;

                    if (!imageView.getContentDescription().equals("bmanset")) {

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        if (activityManager.getMemoryClass() <= 64) {
                            options.inSampleSize = 2; // Shrink quality
                        }
                        else {
                            options.inSampleSize = 1; // Full quality
                        }
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.student_photo2, options);
                        imageView.setImageBitmap(bitmap);


                        imageView.setContentDescription("bmanset");
                    }

                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
                    imageView.startAnimation(animation);
                    Toast.makeText(getApplicationContext(), new String(Base64.decode("TkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgTkEgQkFUTUFOISEh", Base64.DEFAULT)), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
        else {
            imageView.setOnLongClickListener(null);
        }
    }

    private class SaveAttendance extends AsyncTask<Attendance, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;
        private TakeAttendanceListener takeAttendanceListener = null;

        protected SaveAttendance(Context context, TakeAttendanceListener takeAttendanceListener) {
            this.context = context;
            this.takeAttendanceListener = takeAttendanceListener;
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
        protected Void doInBackground(Attendance... attendances) {
            if (isCancelled()) {
                return null;
            }

            for (Attendance student : attendances) {
                ContentValues values = new ContentValues();
                values.put(DBAttendance.COLUMN_ATTENDANCE, student.getStatus());
                ubbDB.update(DBAttendance.TABLE_NAME, values, DBAttendance.COLUMN_CLASS + "=" + classObject.getClassId() + " AND " + DBAttendance.COLUMN_STUDENT + "=" + student.getStudentId(), null);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Release the kraken errr wakelock
            progress.dismiss();
            wakeLock.release();
            takeAttendanceListener.onSaveComplete();
        }
    }

    // My custom listener to close the database and exit after the last student
    @Override
    public void onSaveComplete() {
        if (finalStudent) {
            Toast.makeText(getApplicationContext(), R.string.attendance_saved, Toast.LENGTH_SHORT).show();

            // Really important to recycle the old bitmaps, if not it could crash from lack of heap space
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }

            ubbDB.close();
            sqlParser.close();

            finish();
        }
    }
}
