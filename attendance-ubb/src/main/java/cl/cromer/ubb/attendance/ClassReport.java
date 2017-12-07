package cl.cromer.ubb.attendance;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.DBSchema.*;

public class ClassReport extends AppCompatActivity {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    private AlertDialog choiceDialog = null;
    private AlertDialog confirmDialog = null;
    private View reportView;

    private List<Subject> subjects = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();
    private List<Class> classes = new ArrayList<>();
    private List<Attendance> students = new ArrayList<>();

    private Class classObject = new Class();

    private int disabledButtonColor;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_report);

        this.context = this;

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        reportView = inflater.inflate(R.layout.view_class_report, new RelativeLayout(this), false);

        // Build the add subject dialog window using the subject view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(reportView);
        builder.setPositiveButton(R.string.input_accept, null);
        builder.setNegativeButton(R.string.input_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        choiceDialog = builder.create();

        choiceDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                ColorStateList colorStateList = choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTextColors();
                choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
                choiceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));

                if (colorStateList != null) {
                    disabledButtonColor = colorStateList.getColorForState(new int[] {-android.R.attr.state_enabled}, R.color.colorPrimary);
                }
            }
        });

        choiceDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        choiceDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        choiceDialog.setCanceledOnTouchOutside(false);

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
        // We need to get rid of the progressbar if it's showing
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        if (choiceDialog != null && choiceDialog.isShowing()) {
            choiceDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void databaseLoaded() {
        final GetSubjects getSubjects = new GetSubjects(getApplicationContext());
        getSubjects.execute();

        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                getSubjects.cancel(true);
                finish();
            }
        });
    }

    private AdapterView.OnItemSelectedListener subjectListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                courses = new ArrayList<>();
                classes = new ArrayList<>();
                if (position == 0) {
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(disabledButtonColor);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);

                    Spinner spinner = (Spinner) reportView.findViewById(R.id.course_spinner);

                    List<String> options = new ArrayList<>();
                    options.add(getResources().getString(R.string.report_course));

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
                    arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

                    spinner.setAdapter(arrayAdapter);
                    spinner.setSelection(0);
                    spinner.setEnabled(false);
                    spinner.setClickable(false);
                    spinner.setOnItemSelectedListener(null);

                    spinner = (Spinner) reportView.findViewById(R.id.class_spinner);

                    options = new ArrayList<>();
                    options.add(getResources().getString(R.string.report_class));

                    arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
                    arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

                    spinner.setAdapter(arrayAdapter);
                    spinner.setSelection(0);
                    spinner.setEnabled(false);
                    spinner.setClickable(false);
                    spinner.setOnItemSelectedListener(null);
                }
                else {
                    progress = new Progress();
                    progress.show(context, 1);
                    progress.setCancelable(false);

                    final int thePosition = position - 1;

                    classObject.setSubject(subjects.get(thePosition));

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {

                                    final GetCourses getCourses = new GetCourses(getApplicationContext());
                                    getCourses.execute(thePosition);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            getCourses.cancel(true);
                                            finish();
                                        }
                                    });
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    private AdapterView.OnItemSelectedListener courseListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                classes = new ArrayList<>();
                if (position == 0) {
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(disabledButtonColor);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);

                    Spinner spinner = (Spinner) reportView.findViewById(R.id.class_spinner);

                    List<String> options = new ArrayList<>();
                    options.add(getResources().getString(R.string.report_class));

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
                    arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

                    spinner.setAdapter(arrayAdapter);
                    spinner.setSelection(0);
                    spinner.setEnabled(false);
                    spinner.setClickable(false);
                    spinner.setOnItemSelectedListener(null);
                }
                else {
                    progress = new Progress();
                    progress.show(context, 1);
                    progress.setCancelable(false);

                    final int thePosition = position - 1;

                    classObject.setCourse(courses.get(thePosition));

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {

                                    final GetClasses getClasses = new GetClasses(getApplicationContext());
                                    getClasses.execute(thePosition);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            getClasses.cancel(true);
                                            finish();
                                        }
                                    });
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    private AdapterView.OnItemSelectedListener classListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(disabledButtonColor);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(false);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null);
                }
                else {
                    classObject.setClass(classes.get(position - 1));

                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setClickable(true);
                    choiceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(acceptListener());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    private View.OnClickListener acceptListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress = new Progress();
                progress.show(context, 1);
                progress.setCancelable(false);

                choiceDialog.dismiss();

                sqlParser = new SQLParser(getApplicationContext());
                thread = new Thread(new Runnable() {
                    public void run() {
                        ubbDB = sqlParser.getWritableDatabase();
                        threadHandler.post(new Runnable() {
                            public void run() {

                                final GetAttendance getAttendance = new GetAttendance(getApplicationContext());
                                getAttendance.execute();

                                progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        getAttendance.cancel(true);
                                        finish();
                                    }
                                });
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
        };
    }

    private class GetSubjects extends AsyncTask<Void, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected GetSubjects(Context context) {
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
        protected Void doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }

            // Get subjects by name in ascending order
            String query = "SELECT " + sqlParser.combineColumnsStrings(DBSubjects.ALL_COLUMNS, DBMajors.ALL_COLUMNS) + " FROM " + DBSubjects.TABLE_NAME + " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBMajors.ALL_COLUMNS) +" FROM " + DBMajors.TABLE_NAME + ") " + DBMajors.TABLE_NAME + " ON " + DBSubjects.TABLE_NAME + "." + DBSubjects.COLUMN_MAJOR + "=" + DBMajors.TABLE_NAME + "." + DBMajors.COLUMN_ID + " ORDER BY " + DBSubjects.COLUMN_NAME + " ASC";
            if (BuildConfig.DEBUG) {
                Log.d("ClassReport", query);
            }
            Cursor cursor = ubbDB.rawQuery(query, null);

            // Iterate through the database rows
            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    return null;
                }
                Major major = new Major(
                    cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(DBMajors.COLUMN_NAME)),
                    cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_CODE)));
                subjects.add(
                    new Subject(
                        major,
                        cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DBSubjects.COLUMN_NAME)),
                        cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_CODE))
                    )
                );
            }
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Release the kraken errr wakelock
            List<String> options = new ArrayList<>();
            options.add(getResources().getString(R.string.report_subject));
            for (Subject subject : subjects) {
                options.add(subject.getSubjectName());
            }

            Spinner spinner = (Spinner) reportView.findViewById(R.id.subject_spinner);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
            arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

            spinner.setAdapter(arrayAdapter);
            spinner.setSelection(0);

            spinner.setOnItemSelectedListener(subjectListener());
            progress.dismiss();
            wakeLock.release();
            choiceDialog.show();
        }
    }

    private class GetCourses extends AsyncTask<Integer, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected GetCourses(Context context) {
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
        protected Void doInBackground(Integer... position) {
            if (isCancelled()) {
                return null;
            }

            Cursor cursor = ubbDB.query(
                    DBCourses.TABLE_NAME,
                    DBCourses.ALL_COLUMNS,
                    DBCourses.COLUMN_SUBJECT + "=" + subjects.get(position[0]).getSubjectId(),
                    null,
                    null,
                    null,
                    DBCourses.COLUMN_YEAR + " DESC, " + DBCourses.COLUMN_SEMESTER + " DESC, " + DBCourses.COLUMN_SECTION + " ASC",
                    null);

            // Iterate through the database rows
            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    return null;
                }
                courses.add(
                    new Course(
                        subjects.get(position[0]),
                        cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_SECTION)),
                        cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_SEMESTER)),
                        cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_YEAR))
                    )
                );
            }
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Release the kraken errr wakelock
            List<String> options = new ArrayList<>();
            options.add(getResources().getString(R.string.report_course));
            for (Course course : courses) {
                options.add(course.getYear() + "-" + course.getCourseSemester() + " " + getResources().getString(R.string.report_section) + " " + course.getCourseSection());
            }

            Spinner spinner = (Spinner) reportView.findViewById(R.id.course_spinner);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
            arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

            spinner.setAdapter(arrayAdapter);
            spinner.setSelection(0);
            spinner.setEnabled(true);
            spinner.setClickable(true);

            spinner.setOnItemSelectedListener(courseListener());
            progress.dismiss();
            wakeLock.release();
        }
    }

    private class GetClasses extends AsyncTask<Integer, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected GetClasses(Context context) {
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
        protected Void doInBackground(Integer... position) {
            if (isCancelled()) {
                return null;
            }

            Cursor cursor = ubbDB.query(
                    DBClasses.TABLE_NAME,
                    DBClasses.ALL_COLUMNS,
                    DBClasses.COLUMN_COURSE + "=" + courses.get(position[0]).getCourseId(),
                    null,
                    null,
                    null,
                    DBClasses.COLUMN_DATE + " DESC",
                    null);

            // Iterate through the database rows
            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    return null;
                }
                classes.add(
                    new Class(
                        courses.get(position[0]),
                        cursor.getInt(cursor.getColumnIndex(DBClasses.COLUMN_ID)),
                        cursor.getLong(cursor.getColumnIndex(DBClasses.COLUMN_DATE))
                    )
                );
            }
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Release the kraken errr wakelock
            List<String> options = new ArrayList<>();
            options.add(getResources().getString(R.string.report_class));
            for (Class classObject : classes) {
                options.add(classObject.getFormattedDate(getApplicationContext()));
            }

            Spinner spinner = (Spinner) reportView.findViewById(R.id.class_spinner);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.view_spinner_selected_item, options);
            arrayAdapter.setDropDownViewResource(R.layout.view_spinner_dropdown_item);

            spinner.setAdapter(arrayAdapter);
            spinner.setSelection(0);
            spinner.setEnabled(true);
            spinner.setClickable(true);

            spinner.setOnItemSelectedListener(classListener());
            progress.dismiss();
            wakeLock.release();
        }
    }

    private class GetAttendance extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected GetAttendance(Context context) {
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
        protected Boolean doInBackground(Void... voids) {
            if (isCancelled()) {
                return false;
            }

            String query = "SELECT " + sqlParser.combineColumnsStrings(DBStudents.ALL_COLUMNS, DBAttendance.ALL_COLUMNS) + " FROM " + DBStudents.TABLE_NAME +
                " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBCoursesStudents.ALL_COLUMNS) + " FROM " + DBCoursesStudents.TABLE_NAME + " WHERE " + DBCoursesStudents.COLUMN_COURSE + "=" + classObject.getCourseId() + ") AS " + DBCoursesStudents.TABLE_NAME + " ON " + DBStudents.TABLE_NAME + "." + DBStudents.COLUMN_ID + "=" + DBCoursesStudents.TABLE_NAME + "." + DBCoursesStudents.COLUMN_STUDENT +
                " LEFT OUTER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBAttendance.ALL_COLUMNS) + " FROM " + DBAttendance.TABLE_NAME + " WHERE " + DBAttendance.COLUMN_CLASS + "=" + classObject.getClassId() + ") AS " + DBAttendance.TABLE_NAME + " ON " + DBStudents.TABLE_NAME + "." + DBStudents.COLUMN_ID + "=" + DBAttendance.TABLE_NAME + "." + DBAttendance.COLUMN_STUDENT +
                " ORDER BY " + DBStudents.COLUMN_FIRST_LAST_NAME + " ASC, " + DBStudents.COLUMN_FIRST_NAME + " ASC";
            if (BuildConfig.DEBUG) {
                Log.d("ClassReport", query);
            }
            Cursor cursor = ubbDB.rawQuery(query, null);

            // Iterate through the database rows
            while (cursor.moveToNext()) {
                if (isCancelled()) {
                    return null;
                }
                students.add(new Attendance(
                    cursor.getInt(cursor.getColumnIndex(DBAttendance.COLUMN_ATTENDANCE)),
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
            cursor.close();

            Workbook workbook = new HSSFWorkbook();
            Sheet sheet = workbook.createSheet(getResources().getString(R.string.app_name));

            Excel excel = new Excel(workbook);
            excel.createStyles();

            Row row;
            Cell cell;
            // Major
            row = sheet.createRow(0);
            cell = row.createCell(0);
            cell.setCellValue(classObject.getMajorName() + " - " + classObject.getMajorCode());
            cell.setCellStyle(excel.getCellStyle("left"));
            for (int i = 1; i < 7; i++) {
                cell = row.createCell(i);
                if (i == 6) {
                    cell.setCellStyle(excel.getCellStyle("right"));
                }
                else {
                    cell.setCellStyle(excel.getCellStyle("middle"));
                }
            }

            // Subject
            row = sheet.createRow(1);
            cell = row.createCell(0);
            cell.setCellValue(classObject.getSubjectName() + " - " + classObject.getSubjectCode());
            cell.setCellStyle(excel.getCellStyle("left"));
            for (int i = 1; i < 7; i++) {
                cell = row.createCell(i);
                if (i == 6) {
                    cell.setCellStyle(excel.getCellStyle("right"));
                }
                else {
                    cell.setCellStyle(excel.getCellStyle("middle"));
                }
            }

            // Columns
            row = sheet.createRow(2);

            cell = row.createCell(0);
            cell.setCellValue(getResources().getString(R.string.report_names));
            cell.setCellStyle(excel.getCellStyle("blue"));

            cell = row.createCell(1);
            cell.setCellValue(getResources().getString(R.string.report_first_last_name));
            cell.setCellStyle(excel.getCellStyle("blue"));

            cell = row.createCell(2);
            cell.setCellValue(getResources().getString(R.string.report_second_last_name));
            cell.setCellStyle(excel.getCellStyle("blue"));

            cell = row.createCell(3);
            cell.setCellValue(getResources().getString(R.string.report_present));
            cell.setCellStyle(excel.getCellStyle("green"));

            cell = row.createCell(4);
            cell.setCellValue(getResources().getString(R.string.report_late));
            cell.setCellStyle(excel.getCellStyle("yellow"));

            cell = row.createCell(5);
            cell.setCellValue(getResources().getString(R.string.report_justified));
            cell.setCellStyle(excel.getCellStyle("orange"));

            cell = row.createCell(6);
            cell.setCellValue(getResources().getString(R.string.report_absent));
            cell.setCellStyle(excel.getCellStyle("red"));

            int cellSize1 = getResources().getString(R.string.report_names).length();
            int cellSize2 = getResources().getString(R.string.report_first_last_name).length();
            int cellSize3 = getResources().getString(R.string.report_second_last_name).length();
            int rowNumber = 3;
            String value;
            for (Attendance student : students) {
                row = sheet.createRow(rowNumber);

                // First and second name
                cell = row.createCell(0);
                value = student.getFirstName() + " " + student.getSecondName();
                if (value.length() > cellSize1) {
                    cellSize1 = value.length();
                }
                cell.setCellValue(value);
                cell.setCellStyle(excel.getCellStyle("main"));

                // First last name
                cell = row.createCell(1);
                value = student.getFirstLastName();
                if (value.length() > cellSize2) {
                    cellSize2 = value.length();
                }
                cell.setCellValue(value);
                cell.setCellStyle(excel.getCellStyle("main"));

                // Second last name
                cell = row.createCell(2);
                value = student.getSecondLastName();
                if (value.length() > cellSize3) {
                    cellSize3 = value.length();
                }
                cell.setCellValue(value);
                cell.setCellStyle(excel.getCellStyle("main"));

                int status = student.getStatus();
                int attendanceCell = 3;
                if (status != 0) {
                    switch (status) {
                        case 1:
                            attendanceCell = 3;
                            break;
                        case 4:
                            attendanceCell = 4;
                            break;
                        case 2:
                            attendanceCell = 5;
                            break;
                        case 3:
                            attendanceCell = 6;
                            break;
                    }
                }

                CellStyle attendanceColor = null;
                for (int i = 3; i < 7; i++) {
                    cell = row.createCell(i);
                    switch (i) {
                        case 3:
                            attendanceColor = excel.getCellStyle("green");
                            break;
                        case 4:
                            attendanceColor = excel.getCellStyle("yellow");
                            break;
                        case 5:
                            attendanceColor = excel.getCellStyle("orange");
                            break;
                        case 6:
                            attendanceColor = excel.getCellStyle("red");
                            break;
                    }
                    if (i == attendanceCell) {
                        cell.setCellValue("X");
                    }
                    cell.setCellStyle(attendanceColor);
                }

                rowNumber++;
            }
            sheet.setColumnWidth(0, (cellSize1 + 1) * 256);
            sheet.setColumnWidth(1, (cellSize2 + 1) * 256);
            sheet.setColumnWidth(2, (cellSize3 + 1) * 256);

            final String filePath = StringFixer.removeInvalidFileCharacters(Environment.getExternalStorageDirectory() +
                "/" + getResources().getString(R.string.app_name) +
                "/" + classObject.getMajorName() +
                "/" + classObject.getYear() + "-" + classObject.getCourseSemester() + "-" + classObject.getCourseSection());

            final String fileName = "/" + getResources().getString(R.string.report_class) + "-" + classObject.getFormattedShortDate(context).replaceAll("/", "-") + ".xls";

            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        return false;
                    }
                }
                FileOutputStream fileOutputStream = new FileOutputStream(filePath + fileName);
                workbook.write(fileOutputStream);
                fileOutputStream.close();
            }
            catch (FileNotFoundException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
                return false;
            }
            catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
                return false;
            }

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Release the kraken errr wakelock
            if (result) {
                confirmOpen();
            }
            else {
                Toast.makeText(getApplicationContext(), R.string.report_fail, Toast.LENGTH_SHORT).show();
                finish();
            }
            progress.dismiss();
            wakeLock.release();
        }
    }

    private void confirmOpen() {
        final String filePath = StringFixer.removeInvalidFileCharacters(Environment.getExternalStorageDirectory() +
            "/" + getResources().getString(R.string.app_name) +
            "/" + classObject.getMajorName() +
            "/" + classObject.getYear() + "-" + classObject.getCourseSemester() + "-" + classObject.getCourseSection());

        final String fileName = "/" + getResources().getString(R.string.report_class) + "-" + classObject.getFormattedShortDate(context).replaceAll("/", "-") + ".xls";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.report_created);
        builder.setPositiveButton(R.string.input_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file  = new File(filePath + fileName);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-excel");

                try {
                    startActivity(intent);
                }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(ClassReport.this, "No Application Available to View Excel", Toast.LENGTH_SHORT).show();
                }
                confirmDialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton(R.string.input_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmDialog.dismiss();
                finish();
            }
        });

        builder.setCancelable(true);
        confirmDialog = builder.create();

        confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            }
        });

        confirmDialog.show();
    }
}
