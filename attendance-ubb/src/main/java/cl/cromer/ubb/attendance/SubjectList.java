package cl.cromer.ubb.attendance;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.List;

import cl.cromer.ubb.attendance.FileSystem;
import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.DBSchema.*;

public class SubjectList extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    // Add subject dialog window
    private AlertDialog addEditSubjectDialog = null;
    private View addEditSubjectView;

    private AlertDialog permissionDialog = null;
    private AlertDialog confirmDialog = null;

    // Floating action button
    private FloatingActionButton fab = null;

    // Multi select
    private boolean optionsSelected[] = null;

    // RecyclerView
    private RecyclerView recyclerView = null;
    private SubjectListAdapter subjectListAdapter = null;

    // Permission to read and write files?
    private int filePermission = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Set the main view
        setContentView(R.layout.activity_subject_list);

        // Create and show the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setSubtitle(getResources().getString(R.string.subjects_title));
        setSupportActionBar(toolbar);

        // Inflate the add subject view
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addEditSubjectView = inflater.inflate(R.layout.view_subject_add_edit, new RelativeLayout(this), false);

        // Build the add subject dialog window using the subject view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(addEditSubjectView);
        builder.setPositiveButton(R.string.input_accept, null);
        builder.setNegativeButton(R.string.input_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eraseAddEditSubjects();
            }
        });

        builder.setCancelable(false);
        addEditSubjectDialog = builder.create();

        addEditSubjectDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                addEditSubjectDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                addEditSubjectDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            }
        });

        addEditSubjectDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        // Floating action button for add subject
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEditSubjectDialog.setTitle(R.string.subjects_add_subject);
                addEditSubjectDialog.show();
                addSubjectListeners(); // Override the accept button!
            }
        });

        // Add the navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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

    // After the view is added to the screen check to see if it needs to be selected
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus){
            // Restore the recycler view after recreation
            if (optionsSelected != null) {
                fab.hide();
                for (int i = 0; i < optionsSelected.length; i++) {
                    if (optionsSelected[i]) {
                        CardView cardView = (CardView) recyclerView.getChildAt(i);
                        cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimarySelected));
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        // Save the state of the dialog
        if (addEditSubjectDialog != null && addEditSubjectDialog.isShowing()) {
            state.putBoolean("add_dialog_showing", addEditSubjectDialog.isShowing());
            TextView textView = (TextView) addEditSubjectDialog.findViewById(R.id.edit_subject_name);
            state.putString("add_dialog_subject_name", textView.getText().toString());
            textView = (TextView) addEditSubjectDialog.findViewById(R.id.edit_subject_code);
            state.putString("add_dialog_subject_code", textView.getText().toString());
            textView = (TextView) addEditSubjectDialog.findViewById(R.id.edit_major_name);
            state.putString("add_dialog_major_name", textView.getText().toString());
            textView = (TextView) addEditSubjectDialog.findViewById(R.id.edit_major_code);
            state.putString("add_dialog_major_code", textView.getText().toString());
        }
        // Save the state of the adapter
        state.putBooleanArray("options_selected", optionsSelected);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore the dialog if it was on screen
        optionsSelected = savedInstanceState.getBooleanArray("options_selected");
        if (savedInstanceState.getBoolean("add_dialog_showing", false)) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addEditSubjectView = inflater.inflate(R.layout.view_subject_add_edit, new RelativeLayout(this), false);
            TextView textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_name);
            textView.setText(savedInstanceState.getString("add_dialog_subject_name"));
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_code);
            textView.setText(savedInstanceState.getString("add_dialog_subject_code"));
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_name);
            textView.setText(savedInstanceState.getString("add_dialog_major_name"));
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_code);
            textView.setText(savedInstanceState.getString("add_dialog_major_code"));
            addEditSubjectDialog.setView(addEditSubjectView);
            if (optionsSelected == null) {
                addEditSubjectDialog.setTitle(R.string.subjects_add_subject);
                addEditSubjectDialog.show();
                addSubjectListeners(); // Override the accept button!
            }
            else {
                addEditSubjectDialog.setTitle(R.string.subjects_edit_subject);
                addEditSubjectDialog.show();
                editSubjectListeners(); // Override the accept button!
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We need to get rid of the progress bar if it's showing
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        // Same for the add subject dialog
        if (addEditSubjectDialog != null && addEditSubjectDialog.isShowing()) {
            addEditSubjectDialog.dismiss();
        }
        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
        }
        if (confirmDialog != null && confirmDialog.isShowing()) {
            confirmDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (optionsSelected != null) {
            fab.show();
            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    supportInvalidateOptionsMenu();
                }
            }
            optionsSelected = null;
        }
        else {
            super.onBackPressed();
            //overridePendingTransition(R.anim.expand, R.anim.fade_out);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.subject_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        int selectedCount = 0;
        if (optionsSelected != null) {
            for (boolean selected : optionsSelected) {
                if (selected) {
                    selectedCount++;
                    if (selectedCount > 1) {
                        break;
                    }
                }
            }
        }
        getMenuInflater().inflate(R.menu.subject_list, menu);
        if (optionsSelected == null) {
            menu.removeItem(R.id.action_delete);
            menu.removeItem(R.id.action_edit);
        }
        else if (optionsSelected != null && selectedCount > 1) {
            menu.removeItem(R.id.action_edit);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            // Load the database  then run the new thread
            sqlParser = new SQLParser(getApplicationContext());
            thread = new Thread(new Runnable() {
                public void run() {
                    ubbDB = sqlParser.getWritableDatabase();
                    threadHandler.post(new Runnable() {
                        public void run() {
                            final EditSubject editSubject = new EditSubject(getApplicationContext());
                            editSubject.execute();

                            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    editSubject.cancel(true);
                                    addEditSubjectDialog.dismiss();
                                    finish();
                                }
                            });
                        }
                    });
                }
            });
            thread.start();
            return true;
        }
        else if (id == R.id.action_delete) {
            deleteSubjectsConfirm();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_import_excel) {
            if (filePermission != PackageManager.PERMISSION_GRANTED) {
                //Show an explanation if they previously denied the permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.permission_file);
                    builder.setPositiveButton(R.string.input_accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_EXCEL);
                        }
                    });
                    builder.setCancelable(true);
                    permissionDialog = builder.create();

                    permissionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            permissionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            permissionDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        }
                    });

                    permissionDialog.show();
                }
                else {
                    // No explanation needed, we can request the permission.
                    requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_EXCEL);
                }
            }
            else {
                // We have permission, open the file manager.
                openFileManager("application/vnd.ms-excel", StaticVariables.IMPORT_EXCEL_SELECTOR);
            }
        }
        else if (id == R.id.nav_import_photo_zip) {
            if (filePermission != PackageManager.PERMISSION_GRANTED) {
                //Show an explanation if they previously denied the permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.permission_file);
                    builder.setPositiveButton(R.string.input_accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_PHOTO_ZIP);
                        }
                    });
                    builder.setCancelable(true);
                    permissionDialog = builder.create();

                    permissionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            permissionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            permissionDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        }
                    });

                    permissionDialog.show();
                }
                else {
                    // No explanation needed, we can request the permission.
                    requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_PHOTO_ZIP);
                }
            }
            else {
                // We have permission, open the file manager.
                openFileManager("application/zip", StaticVariables.IMPORT_PHOTO_ZIP_SELECTOR);
            }
        }
        else if (id == R.id.nav_report_class) {
            if (filePermission != PackageManager.PERMISSION_GRANTED) {
                //Show an explanation if they previously denied the permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.permission_file);
                    builder.setPositiveButton(R.string.input_accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_CLASS_REPORT);
                        }
                    });
                    builder.setCancelable(true);
                    permissionDialog = builder.create();

                    permissionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            permissionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            permissionDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        }
                    });

                    permissionDialog.show();
                }
                else {
                    // No explanation needed, we can request the permission.
                    requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_CLASS_REPORT);
                }
            }
            else {
                // We have permission, export the report
                Intent classReportIntent = new Intent(getApplicationContext(), ClassReport.class);
                startActivity(classReportIntent);
            }
        }
        else if (id == R.id.nav_report_course) {
            if (filePermission != PackageManager.PERMISSION_GRANTED) {
                //Show an explanation if they previously denied the permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.permission_file);
                    builder.setPositiveButton(R.string.input_accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_COURSE_REPORT);
                        }
                    });
                    builder.setCancelable(true);
                    permissionDialog = builder.create();

                    permissionDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            permissionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            permissionDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        }
                    });

                    permissionDialog.show();
                }
                else {
                    // No explanation needed, we can request the permission.
                    requestPermission(StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_COURSE_REPORT);
                }
            }
            else {
                // We have permission, export the report
                Intent courseReportIntent = new Intent(getApplicationContext(), CourseReport.class);
                startActivity(courseReportIntent);
            }
        }
        else if (id == R.id.nav_about) {
            Intent aboutIntent = new Intent(getApplicationContext(), About.class);
            startActivity(aboutIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_EXCEL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    filePermission = PackageManager.PERMISSION_GRANTED;
                    openFileManager("application/vnd.ms-excel", StaticVariables.IMPORT_EXCEL_SELECTOR);
                }
                else {
                    Toast.makeText(this, R.string.permission_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_PHOTO_ZIP: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    filePermission = PackageManager.PERMISSION_GRANTED;
                    openFileManager("application/zip", StaticVariables.IMPORT_PHOTO_ZIP_SELECTOR);
                }
                else {
                    Toast.makeText(this, R.string.permission_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_CLASS_REPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent classReportIntent = new Intent(getApplicationContext(), ClassReport.class);
                    startActivity(classReportIntent);
                }
                else {
                    Toast.makeText(this, R.string.permission_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case StaticVariables.PERMISSIONS_WRITE_EXTERNAL_STORAGE_COURSE_REPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent courseReportIntent = new Intent(getApplicationContext(), CourseReport.class);
                    startActivity(courseReportIntent);
                }
                else {
                    Toast.makeText(this, R.string.permission_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == StaticVariables.IMPORT_EXCEL_SELECTOR) {
            if (resultCode == RESULT_OK) {
                Uri uri = intent.getData();
                String filePath;
                String mimeType;
                InputStream inputStream = null;
                ContentResolver contentResolver = getContentResolver();
                Cursor cursor = contentResolver.query(intent.getData(),
                    null,
                    null,
                    null,
                    null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    filePath = uri.toString();
                    try {
                        inputStream = contentResolver.openInputStream(uri);
                    }
                    catch (FileNotFoundException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                            Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                        }
                    }
                    mimeType = cursor.getString(cursor.getColumnIndex("mime_type"));
                    cursor.close();
                }
                else {
                    filePath = uri.getPath();
                    mimeType = FileSystem.getMimeType(filePath);
                }

                if (mimeType == null || !mimeType.equals("application/vnd.ms-excel")) {
                    // Not an xls file
                    Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        POIFSFileSystem fs;
                        if (inputStream != null) {
                            fs = new POIFSFileSystem(inputStream);
                        }
                        else {
                            fs = new POIFSFileSystem(new FileInputStream(filePath));
                        }
                        Workbook wb = new HSSFWorkbook(fs);
                        Sheet sheet = wb.getSheetAt(0);

                        Row row = sheet.getRow(0);
                        int cols = row.getPhysicalNumberOfCells();
                        if (cols >= 1 && row.getFirstCellNum() == 2) {
                            Cell cell = row.getCell(2);
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                if (cell.getRichStringCellValue().getString().trim().equals("Listado de Alumnos")) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("SubjectList", "The xls file is valid");
                                    }
                                    Intent importExcelIntent = new Intent(getApplicationContext(), ImportExcel.class);
                                    importExcelIntent.putExtra(StaticVariables.IMPORT_EXCEL_FILE_TYPE, (inputStream != null)?0:1);
                                    importExcelIntent.putExtra(StaticVariables.IMPORT_EXCEL_FILE, filePath);
                                    startActivityForResult(importExcelIntent, StaticVariables.IMPORT_EXCEL_ACTIVITY);
                                }
                                else {
                                    if (BuildConfig.DEBUG) {
                                        Log.d("SubjectList", "Does not have the right cell");
                                        Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            else {
                                if (BuildConfig.DEBUG) {
                                    Log.d("SubjectList", "Wrong cell data type");
                                    Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        else {
                            if (BuildConfig.DEBUG) {
                                Log.d("SubjectList", "Has too few columns");
                                Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                            }
                        }
                        fs.close();
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                    catch (IOException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                            Toast.makeText(this, R.string.import_not_valid_excel, Toast.LENGTH_SHORT).show();
                            Log.d("SubjectList", "The file is either corrupt or isn't really an excel file");
                        }
                    }
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("SubjectList", "Somehow the file selection failed?");
                }
            }
        }
        else if (requestCode == StaticVariables.IMPORT_EXCEL_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                // Add the imported info from the excel if necessary
                Subject subject = intent.getParcelableExtra(StaticVariables.SUBJECT_OBJECT);
                if (!subjectListAdapter.hasSubject(subject)) {
                    subjectListAdapter.addSubject(subject);
                }
                recyclerView.swapAdapter(subjectListAdapter, true);
                Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("SubjectList", "Reading the excel file failed!");
                }
            }
        }
        else if (requestCode == StaticVariables.IMPORT_PHOTO_ZIP_SELECTOR) {
            if (resultCode == RESULT_OK) {
                Uri uri = intent.getData();
                String filePath;
                String mimeType;
                InputStream inputStream = null;
                ContentResolver contentResolver = getContentResolver();
                Cursor cursor = contentResolver.query(intent.getData(),
                    null,
                    null,
                    null,
                    null);

                if (cursor != null) {
                    cursor.moveToFirst();
                    filePath = uri.toString();
                    try {
                        inputStream = contentResolver.openInputStream(uri);
                    }
                    catch (FileNotFoundException e) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace();
                            Toast.makeText(this, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                        }
                    }
                    mimeType = cursor.getString(cursor.getColumnIndex("mime_type"));
                    cursor.close();
                }
                else {
                    filePath = uri.getPath();
                    mimeType = FileSystem.getMimeType(filePath);
                }

                if (mimeType == null || !mimeType.equals("application/zip")) {
                    // Not a zip file
                    Toast.makeText(this, R.string.import_not_valid_zip, Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent importZipIntent = new Intent(getApplicationContext(), ImportPhotos.class);
                    importZipIntent.putExtra(StaticVariables.IMPORT_PHOTO_ZIP_FILE_TYPE, (inputStream != null)?0:1);
                    importZipIntent.putExtra(StaticVariables.IMPORT_PHOTO_ZIP_FILE, filePath);
                    startActivityForResult(importZipIntent, StaticVariables.IMPORT_PHOTO_ZIP_ACTIVITY);
                }
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("SubjectList", "Somehow the file selection failed?");
                }
            }
        }
        else if (requestCode == StaticVariables.IMPORT_PHOTO_ZIP_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
            }
            else {
                if (BuildConfig.DEBUG) {
                    Log.wtf("SubjectList", "Reading the zip file failed!");
                }
            }
        }
    }

    private void requestPermission(int requestCode) {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, requestCode);
    }

    private void openFileManager(String intentType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(intentType);

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (list.size() > 0) {
            // Open the file chooser
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.import_choose_file)), requestCode);
        }
        else {
            // Give the user the option to install a file manager since they don't have one installed
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.estrongs.android.pop")));
            }
            catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.estrongs.android.pop")));
            }
        }
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

    private class ShowContent extends AsyncTask<Void, Void, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        private List<Subject> subjects = new ArrayList<>();

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
        protected Void doInBackground(Void... voids) {
            if (isCancelled()) {
                return null;
            }

            // Get subjects by name in ascending order
            String query = "SELECT " + sqlParser.combineColumnsStrings(DBSubjects.ALL_COLUMNS, DBMajors.ALL_COLUMNS) + " FROM " + DBSubjects.TABLE_NAME + " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBMajors.ALL_COLUMNS) +" FROM " + DBMajors.TABLE_NAME + ") " + DBMajors.TABLE_NAME + " ON " + DBSubjects.TABLE_NAME + "." + DBSubjects.COLUMN_MAJOR + "=" + DBMajors.TABLE_NAME + "." + DBMajors.COLUMN_ID + " ORDER BY " + DBSubjects.COLUMN_NAME + " ASC";
            if (BuildConfig.DEBUG) {
                Log.d("SubjectList", query);
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
            progress.dismiss();
            wakeLock.release();
            // Create the recycler and add it to a layout manager, then add the content to it
            recyclerView = (RecyclerView) findViewById(R.id.subject_list_recycler);
            recyclerView.setHasFixedSize(false);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            subjectListAdapter = new SubjectListAdapter(subjects);
            recyclerView.setAdapter(subjectListAdapter);
            recyclerView.addOnItemTouchListener(new RecyclerClickListener(context, recyclerView, new RecyclerClickListener.OnClickListener() {

                @Override
                public void onClick(View view, int position) {
                    if (optionsSelected == null) {
                        // Open the subject
                        Intent courseListIntent = new Intent(getApplicationContext(), CourseList.class);
                        courseListIntent.putExtra(StaticVariables.SUBJECT_OBJECT, subjectListAdapter.getSubject(position));
                        courseListIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(courseListIntent);
                        overridePendingTransition(R.anim.push_down_in, R.anim.hold_back);
                    }
                    else {
                        CardView cardView = (CardView) view;
                        // Swap the selection
                        if (optionsSelected[position]) {
                            optionsSelected[position] = false;
                            cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        }
                        else {
                            optionsSelected[position] = true;
                            cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimarySelected));
                        }

                        // If none of the options are selected turn off multi select
                        boolean somethingSelected = false;
                        for (boolean selected : optionsSelected) {
                            if (selected) {
                                somethingSelected = true;
                                break;
                            }
                        }
                        if (!somethingSelected) {
                            optionsSelected = null;
                            fab.show();
                        }
                        supportInvalidateOptionsMenu();
                    }
                }

                @Override
                public void onLongClick(View view, int position) {
                    if (optionsSelected == null) {
                        CardView cardView = (CardView) view;
                        cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimarySelected));
                        optionsSelected = new boolean[recyclerView.getChildCount()];
                        optionsSelected[position] = true;
                        fab.hide();
                        supportInvalidateOptionsMenu();
                    }
                }
            }));
        }
    }

    private void deleteSubjectsConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.input_confirm);
        builder.setPositiveButton(R.string.input_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sqlParser = new SQLParser(getApplicationContext());
                thread = new Thread(new Runnable() {
                    public void run() {
                        ubbDB = sqlParser.getWritableDatabase();
                        threadHandler.post(new Runnable() {
                            public void run() {
                                deleteSubject();
                            }
                        });
                    }
                });
                thread.start();
            }
        });
        builder.setNegativeButton(R.string.input_cancel, null);
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

    private void deleteSubject() {
        int options = optionsSelected.length;
        List<Subject> subjects = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            if (optionsSelected[i]) {
                subjects.add(subjectListAdapter.getSubject(i));
            }
        }
        options = subjects.size();
        for (int i = 0; i < options; i++) {
            Cursor cursor = ubbDB.query(DBMajors.TABLE_NAME, DBMajors.ALL_COLUMNS, DBMajors.COLUMN_ID + "=" + subjects.get(i).getMajorId(), null, null, null, null);

            // If only one subject uses the major delete the major
            if (cursor.getCount() == 1) {
                ubbDB.delete(
                    DBMajors.TABLE_NAME,
                    DBMajors.COLUMN_ID + "=" + subjects.get(i).getMajorId(),
                    null
                );
            }
            cursor.close();

            // Remove recursive
            cursor = ubbDB.query(DBCourses.TABLE_NAME, DBCourses.ALL_COLUMNS, DBCourses.COLUMN_SUBJECT + "=" + subjects.get(i).getSubjectId(), null, null, null, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int course = cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_ID));
                    Cursor cursor2 = ubbDB.query(DBClasses.TABLE_NAME, DBClasses.ALL_COLUMNS, DBClasses.COLUMN_COURSE + "=" + course, null, null, null, null);
                    if (cursor2.getCount() > 0) {
                        while (cursor2.moveToNext()) {
                            int classId = cursor2.getInt(cursor2.getColumnIndex(DBClasses.COLUMN_ID));

                            // Delete the attendance
                            ubbDB.delete(
                                DBAttendance.TABLE_NAME,
                                DBAttendance.COLUMN_CLASS + "=" + classId,
                                null
                            );
                        }
                        cursor2.close();

                        // Delete the classes
                        ubbDB.delete(
                            DBClasses.TABLE_NAME,
                            DBClasses.COLUMN_COURSE + "=" + course,
                            null
                        );
                    }

                    // Delete course
                    ubbDB.delete(
                        DBCourses.TABLE_NAME,
                        DBCourses.COLUMN_ID + "=" + course,
                        null
                    );

                    // Delete the students that were connected to the course
                    ubbDB.delete(
                        DBCoursesStudents.TABLE_NAME,
                        DBCoursesStudents.COLUMN_COURSE + "=" + course,
                        null
                    );
                }
            }

            // Delete it from the database
            ubbDB.delete(
                DBSubjects.TABLE_NAME,
                DBSubjects.COLUMN_ID + "=" + subjects.get(i).getSubjectId(),
                null
            );
            // Delete it from the adapter
            subjectListAdapter.deleteSubject(subjects.get(i));
        }
        ubbDB.close();
        sqlParser.close();
        recyclerView.setAdapter(subjectListAdapter);
        optionsSelected = null;
        fab.show();
        supportInvalidateOptionsMenu();
    }

    private void addSubjectListeners() {
        // Override the back button so it closes the dialog
        addEditSubjectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                    dialog.cancel();
                    eraseAddEditSubjects();
                    return true;
                }
                return false;
            }
        });

        // Override the back button
        addEditSubjectDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Override the standard dismiss onClick to make sure that the window does not close

                TextView textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_name);
                String subjectName = textView.getText().toString();
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_code);
                int subjectCode = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_name);
                String majorName = textView.getText().toString();
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_code);
                int majorCode = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());

                if (subjectName.trim().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_subject_name, Toast.LENGTH_SHORT).show();
                }
                else if (majorName.trim().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_major_name, Toast.LENGTH_SHORT).show();
                }
                else if (subjectCode == 0) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_subject_code, Toast.LENGTH_SHORT).show();
                }
                else if (String.valueOf(subjectCode).length() < 6) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_invalid_subject_code, Toast.LENGTH_SHORT).show();
                }
                else if (majorCode == 0) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_major_code, Toast.LENGTH_SHORT).show();
                }
                else if (String.valueOf(majorCode).length() < 4) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_invalid_major_code, Toast.LENGTH_SHORT).show();
                }
                else {
                    Major major = new Major(majorName, majorCode);
                    final Subject subject = new Subject(major, subjectName, subjectCode);

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {
                                    final AddSubject addSubject = new AddSubject(getApplicationContext());
                                    addSubject.execute(subject);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            addSubject.cancel(true);
                                            addEditSubjectDialog.dismiss();
                                            finish();
                                        }
                                    });
                                }
                            });
                        }
                    });
                    thread.start();
                }
            }
        });
    }

    private void editSubjectListeners() {
        // Override the back button so it closes the dialog
        addEditSubjectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                    dialog.cancel();
                    eraseAddEditSubjects();
                    return true;
                }
                return false;
            }
        });

        // Override the back button
        addEditSubjectDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Override the standard dismiss onClick to make sure that the window does not close

                TextView textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_name);
                String subjectName = textView.getText().toString();
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_code);
                int subjectCode = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_name);
                String majorName = textView.getText().toString();
                textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_code);
                int majorCode = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());

                int subjectId = 0;
                int majorId = 0;
                for (int i = 0; i < optionsSelected.length; i++) {
                    if (optionsSelected[i]) {
                        subjectId = subjectListAdapter.getSubject(i).getSubjectId();
                        majorId = subjectListAdapter.getSubject(i).getMajorId();
                    }
                }

                if (subjectName.trim().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_subject_name, Toast.LENGTH_SHORT).show();
                }
                else if (majorName.trim().equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_major_name, Toast.LENGTH_SHORT).show();
                }
                else if (subjectCode == 0) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_subject_code, Toast.LENGTH_SHORT).show();
                }
                else if (String.valueOf(subjectCode).length() < 6) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_invalid_subject_code, Toast.LENGTH_SHORT).show();
                }
                else if (majorCode == 0) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_blank_major_code, Toast.LENGTH_SHORT).show();
                }
                else if (String.valueOf(majorCode).length() < 4) {
                    Toast.makeText(getApplicationContext(), R.string.subjects_invalid_major_code, Toast.LENGTH_SHORT).show();
                }
                else {
                    Major major = new Major(majorId, majorName, majorCode);
                    final Subject subject = new Subject(major, subjectId, subjectName, subjectCode);

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {
                                    final SaveEditSubject saveEditSubject = new SaveEditSubject(getApplicationContext());
                                    saveEditSubject.execute(subject);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            saveEditSubject.cancel(true);
                                            addEditSubjectDialog.dismiss();
                                            finish();
                                        }
                                    });
                                }
                            });
                        }
                    });
                    thread.start();
                }
            }
        });
    }

    private void eraseAddEditSubjects() {
        TextView textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_name);
        textView.setText("");
        textView.requestFocus();
        textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_code);
        textView.setText("");
        textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_name);
        textView.setText("");
        textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_code);
        textView.setText("");
    }

    private class EditSubject extends AsyncTask<Void, Void, Subject> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected EditSubject(Context context) {
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
        protected Subject doInBackground(Void... variable) {
            if (isCancelled()) {
                return null;
            }

            Subject subject = new Subject();

            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    subject = subjectListAdapter.getSubject(i);
                }
            }

            String query = "SELECT " + sqlParser.combineColumnsStrings(DBSubjects.ALL_COLUMNS, DBMajors.ALL_COLUMNS) + " FROM " + DBSubjects.TABLE_NAME + " INNER JOIN (SELECT " + sqlParser.combineColumnsStrings(DBMajors.ALL_COLUMNS) +" FROM " + DBMajors.TABLE_NAME + ") " + DBMajors.TABLE_NAME + " ON " + DBSubjects.TABLE_NAME + "." + DBSubjects.COLUMN_MAJOR + "=" + DBMajors.TABLE_NAME + "." + DBMajors.COLUMN_ID + " WHERE " + DBSubjects.COLUMN_CODE + "=" + subject.getSubjectCode() + " LIMIT 1";
            if (BuildConfig.DEBUG) {
                Log.d("SubjectList", query);
            }
            Cursor cursor = ubbDB.rawQuery(query, null);

            cursor.moveToFirst();
            Major major = new Major(
                cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(DBMajors.COLUMN_NAME)),
                cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_CODE)));
            subject = new Subject(
                major,
                cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(DBSubjects.COLUMN_NAME)),
                cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_CODE))
            );
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return subject;
        }

        @Override
        protected void onPostExecute(Subject subject) {
            // Release the kraken errr wakelock
            wakeLock.release();
            addEditSubjectDialog.setTitle(R.string.subjects_edit_subject);

            TextView textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_name);
            textView.setText(subject.getSubjectName());
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_subject_code);
            textView.setText(String.valueOf(subject.getSubjectCode()));
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_name);
            textView.setText(subject.getMajorName());
            textView = (TextView) addEditSubjectView.findViewById(R.id.edit_major_code);
            textView.setText(String.valueOf(subject.getMajorCode()));

            addEditSubjectDialog.show();
            editSubjectListeners();
        }
    }

    private class SaveEditSubject extends AsyncTask<Subject, Subject, Void> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected SaveEditSubject(Context context) {
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
        protected Void doInBackground(Subject... subjects) {
            for (Subject subject : subjects) {
                if (isCancelled()) {
                    return null;
                }

                // Get the major id from the subject
                Cursor cursor = ubbDB.query(
                    DBSubjects.TABLE_NAME,
                    new String[] {DBSubjects.COLUMN_MAJOR},
                    DBSubjects.COLUMN_ID+ "=" + subject.getSubjectId(),
                    null,
                    null,
                    null,
                    "1");

                cursor.moveToFirst();
                int oldMajorId = cursor.getInt(cursor.getColumnIndex(DBSubjects.COLUMN_MAJOR));
                cursor.close();

                // Get the old major code and name based on the old id
                cursor = ubbDB.query(
                    DBMajors.TABLE_NAME,
                    DBMajors.ALL_COLUMNS,
                    DBMajors.COLUMN_ID+ "=" + oldMajorId,
                    null,
                    null,
                    null,
                    "1");

                cursor.moveToFirst();
                int oldMajorCode = cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_CODE));
                cursor.close();

                // Get a count of how many subjects use this major code
                cursor = ubbDB.query(
                    DBSubjects.TABLE_NAME,
                    new String[] {DBSubjects.COLUMN_ID},
                    DBSubjects.COLUMN_MAJOR+ "=" + oldMajorId,
                    null,
                    null,
                    null,
                    null);

                // Did they change the major code?
                if (oldMajorCode != subject.getMajorCode()) {
                    if (cursor.getCount() > 1) {
                        cursor.close();

                        // Check if a code already exists that we are changing to
                        cursor = ubbDB.query(
                            DBMajors.TABLE_NAME,
                            new String[]{DBMajors.COLUMN_ID},
                            DBMajors.COLUMN_CODE + "=" + subject.getMajorCode() + " AND " + DBMajors.COLUMN_ID + "!=" + subject.getMajorId(),
                            null,
                            null,
                            null,
                            "1");

                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            subject.setMajorId(cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)));

                            ContentValues values = new ContentValues();
                            values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                            ubbDB.update(DBMajors.TABLE_NAME,
                                values,
                                DBMajors.COLUMN_CODE + "=" + subject.getMajorCode(),
                                null);
                        }
                        else {
                            ContentValues values = new ContentValues();
                            values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
                            values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                            subject.setMajorId((int) ubbDB.insert(DBMajors.TABLE_NAME, null, values));
                        }
                        cursor.close();
                    }
                    else {
                        cursor.close();

                        // Check if a code already exists that we are changing to
                        cursor = ubbDB.query(
                            DBMajors.TABLE_NAME,
                            new String[] {DBMajors.COLUMN_ID},
                            DBMajors.COLUMN_CODE + "=" + subject.getMajorCode() + " AND " + DBMajors.COLUMN_ID + "!=" + subject.getMajorId(),
                            null,
                            null,
                            null,
                            "1");

                        ubbDB.delete(DBMajors.TABLE_NAME, DBMajors.COLUMN_CODE + "=" + oldMajorCode, null);

                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            subject.setMajorId(cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)));

                            ContentValues values = new ContentValues();
                            values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
                            values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                            ubbDB.update(DBMajors.TABLE_NAME,
                                values,
                                DBMajors.COLUMN_ID + "=" + subject.getMajorId(),
                                null);
                        }
                        else {
                            ContentValues values = new ContentValues();
                            values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
                            values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                            subject.setMajorId((int) ubbDB.insert(DBMajors.TABLE_NAME, null, values));
                        }
                        cursor.close();
                    }
                }
                else {
                    ContentValues values = new ContentValues();
                    values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                    ubbDB.update(DBMajors.TABLE_NAME,
                        values,
                        DBMajors.COLUMN_ID + "=" + subject.getMajorId(),
                        null);
                }

                ContentValues values = new ContentValues();
                values.put(DBSubjects.COLUMN_CODE, subject.getSubjectCode());
                values.put(DBSubjects.COLUMN_NAME, subject.getSubjectName());
                values.put(DBSubjects.COLUMN_MAJOR, subject.getMajorId());
                ubbDB.update(DBSubjects.TABLE_NAME,
                    values,
                    DBSubjects.COLUMN_CODE + "=" + subject.getSubjectCode(),
                    null);

                ubbDB.close();
                sqlParser.close();
            }
            publishProgress(subjects);

            return null;
        }

        @Override
        protected void onProgressUpdate(Subject... subjects) {
            // Add the subject to the adapter
            for (Subject subject : subjects) {
                int index;
                for (index = 0; index < optionsSelected.length; index++) {
                    if (optionsSelected[index]) {
                        break;
                    }
                }
                subjectListAdapter.updateSubject(index, subject);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            // Release the kraken errr wakelock
            Toast.makeText(context, R.string.subjects_edited, Toast.LENGTH_SHORT).show();
            wakeLock.release();
            addEditSubjectDialog.dismiss();
            // Update the adapter list to show
            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            }
            recyclerView.swapAdapter(subjectListAdapter, true);
            optionsSelected = null;
            invalidateOptionsMenu();
            fab.show();
            eraseAddEditSubjects();
        }
    }

    private class AddSubject extends AsyncTask<Subject, Subject, Integer> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected AddSubject(Context context) {
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
        protected Integer doInBackground(Subject... subjects) {
            for (Subject subject : subjects) {
                if (isCancelled()) {
                    return 0;
                }

                Cursor cursor = ubbDB.query(DBMajors.TABLE_NAME, new String[] {DBMajors.COLUMN_ID}, DBMajors.COLUMN_CODE + "=" + subject.getMajorCode(), null, null, null, "1");

                boolean majorExists = (cursor.getCount() > 0);
                if (majorExists) {
                    cursor.moveToFirst();
                    subject.setMajorId(cursor.getInt(cursor.getColumnIndex(DBMajors.COLUMN_ID)));
                }
                cursor.close();

                cursor = ubbDB.query(DBSubjects.TABLE_NAME, new String[] {DBSubjects.COLUMN_ID}, DBSubjects.COLUMN_CODE + "=" + subject.getSubjectCode(), null, null, null, "1");

                if (cursor.getCount() > 0) {
                    cursor.close();
                    return -1;
                }
                cursor.close();

                if (majorExists) {
                    ContentValues values = new ContentValues();
                    values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
                    values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                    ubbDB.update(DBMajors.TABLE_NAME, values, DBMajors.COLUMN_ID + "=" + subject.getMajorId(), null);
                }
                else {
                    ContentValues values = new ContentValues();
                    values.put(DBMajors.COLUMN_CODE, subject.getMajorCode());
                    values.put(DBMajors.COLUMN_NAME, subject.getMajorName());
                    subject.setMajorId((int) ubbDB.insert(DBMajors.TABLE_NAME, null, values));
                }

                ContentValues values = new ContentValues();
                values.put(DBSubjects.COLUMN_CODE, subject.getSubjectCode());
                values.put(DBSubjects.COLUMN_NAME, subject.getSubjectName());
                values.put(DBSubjects.COLUMN_MAJOR, subject.getMajorId());
                subject.setSubjectId((int) ubbDB.insert(DBSubjects.TABLE_NAME, null, values));

                ubbDB.close();
                sqlParser.close();
            }
            publishProgress(subjects);

            return 1;
        }

        @Override
        protected void onProgressUpdate(Subject... subjects) {
            // Add the subject to the adapter
            for (Subject subject : subjects) {
                subjectListAdapter.addSubject(subject);
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            // Release the kraken errr wakelock
            wakeLock.release();
            addEditSubjectDialog.dismiss();
            // Update the adapter list to show
            if (result == 1) {
                recyclerView.swapAdapter(subjectListAdapter, true);
                Toast.makeText(context, R.string.subjects_added, Toast.LENGTH_SHORT).show();
            }
            else if(result == -1) {
                Toast.makeText(context, R.string.subjects_subject_exists, Toast.LENGTH_SHORT).show();
            }
            eraseAddEditSubjects();
        }
    }
}
