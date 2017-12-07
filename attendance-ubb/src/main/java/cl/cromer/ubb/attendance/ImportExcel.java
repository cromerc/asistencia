package cl.cromer.ubb.attendance;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.RUT;
import cl.cromer.ubb.attendance.DBSchema.DBSubjects;
import cl.cromer.ubb.attendance.DBSchema.DBMajors;
import cl.cromer.ubb.attendance.DBSchema.DBStudents;
import cl.cromer.ubb.attendance.DBSchema.DBCourses;
import cl.cromer.ubb.attendance.DBSchema.DBCoursesStudents;

public class ImportExcel extends AppCompatActivity {

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
        setContentView(R.layout.activity_import_excel);

        // Receive the info about the excel file
        Intent subjectListIntent = getIntent();
        final int type = subjectListIntent.getIntExtra(StaticVariables.IMPORT_EXCEL_FILE_TYPE, 1);
        final String path = subjectListIntent.getStringExtra(StaticVariables.IMPORT_EXCEL_FILE);

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

    private class ReadFile extends AsyncTask<Void, Void, Subject> {
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
        protected Subject doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }

            Subject subject = new Subject();

            InputStream inputStream = null;
            if (type == 0) {
                ContentResolver contentResolver = getContentResolver();
                try {
                    inputStream = contentResolver.openInputStream(Uri.parse(path));
                }
                catch (FileNotFoundException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                        Log.d("ImportExcel", e.getMessage());
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            if (isCancelled()) {
                return null;
            }

            Workbook workbook = null;

            try {
                POIFSFileSystem fs;
                if (inputStream != null) {
                    fs = new POIFSFileSystem(inputStream);
                    inputStream.close();
                }
                else {
                    fs = new POIFSFileSystem(new FileInputStream(path));
                }
                workbook = new HSSFWorkbook(fs);

            }
            catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                    Log.d("ImportExcel", e.getMessage());
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            Sheet sheet = null;
            if (workbook != null) {
                // Make it so it returns blank when the cell does not exist
                workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                sheet = workbook.getSheetAt(0);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            Pattern pattern;
            Matcher matcher = null;
            Row row = null;
            Cell cell = null;
            String cellContentString = null;
            int cellContentInt = 0;

            // Get the major
            if (sheet != null) {
                row = sheet.getRow(5);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (row != null) {
                cell = row.getCell(7);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (cell != null) {
                cellContentString = cell.getRichStringCellValue().getString();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            // Let's play the matching game
            pattern = Pattern.compile("\\(([0-9]+)\\)\\s(.+)");
            if (cellContentString != null) {
                matcher = pattern.matcher(cellContentString);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            if (matcher != null) {
                if (matcher.find()) {
                    subject.setMajorCode(Integer.valueOf(matcher.group(1)));
                    subject.setMajorName(StringFixer.fixCase(matcher.group(2)));
                }
                else {
                    if (BuildConfig.DEBUG) {
                        // My favorite debug logger!
                        Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            if (isCancelled()) {
                return null;
            }

            // Check for major
            Cursor cursor = ubbDB.query(
                DBMajors.TABLE_NAME,
                new String[] {DBMajors.COLUMN_ID},
                DBMajors.COLUMN_CODE + "=" + subject.getMajorCode(),
                null,
                null,
                null,
                null,
                "1");

            ContentValues values = new ContentValues();
            values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
            values.put(DBMajors.COLUMN_NAME, subject.getMajorName());

            if (cursor.getCount() > 0) {
                // It already exists, so let's update it
                cursor.moveToFirst();
                subject.setMajorId(cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)));
                ubbDB.update(DBMajors.TABLE_NAME, values, DBMajors.COLUMN_CODE + "=" + subject.getMajorCode(), null);
            }
            else {
                // No, it does not exist, let's make it
                subject.setMajorId((int) ubbDB.insert(DBMajors.TABLE_NAME, null, values));
            }
            cursor.close();

            // Get the subject and code
            if (sheet != null) {
                row = sheet.getRow(1);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (row != null) {
                cell = row.getCell(2);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (cell != null) {
                cellContentString = cell.getRichStringCellValue().getString();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            // Match the code and name using regex
            pattern = Pattern.compile("\\(([0-9]+)\\)\\s(.+)");
            if (cellContentString != null) {
                matcher = pattern.matcher(cellContentString);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (matcher != null) {
                if (matcher.find()) {
                    subject.setSubjectCode(Integer.valueOf(matcher.group(1)));
                    subject.setSubjectName(StringFixer.fixCase(matcher.group(2)));
                }
                else {
                    if (BuildConfig.DEBUG) {
                        // My favorite debug logger!
                        Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            // Check if the subject already exists
            cursor = ubbDB.query(
                    DBSubjects.TABLE_NAME,
                    new String[]{DBSubjects.COLUMN_ID},
                    DBSubjects.COLUMN_CODE + "=" + subject.getSubjectCode(),
                    null,
                    null,
                    null,
                    null,
                    "1");

            values = new ContentValues();
            values.put(DBSubjects.COLUMN_CODE, subject.getSubjectCode());
            values.put(DBSubjects.COLUMN_NAME, subject.getSubjectName());
            values.put(DBSubjects.COLUMN_MAJOR, subject.getMajorId());

            if (cursor.getCount() > 0) {
                // It already exists, so let's update it
                cursor.moveToFirst();
                subject.setSubjectId(cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_ID)));
                ubbDB.update(DBSubjects.TABLE_NAME, values, DBSubjects.COLUMN_CODE + "=" + subject.getSubjectCode(), null);
            }
            else {
                // No, it does not exist, let's make it
                subject.setSubjectId((int) ubbDB.insert(DBSubjects.TABLE_NAME, null, values));
            }
            cursor.close();

            if (isCancelled()) {
                return null;
            }

            Course course = new Course(subject);

            // Get the course's year and semester
            if (sheet != null) {
                row = sheet.getRow(2);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (row != null) {
                cell = row.getCell(3);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (cell != null) {
                cellContentString = cell.getRichStringCellValue().getString();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            // Find the values!
            pattern = Pattern.compile("([0-9]+)\\s-\\s([1-2])");
            if (cellContentString != null) {
                matcher = pattern.matcher(cellContentString);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (matcher != null) {
                if (matcher.find()) {
                    course.setCourseYear(Integer.valueOf(matcher.group(1)));
                    course.setCourseSemester(Integer.valueOf(matcher.group(2)));
                }
                else {
                    if (BuildConfig.DEBUG) {
                        // My favorite debug logger!
                        Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            if (isCancelled()) {
                return null;
            }

            // Get the section
            if (sheet != null) {
                row = sheet.getRow(5);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (row != null) {
                cell = row.getCell(5);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (cell != null) {
                cellContentInt = (int) cell.getNumericCellValue();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            course.setCourseSection(cellContentInt);

            if (isCancelled()) {
                return null;
            }

            // Check if the course already exists
            cursor = ubbDB.query(
                DBCourses.TABLE_NAME,
                new String[] {DBCourses.COLUMN_ID},
                DBCourses.COLUMN_SUBJECT + "=" + course.getSubjectId() + " AND " +
                    DBCourses.COLUMN_YEAR + "=" + course.getYear()  + " AND " +
                    DBCourses.COLUMN_SEMESTER + "=" + course.getCourseSemester()  + " AND " +
                    DBCourses.COLUMN_SECTION + "=" + course.getCourseSection(),
                null,
                null,
                null,
                null,
                "1");

            if (cursor.getCount() == 0) {
                // Doesn't exist, so let's make it
                values = new ContentValues();
                values.put(DBCourses.COLUMN_SUBJECT, course.getSubjectId());
                values.put(DBCourses.COLUMN_YEAR, course.getYear());
                values.put(DBCourses.COLUMN_SEMESTER, course.getCourseSemester());
                values.put(DBCourses.COLUMN_SECTION, course.getCourseSection());
                course.setCourseId((int) ubbDB.insert(DBCourses.TABLE_NAME, null, values));
            }
            else {
                cursor.moveToFirst();
                course.setCourseId(cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_ID)));
            }

            cursor.close();

            // Get number of students
            if (sheet != null) {
                row = sheet.getRow(3);
            }
            else {
                if (BuildConfig.DEBUG) {
                    // My favorite debug logger!
                    Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
            if (row != null) {
                cell = row.getCell(2);
            }
            else {
                if (BuildConfig.DEBUG) {
                    // My favorite debug logger!
                    Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
            if (cell != null) {
                cellContentString = cell.getRichStringCellValue().getString();
            }
            else {
                if (BuildConfig.DEBUG) {
                    // My favorite debug logger!
                    Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            int numberOfStudents = 0;
            // Match the code and name using regex
            pattern = Pattern.compile("([0-9]+)");
            if (cellContentString != null) {
                matcher = pattern.matcher(cellContentString);
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }
            if (matcher != null) {
                if (matcher.find()) {
                    numberOfStudents = Integer.valueOf(matcher.group(1));
                }
                else {
                    if (BuildConfig.DEBUG) {
                        // My favorite debug logger!
                        Log.wtf("ImportExcel", "Really? Did you really mess with the excel file...?");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("ImportExcel", "How the hell did that?");
                }
                setResult(RESULT_CANCELED);
                finish();
            }

            if (isCancelled()) {
                return null;
            }

            // Get students
            for (int i = 5; i < numberOfStudents + 5; i++) {
                Student student = new Student();
                assert sheet != null;
                row = sheet.getRow(i);

                // Get RUN
                cell = row.getCell(1);
                cellContentString = RUT.cleanRut(StringFixer.fixCase(cell.getRichStringCellValue().getString()));
                if (!RUT.isValidRut(cellContentString)) {
                    if (BuildConfig.DEBUG) {
                        Log.wtf("ImportExcel", "The run \"" + cellContentString + "\" is invalid!");
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
                student.setRun(cellContentString);

                // Get names
                cell = row.getCell(2);
                cellContentString = StringFixer.fixCase(cell.getRichStringCellValue().getString());
                String[] names = cellContentString.split("\\s", 2);

                student.setFirstName(names[0]);
                student.setSecondName(names[1]);

                // Get last names
                cell = row.getCell(3);
                cellContentString = StringFixer.fixCase(cell.getRichStringCellValue().getString());
                student.setFirstLastName(cellContentString);
                cell = row.getCell(4);
                if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    // Oh my god, it's a gringo!
                    student.setSecondLastName("");
                }
                else {
                    cellContentString = StringFixer.fixCase(cell.getRichStringCellValue().getString());
                    student.setSecondLastName(cellContentString);
                }

                // Major id
                student.setMajor(subject.getMajorId());

                // Entered the uni in what year?
                cell = row.getCell(8);
                cellContentInt = (int) cell.getNumericCellValue();
                student.setEnrolled(cellContentInt);

                // Email
                cell = row.getCell(9);
                cellContentString = StringFixer.fixCase(cell.getRichStringCellValue().getString());
                student.setEmail(cellContentString);

                // Check if student exists in the database
                cursor = ubbDB.query(
                    DBStudents.TABLE_NAME,
                    new String[] {DBStudents.COLUMN_ID},
                    DBStudents.COLUMN_RUN + "=" + student.getRun(),
                    null,
                    null,
                    null,
                    null,
                    "1");

                values = new ContentValues();
                values.put(DBStudents.COLUMN_RUN, student.getRun());
                values.put(DBStudents.COLUMN_FIRST_NAME, student.getFirstName());
                values.put(DBStudents.COLUMN_SECOND_NAME, student.getSecondName());
                values.put(DBStudents.COLUMN_FIRST_LAST_NAME, student.getFirstLastName());
                values.put(DBStudents.COLUMN_SECOND_LAST_NAME, student.getSecondLastName());
                values.put(DBStudents.COLUMN_MAJOR, student.getMajor());
                values.put(DBStudents.COLUMN_ENROLLED, student.getEnrolled());
                values.put(DBStudents.COLUMN_EMAIL, student.getEmail());

                if (cursor.getCount() > 0) {
                    // They already exist, update their info
                    cursor.moveToFirst();
                    student.setStudentId(cursor.getInt(cursor.getColumnIndex(DBStudents.COLUMN_ID)));
                    ubbDB.update(DBStudents.TABLE_NAME, values, DBStudents.COLUMN_RUN + "=" + student.getRun(), null);
                }
                else {
                    // No, they does not exist, let's make them
                    student.setStudentId((int) ubbDB.insert(DBStudents.TABLE_NAME, null, values));
                }
                cursor.close();

                // Check if the student belongs to the course, if not add them
                cursor = ubbDB.query(
                    DBCoursesStudents.TABLE_NAME,
                    new String[] {DBCoursesStudents.COLUMN_ID},
                    DBCoursesStudents.COLUMN_STUDENT + "=" + student.getStudentId() + " AND " + DBCoursesStudents.COLUMN_COURSE + "=" + course.getCourseId(),
                    null,
                    null,
                    null,
                    null,
                    "1");

                if (cursor.getCount() == 0) {
                    // No, they does not exist, let's make them
                    values = new ContentValues();
                    values.put(DBCoursesStudents.COLUMN_STUDENT, student.getStudentId());
                    values.put(DBCoursesStudents.COLUMN_COURSE, course.getCourseId());
                    ubbDB.insert(DBCoursesStudents.TABLE_NAME, null, values);
                }
                cursor.close();
            }

            ubbDB.close();
            sqlParser.close();

            return subject;
        }

        @Override
        protected void onPostExecute(Subject subject) {
            // Release the kraken errr wakelock
            progress.dismiss();
            wakeLock.release();
            Intent intent = new Intent();
            intent.putExtra(StaticVariables.SUBJECT_OBJECT, subject);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}