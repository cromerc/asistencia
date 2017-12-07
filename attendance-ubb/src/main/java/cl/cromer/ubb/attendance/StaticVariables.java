package cl.cromer.ubb.attendance;

final public class StaticVariables {

    // Disable the constructor
    private StaticVariables() {}

    // Global variable
    protected final static String PACKAGE_NAME = "cl.cromer.ubb.attendance";

    protected final static int PERMISSIONS_WRITE_EXTERNAL_STORAGE_EXCEL = 1;
    protected final static int PERMISSIONS_WRITE_EXTERNAL_STORAGE_PHOTO_ZIP = 2;
    protected final static int PERMISSIONS_WRITE_EXTERNAL_STORAGE_CLASS_REPORT = 3;
    protected final static int PERMISSIONS_WRITE_EXTERNAL_STORAGE_COURSE_REPORT = 4;

    protected final static int IMPORT_EXCEL_SELECTOR = 1;
    protected final static int IMPORT_EXCEL_ACTIVITY = 2;
    protected final static String IMPORT_EXCEL_FILE = PACKAGE_NAME + ".EXCEL_FILE_NAME";
    protected final static String IMPORT_EXCEL_FILE_TYPE = PACKAGE_NAME + ".EXCEL_FILE_TYPE";

    protected final static int IMPORT_PHOTO_ZIP_SELECTOR = 3;
    protected final static int IMPORT_PHOTO_ZIP_ACTIVITY = 4;
    protected final static String IMPORT_PHOTO_ZIP_FILE = PACKAGE_NAME + ".PHOTO_ZIP_FILE_NAME";
    protected final static String IMPORT_PHOTO_ZIP_FILE_TYPE = PACKAGE_NAME + ".PHOTO_ZIP_FILE_TYPE";

    protected final static String SUBJECT_OBJECT = PACKAGE_NAME + ".SUBJECT_OBJECT";
    protected final static String COURSE_OBJECT = PACKAGE_NAME + ".COURSE_OBJECT";
    protected final static String CLASS_OBJECT = PACKAGE_NAME + ".CLASS_OBJECT";
}
