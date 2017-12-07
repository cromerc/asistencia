package cl.cromer.ubb.attendance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import cl.cromer.ubb.attendance.DBSchema.*;

public class SQLParser extends SQLiteOpenHelper {

    public SQLParser(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Configuration
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "UBBAttendance.db";

    // Strings to make things easier to change code
    private static final String PRIMARY_TYPE = " INTEGER PRIMARY KEY";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String LONG_TYPE = " INTEGER";
    //private static final String FLOAT_TYPE = " REAL";
    private static final String BLOB_TYPE = " BLOB";
    //private static final String UNIQUE = " UNIQUE";
    private static final String COMMA_SEP = ",";

    // Database queries to create the schema
    private static final String SQL_CREATE_MAJORS = "CREATE TABLE " + DBMajors.TABLE_NAME + " (" +
        DBMajors.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBMajors.COLUMN_CODE + INT_TYPE + COMMA_SEP +
        DBMajors.COLUMN_NAME + TEXT_TYPE + ")";

    private static final String SQL_CREATE_SUBJECTS = "CREATE TABLE " + DBSubjects.TABLE_NAME + " (" +
        DBSubjects.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBSubjects.COLUMN_CODE + INT_TYPE + COMMA_SEP +
        DBSubjects.COLUMN_NAME + TEXT_TYPE + COMMA_SEP +
        DBSubjects.COLUMN_MAJOR + TEXT_TYPE + ")";

    private static final String SQL_CREATE_COURSES = "CREATE TABLE " + DBCourses.TABLE_NAME + " (" +
        DBCourses.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBCourses.COLUMN_SUBJECT + INT_TYPE + COMMA_SEP +
        DBCourses.COLUMN_SECTION + INT_TYPE + COMMA_SEP +
        DBCourses.COLUMN_YEAR + INT_TYPE + COMMA_SEP +
        DBCourses.COLUMN_SEMESTER + INT_TYPE + ")";

    private static final String SQL_CREATE_COURSES_STUDENTS = "CREATE TABLE " + DBCoursesStudents.TABLE_NAME + " (" +
        DBCoursesStudents.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBCoursesStudents.COLUMN_COURSE + INT_TYPE + COMMA_SEP +
        DBCoursesStudents.COLUMN_STUDENT + INT_TYPE + ")";

    private static final String SQL_CREATE_CLASSES = "CREATE TABLE " + DBClasses.TABLE_NAME + " (" +
        DBClasses.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBClasses.COLUMN_COURSE + INT_TYPE + COMMA_SEP +
        DBClasses.COLUMN_DATE + LONG_TYPE + ")";

    private static final String SQL_CREATE_STUDENTS = "CREATE TABLE " + DBStudents.TABLE_NAME + " (" +
        DBStudents.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBStudents.COLUMN_RUN + TEXT_TYPE + COMMA_SEP + // Text type because of the K
        DBStudents.COLUMN_FIRST_NAME + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_SECOND_NAME + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_FIRST_LAST_NAME + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_SECOND_LAST_NAME + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_FIRST_NAME_NORM + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_SECOND_NAME_NORM + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_FIRST_LAST_NAME_NORM + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_SECOND_LAST_NAME_NORM + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_MAJOR + INT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_EMAIL + TEXT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_ENROLLED + INT_TYPE + COMMA_SEP +
        DBStudents.COLUMN_PHOTO + BLOB_TYPE + ")";

    private static final String SQL_CREATE_ATTENDANCE = "CREATE TABLE " + DBAttendance.TABLE_NAME + " (" +
        DBAttendance.COLUMN_ID + PRIMARY_TYPE + COMMA_SEP +
        DBAttendance.COLUMN_CLASS + INT_TYPE + COMMA_SEP +
        DBAttendance.COLUMN_STUDENT + INT_TYPE + COMMA_SEP +
        DBAttendance.COLUMN_ATTENDANCE + INT_TYPE + ")";

    // Database queries to delete the schema
    private static final String SQL_DELETE_SUBJECTS = "DROP TABLE IF EXISTS " + DBSubjects.TABLE_NAME;
    private static final String SQL_DELETE_COURSES = "DROP TABLE IF EXISTS " + DBCourses.TABLE_NAME;
    private static final String SQL_DELETE_COURSES_STUDENTS = "DROP TABLE IF EXISTS " + DBCoursesStudents.TABLE_NAME;
    private static final String SQL_DELETE_STUDENTS = "DROP TABLE IF EXISTS " + DBStudents.TABLE_NAME;
    private static final String SQL_DELETE_CLASSES = "DROP TABLE IF EXISTS " + DBClasses.TABLE_NAME;
    private static final String SQL_DELETE_ATTENDANCE = "DROP TABLE IF EXISTS " + DBAttendance.TABLE_NAME;
    private static final String SQL_DELETE_MAJORS = "DROP TABLE IF EXISTS " + DBMajors.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        // The database does not exist yet, let's create it.
        db.execSQL(SQL_CREATE_MAJORS);
        db.execSQL(SQL_CREATE_SUBJECTS);
        db.execSQL(SQL_CREATE_COURSES);
        db.execSQL(SQL_CREATE_COURSES_STUDENTS);
        db.execSQL(SQL_CREATE_CLASSES);
        db.execSQL(SQL_CREATE_STUDENTS);
        db.execSQL(SQL_CREATE_ATTENDANCE);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrades to the database will go here.
        if (oldVersion < 2) {
            // Upgrade from database version 1
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Downgrading is not supported, so let's just delete everything and start from scratch
        deleteDatabase(db);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        // The database has been opened...
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // The android version is lower than 3.0 so we need to see if a downgrade is necessary
            if (db.getVersion() < DATABASE_VERSION) {
                // Yup, they are downgrading...
                onDowngrade(db, db.getVersion(), DATABASE_VERSION);
            }
        }
    }

    public void deleteDatabase(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_SUBJECTS);
        db.execSQL(SQL_DELETE_COURSES);
        db.execSQL(SQL_DELETE_COURSES_STUDENTS);
        db.execSQL(SQL_DELETE_MAJORS);
        db.execSQL(SQL_DELETE_STUDENTS);
        db.execSQL(SQL_DELETE_CLASSES);
        db.execSQL(SQL_DELETE_ATTENDANCE);
    }

    public String combineColumnsStrings(String[]... strings) {
        if (strings.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String[] subStringArray : strings) {
            for (String string : subStringArray) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(string);
            }
        }
        return stringBuilder.toString().trim();
    }
}