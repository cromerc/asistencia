package cl.cromer.ubb.attendance;

import android.provider.BaseColumns;

final public class DBSchema {

    // Disable the constructor
    private DBSchema() {}

    protected static abstract class DBMajors implements BaseColumns {
        protected static final String TABLE_NAME = "majors";
        protected static final String COLUMN_ID = "major_id";
        protected static final String COLUMN_CODE = "major_code";
        protected static final String COLUMN_NAME = "major_name";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_CODE, COLUMN_NAME};
    }

    protected static abstract class DBSubjects implements BaseColumns {
        protected static final String TABLE_NAME = "subjects";
        protected static final String COLUMN_ID = "subject_id";
        protected static final String COLUMN_CODE = "subject_code";
        protected static final String COLUMN_NAME = "subject_name";
        protected static final String COLUMN_MAJOR = "subject_major";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_CODE, COLUMN_NAME, COLUMN_MAJOR};
    }

    protected static abstract class DBCourses implements BaseColumns {
        protected static final String TABLE_NAME = "courses";
        protected static final String COLUMN_ID = "course_id";
        protected static final String COLUMN_SUBJECT = "course_subject";
        protected static final String COLUMN_SECTION = "course_section";
        protected static final String COLUMN_SEMESTER = "course_semester";
        protected static final String COLUMN_YEAR = "course_year";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_SUBJECT, COLUMN_SECTION, COLUMN_SEMESTER, COLUMN_YEAR};
    }

    protected static abstract class DBCoursesStudents implements BaseColumns {
        protected static final String TABLE_NAME = "courses_students";
        protected static final String COLUMN_ID = "cs_id";
        protected static final String COLUMN_COURSE = "cs_course";
        protected static final String COLUMN_STUDENT = "cs_student";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_COURSE, COLUMN_STUDENT};
    }

    protected static abstract class DBClasses implements BaseColumns {
        protected static final String TABLE_NAME = "classes";
        protected static final String COLUMN_ID = "class_id";
        protected static final String COLUMN_COURSE = "class_course";
        protected static final String COLUMN_DATE = "class_date";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_COURSE, COLUMN_DATE};
    }

    protected static abstract class DBStudents implements BaseColumns {
        protected static final String TABLE_NAME = "students";
        protected static final String COLUMN_ID = "student_id";
        protected static final String COLUMN_RUN = "student_run";
        protected static final String COLUMN_FIRST_NAME = "student_first_name";
        protected static final String COLUMN_SECOND_NAME = "student_second_name";
        protected static final String COLUMN_FIRST_LAST_NAME = "student_first_last_name";
        protected static final String COLUMN_SECOND_LAST_NAME = "student_second_last_name";
        protected static final String COLUMN_FIRST_NAME_NORM = "student_first_name_norm";
        protected static final String COLUMN_SECOND_NAME_NORM = "student_second_name_norm";
        protected static final String COLUMN_FIRST_LAST_NAME_NORM = "student_first_last_name_norm";
        protected static final String COLUMN_SECOND_LAST_NAME_NORM = "student_second_last_name_norm";
        protected static final String COLUMN_MAJOR = "student_major";
        protected static final String COLUMN_EMAIL = "student_email";
        protected static final String COLUMN_ENROLLED = "student_enrolled";
        protected static final String COLUMN_PHOTO = "student_photo";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_RUN, COLUMN_FIRST_NAME, COLUMN_SECOND_NAME, COLUMN_FIRST_LAST_NAME, COLUMN_SECOND_LAST_NAME, COLUMN_MAJOR, COLUMN_EMAIL, COLUMN_ENROLLED, COLUMN_PHOTO};
    }

    protected static abstract class DBAttendance implements BaseColumns {
        protected static final String TABLE_NAME = "attendance";
        protected static final String COLUMN_ID = "attendance_id";
        protected static final String COLUMN_CLASS = "attendance_class";
        protected static final String COLUMN_STUDENT = "attendance_student";
        protected static final String COLUMN_ATTENDANCE = "attendance_attendance";
        protected static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_CLASS, COLUMN_STUDENT, COLUMN_ATTENDANCE};
    }
}
