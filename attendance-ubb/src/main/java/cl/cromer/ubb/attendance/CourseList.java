package cl.cromer.ubb.attendance;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.DBSchema.DBCourses;
import cl.cromer.ubb.attendance.DBSchema.DBClasses;
import cl.cromer.ubb.attendance.DBSchema.DBAttendance;
import cl.cromer.ubb.attendance.DBSchema.DBCoursesStudents;

public class CourseList extends AppCompatActivity {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    // Add course dialog window
    private AlertDialog addEditCourseDialog = null;
    private View addEditCourseView;

    private AlertDialog confirmDialog = null;

    // Floating action button
    private FloatingActionButton fab = null;

    // Multi select
    private boolean optionsSelected[] = null;

    // RecyclerView
    private RecyclerView recyclerView = null;
    private CourseListAdapter courseListAdapter = null;

    private Subject subject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive the subject from the previous activity
        Intent subjectListIntent = getIntent();
        subject = subjectListIntent.getParcelableExtra(StaticVariables.SUBJECT_OBJECT);

        setContentView(R.layout.activity_course_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(subject.getSubjectName());
        toolbar.setSubtitle(subject.getMajorName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Inflate the add course view
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addEditCourseView = inflater.inflate(R.layout.view_course_add_edit, new RelativeLayout(this), false);

        // Build the add course dialog window using the course view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(addEditCourseView);
        builder.setPositiveButton(R.string.input_accept, null);
        builder.setNegativeButton(R.string.input_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                eraseAddEditCourses();
            }
        });

        builder.setCancelable(false);
        addEditCourseDialog = builder.create();

        addEditCourseDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                addEditCourseDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                addEditCourseDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            }
        });

        addEditCourseDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEditCourseDialog.setTitle(R.string.courses_add_course);
                addEditCourseDialog.show();
                addCourseListeners(); // Override the accept button!
            }
        });

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
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                subject = intent.getParcelableExtra(StaticVariables.SUBJECT_OBJECT);
            }
        }
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
        if (addEditCourseDialog != null && addEditCourseDialog.isShowing()) {
            state.putBoolean("add_dialog_showing", addEditCourseDialog.isShowing());
            TextView textView = (TextView) addEditCourseDialog.findViewById(R.id.edit_year);
            state.putString("add_dialog_year", textView.getText().toString());
            textView = (TextView) addEditCourseDialog.findViewById(R.id.edit_semester);
            state.putString("add_dialog_semester", textView.getText().toString());
            textView = (TextView) addEditCourseDialog.findViewById(R.id.edit_section);
            state.putString("add_dialog_section", textView.getText().toString());
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
            addEditCourseView = inflater.inflate(R.layout.view_course_add_edit, new RelativeLayout(this), false);
            TextView textView = (TextView) addEditCourseView.findViewById(R.id.edit_year);
            textView.setText(savedInstanceState.getString("add_dialog_year"));
            textView = (TextView) addEditCourseView.findViewById(R.id.edit_semester);
            textView.setText(savedInstanceState.getString("add_dialog_semester"));
            textView = (TextView) addEditCourseView.findViewById(R.id.edit_section);
            textView.setText(savedInstanceState.getString("add_dialog_section"));
            addEditCourseDialog.setView(addEditCourseView);
            if (optionsSelected == null) {
                addEditCourseDialog.setTitle(R.string.courses_add_course);
                addEditCourseDialog.show();
                addCourseListeners(); // Override the accept button!
            }
            else {
                addEditCourseDialog.setTitle(R.string.courses_edit_course);
                addEditCourseDialog.show();
                editCourseListeners(); // Override the accept button!
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
        // Same for the add course dialog
        if (addEditCourseDialog != null && addEditCourseDialog.isShowing()) {
            addEditCourseDialog.dismiss();
        }
        if (confirmDialog != null && confirmDialog.isShowing()) {
            confirmDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (optionsSelected != null) {
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
            overridePendingTransition(R.anim.hold_back, R.anim.push_right_out);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.course_list, menu);
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
        getMenuInflater().inflate(R.menu.course_list, menu);
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
                            final EditCourse editCourse = new EditCourse(getApplicationContext());
                            editCourse.execute();

                            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    editCourse.cancel(true);
                                    addEditCourseDialog.dismiss();
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
            deleteCoursesConfirm();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        private List<Course> courses = new ArrayList<>();

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

            // Get courses
            Cursor cursor = ubbDB.query(
                DBCourses.TABLE_NAME,
                DBCourses.ALL_COLUMNS,
                DBCourses.COLUMN_SUBJECT + "=" + subject.getSubjectId(),
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
                        subject,
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
            progress.dismiss();
            wakeLock.release();
            // Create the recycler and add it to a layout manager, then add the content to it
            recyclerView = (RecyclerView) findViewById(R.id.course_list_recycler);
            recyclerView.setHasFixedSize(false);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            courseListAdapter = new CourseListAdapter(courses);
            recyclerView.setAdapter(courseListAdapter);
            recyclerView.addOnItemTouchListener(new RecyclerClickListener(context, recyclerView, new RecyclerClickListener.OnClickListener() {

                @Override
                public void onClick(View view, int position) {
                    if (optionsSelected == null) {
                        // Open the class
                        Intent classListIntent = new Intent(getApplicationContext(), ClassList.class);
                        classListIntent.putExtra(StaticVariables.COURSE_OBJECT, courseListAdapter.getCourse(position));
                        classListIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(classListIntent);
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

    private void deleteCoursesConfirm() {
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
                                deleteCourse();
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

    private void deleteCourse() {
        int options = optionsSelected.length;
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            if (optionsSelected[i]) {
                courses.add(courseListAdapter.getCourse(i));
            }
        }
        options = courses.size();
        for (int i = 0; i < options; i++) {
            Cursor cursor = ubbDB.query(DBClasses.TABLE_NAME, DBClasses.ALL_COLUMNS, DBClasses.COLUMN_COURSE + "=" + courses.get(i).getCourseId(), null, null, null, null);
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int classId = cursor.getInt(cursor.getColumnIndex(DBClasses.COLUMN_ID));

                    // Delete the attendance
                    ubbDB.delete(
                        DBAttendance.TABLE_NAME,
                        DBAttendance.COLUMN_CLASS + "=" + classId,
                        null
                    );
                }
                cursor.close();

                // Delete the classes
                ubbDB.delete(
                    DBClasses.TABLE_NAME,
                    DBClasses.COLUMN_COURSE + "=" + courses.get(i).getCourseId(),
                    null
                );
            }

            // Delete the students that were connected to the course
            ubbDB.delete(
                DBCoursesStudents.TABLE_NAME,
                DBCoursesStudents.COLUMN_COURSE + "=" + courses.get(i).getCourseId(),
                null
            );

            // Delete it from the database
            ubbDB.delete(
                DBCourses.TABLE_NAME,
                DBCourses.COLUMN_ID + "=" + courses.get(i).getCourseId(),
                null
            );
            // Delete it from the adapter
            courseListAdapter.deleteCourse(courses.get(i));
        }
        ubbDB.close();
        sqlParser.close();
        recyclerView.setAdapter(courseListAdapter);
        optionsSelected = null;
        fab.show();
        supportInvalidateOptionsMenu();
    }

    private void addCourseListeners() {
        // Override the back button so it closes the dialog
        addEditCourseDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                    dialog.cancel();
                    eraseAddEditCourses();
                    return true;
                }
                return false;
            }
        });

        // Override the back button
        addEditCourseDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Override the standard dismiss onClick to make sure that the window does not close

                TextView textView = (TextView) addEditCourseView.findViewById(R.id.edit_year);
                int year = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditCourseView.findViewById(R.id.edit_semester);
                int semester = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditCourseView.findViewById(R.id.edit_section);
                int section = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());

                if (String.valueOf(year).length() != 4) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_year, Toast.LENGTH_SHORT).show();
                }
                else if (semester < 1 || semester > 2) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_semester, Toast.LENGTH_SHORT).show();
                }
                else if (section == 0) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_section, Toast.LENGTH_SHORT).show();
                }
                else {
                    final Course course = new Course(subject, section, semester, year);

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {
                                    final AddCourse addCourse = new AddCourse(getApplicationContext());
                                    addCourse.execute(course);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            addCourse.cancel(true);
                                            addEditCourseDialog.dismiss();
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

    private void editCourseListeners() {
        // Override the back button so it closes the dialog
        addEditCourseDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                    dialog.cancel();
                    eraseAddEditCourses();
                    return true;
                }
                return false;
            }
        });

        // Override the back button
        addEditCourseDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Override the standard dismiss onClick to make sure that the window does not close

                TextView textView = (TextView) addEditCourseView.findViewById(R.id.edit_year);
                int year = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditCourseView.findViewById(R.id.edit_semester);
                int semester = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());
                textView = (TextView) addEditCourseView.findViewById(R.id.edit_section);
                int section = (textView.getText().toString().equals("")) ? 0 : Integer.parseInt(textView.getText().toString());

                int courseId = 0;
                for (int i = 0; i < optionsSelected.length; i++) {
                    if (optionsSelected[i]) {
                        courseId = courseListAdapter.getCourse(i).getCourseId();
                    }
                }

                if (String.valueOf(year).length() != 4) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_year, Toast.LENGTH_SHORT).show();
                }
                else if (semester < 1 || semester > 2) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_semester, Toast.LENGTH_SHORT).show();
                }
                else if (section == 0) {
                    Toast.makeText(getApplicationContext(), R.string.courses_invalid_section, Toast.LENGTH_SHORT).show();
                }
                else {
                    final Course course = new Course(subject, courseId, section, semester, year);

                    sqlParser = new SQLParser(getApplicationContext());
                    thread = new Thread(new Runnable() {
                        public void run() {
                            ubbDB = sqlParser.getWritableDatabase();
                            threadHandler.post(new Runnable() {
                                public void run() {
                                    final SaveEditCourse saveEditCourse = new SaveEditCourse(getApplicationContext());
                                    saveEditCourse.execute(course);

                                    progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            saveEditCourse.cancel(true);
                                            addEditCourseDialog.dismiss();
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

    private void eraseAddEditCourses() {
        TextView textView = (TextView) addEditCourseView.findViewById(R.id.edit_year);
        textView.setText("");
        textView.requestFocus();
        textView = (TextView) addEditCourseView.findViewById(R.id.edit_semester);
        textView.setText("");
        textView = (TextView) addEditCourseView.findViewById(R.id.edit_section);
        textView.setText("");
    }

    private class EditCourse extends AsyncTask<Void, Void, Course> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected EditCourse(Context context) {
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
        protected Course doInBackground(Void... variable) {
            if (isCancelled()) {
                return null;
            }

            Course course = new Course();

            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    course = courseListAdapter.getCourse(i);
                }
            }

            Cursor cursor = ubbDB.query(
                DBCourses.TABLE_NAME,
                DBCourses.ALL_COLUMNS,
                DBCourses.COLUMN_ID + "=" + course.getCourseId(),
                null,
                null,
                null,
                null,
                "1");

            cursor.moveToFirst();
            course = new Course(
                subject,
                cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_ID)),
                cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_SECTION)),
                cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_SEMESTER)),
                cursor.getInt(cursor.getColumnIndex(DBCourses.COLUMN_YEAR))
            );
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return course;
        }

        @Override
        protected void onPostExecute(Course course) {
            // Release the kraken errr wakelock
            wakeLock.release();
            addEditCourseDialog.setTitle(R.string.courses_edit_course);

            TextView textView = (TextView) addEditCourseView.findViewById(R.id.edit_year);
            textView.setText(String.valueOf(course.getYear()));
            textView = (TextView) addEditCourseView.findViewById(R.id.edit_semester);
            textView.setText(String.valueOf(course.getCourseSemester()));
            textView = (TextView) addEditCourseView.findViewById(R.id.edit_section);
            textView.setText(String.valueOf(course.getCourseSection()));

            addEditCourseDialog.show();
            editCourseListeners();
        }
    }

    private class SaveEditCourse extends AsyncTask<Course, Course, Boolean> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected SaveEditCourse(Context context) {
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
        protected Boolean doInBackground(Course... courses) {
            for (Course course : courses) {
                if (isCancelled()) {
                    return false;
                }

                Cursor cursor = ubbDB.query(DBCourses.TABLE_NAME,
                    DBCourses.ALL_COLUMNS, DBCourses.COLUMN_SECTION + "=" + course.getCourseSection() + " AND " + DBCourses.COLUMN_YEAR + "=" + course.getYear() + " AND " + DBCourses.COLUMN_SEMESTER + "=" + course.getCourseSemester()  + " AND " + DBCourses.COLUMN_SUBJECT + "=" + course.getSubjectId(),
                    null,
                    null,
                    null,
                    null,
                    "1");
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(DBCourses.COLUMN_SECTION, course.getCourseSection());
                    values.put(DBCourses.COLUMN_SEMESTER, course.getCourseSemester());
                    values.put(DBCourses.COLUMN_YEAR, course.getYear());
                    ubbDB.update(DBCourses.TABLE_NAME,
                        values,
                        DBCourses.COLUMN_ID + "=" + course.getCourseId(),
                        null);

                    ubbDB.close();
                    sqlParser.close();
                }
                else {
                    return true;
                }
                cursor.close();
            }
            publishProgress(courses);

            return false;
        }

        @Override
        protected void onProgressUpdate(Course... courses) {
            // Add the course to the adapter
            for (Course course : courses) {
                int index;
                for (index = 0; index < optionsSelected.length; index++) {
                    if (optionsSelected[index]) {
                        break;
                    }
                }
                courseListAdapter.updateCourse(index, course);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Release the kraken errr wakelock
            if (result) {
                Toast.makeText(context, R.string.courses_course_exists, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.courses_edited, Toast.LENGTH_SHORT).show();
            }
            wakeLock.release();
            addEditCourseDialog.dismiss();
            // Update the adapter list to show
            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            }
            recyclerView.swapAdapter(courseListAdapter, true);
            optionsSelected = null;
            invalidateOptionsMenu();
            fab.show();
            eraseAddEditCourses();
        }
    }

    private class AddCourse extends AsyncTask<Course, Course, Boolean> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected AddCourse(Context context) {
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
        protected Boolean doInBackground(Course... courses) {
            for (Course course : courses) {
                if (isCancelled()) {
                    return false;
                }

                Cursor cursor = ubbDB.query(DBCourses.TABLE_NAME,
                        DBCourses.ALL_COLUMNS, DBCourses.COLUMN_SECTION + "=" + course.getCourseSection() + " AND " + DBCourses.COLUMN_YEAR + "=" + course.getYear() + " AND " + DBCourses.COLUMN_SEMESTER + "=" + course.getCourseSemester()  + " AND " + DBCourses.COLUMN_SUBJECT + "=" + course.getSubjectId(),
                        null,
                        null,
                        null,
                        null,
                        "1");
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(DBCourses.COLUMN_SUBJECT, course.getSubjectId());
                    values.put(DBCourses.COLUMN_SECTION, course.getCourseSection());
                    values.put(DBCourses.COLUMN_SEMESTER, course.getCourseSemester());
                    values.put(DBCourses.COLUMN_YEAR, course.getYear());
                    ubbDB.insert(DBCourses.TABLE_NAME, null, values);
                }
                else {
                    return true;
                }
                cursor.close();

                ubbDB.close();
                sqlParser.close();
            }
            publishProgress(courses);

            return false;
        }

        @Override
        protected void onProgressUpdate(Course... courses) {
            // Add the course to the adapter
            for (Course course : courses) {
                courseListAdapter.addCourse(course);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Release the kraken errr wakelock
            if (result) {
                Toast.makeText(context, R.string.courses_course_exists, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.courses_added, Toast.LENGTH_SHORT).show();
            }
            wakeLock.release();
            addEditCourseDialog.dismiss();
            // Update the adapter list to show
            recyclerView.swapAdapter(courseListAdapter, true);
            eraseAddEditCourses();
        }
    }
}
