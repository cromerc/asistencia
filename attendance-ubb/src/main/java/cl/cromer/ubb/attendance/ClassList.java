package cl.cromer.ubb.attendance;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cl.cromer.ubb.attendance.Progress;
import cl.cromer.ubb.attendance.DBSchema.DBClasses;
import cl.cromer.ubb.attendance.DBSchema.DBAttendance;

public class ClassList extends AppCompatActivity {

    // SQLite database
    private SQLParser sqlParser = null;
    private SQLiteDatabase ubbDB = null;

    // Background thread for the database
    private Thread thread = null;
    private Handler threadHandler = new Handler();

    // Progress bar
    private Progress progress = null;

    // Add class dialog window
    private AlertDialog addEditClassDialog = null;
    private View addEditClassView;

    private AlertDialog confirmDialog = null;

    // Floating action button
    private FloatingActionButton fab = null;

    // Multi select
    private boolean optionsSelected[] = null;

    // RecyclerView
    private RecyclerView recyclerView = null;
    private ClassListAdapter classListAdapter = null;

    private Course course = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receive the course from the previous activity
        Intent courseListIntent = getIntent();
        course = courseListIntent.getParcelableExtra(StaticVariables.COURSE_OBJECT);

        setContentView(R.layout.activity_class_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.classes_section) + " " + String.valueOf(course.getCourseSection()));
        toolbar.setSubtitle(String.valueOf(course.getYear()) + "-" + String.valueOf(course.getCourseSemester()));
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Inflate the add course view
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addEditClassView = inflater.inflate(R.layout.view_class_add_edit, new RelativeLayout(this), false);

        // Build the add course dialog window using the course view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(addEditClassView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);
            calendarView.setMinDate(calendar.getTimeInMillis());
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 2);
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 10);
            calendarView.setMaxDate(calendar.getTimeInMillis());
            calendarView.setShowWeekNumber(false);
            calendarView.setFirstDayOfWeek(Calendar.MONDAY);
        }
        else {
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.input_accept, null);
            builder.setNegativeButton(R.string.input_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    eraseAddEditClasses();
                }
            });
        }

        addEditClassDialog = builder.create();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addEditClassDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    addEditClassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    addEditClassDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            });
        }

        addEditClassDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEditClassDialog.setTitle(R.string.classes_add_class);
                addEditClassDialog.show();
                addEditClassDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        eraseAddEditClasses();
                    }
                });
                addClassListeners();
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
        if (addEditClassDialog != null && addEditClassDialog.isShowing()) {
            state.putBoolean("add_dialog_showing", addEditClassDialog.isShowing());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                CalendarView calendarView = (CalendarView) addEditClassDialog.findViewById(R.id.calendar_view);
                state.putLong("add_dialog_date", calendarView.getDate());
            }
            else {
                DatePicker datePicker = (DatePicker) addEditClassDialog.findViewById(R.id.date_picker);
                Calendar calendar = Calendar.getInstance();
                calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                state.putLong("add_dialog_date", calendar.getTimeInMillis());
            }
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
            addEditClassView = inflater.inflate(R.layout.view_class_add_edit, new RelativeLayout(this), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
                calendarView.setDate(savedInstanceState.getLong("add_dialog_date"));
            }
            else {
                DatePicker datePicker = (DatePicker) addEditClassView.findViewById(R.id.date_picker);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(savedInstanceState.getLong("add_dialog_date"));
                datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }
            addEditClassDialog.setView(addEditClassView);
            if (optionsSelected == null) {
                addEditClassDialog.setTitle(R.string.classes_add_class);
                addEditClassDialog.show();
                addClassListeners(); // Override the accept button!
            }
            else {
                addEditClassDialog.setTitle(R.string.classes_edit_class);
                addEditClassDialog.show();
                editClassListeners(); // Override the accept button!
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
        // Same for the add class dialog
        if (addEditClassDialog != null && addEditClassDialog.isShowing()) {
            addEditClassDialog.dismiss();
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
        getMenuInflater().inflate(R.menu.class_list, menu);
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
        getMenuInflater().inflate(R.menu.class_list, menu);
        if (optionsSelected == null) {
            menu.removeItem(R.id.action_delete);
            menu.removeItem(R.id.action_edit);
            menu.removeItem(R.id.action_correct);
            menu.removeItem(R.id.action_late);
        }
        else if (optionsSelected != null && selectedCount > 1) {
            menu.removeItem(R.id.action_edit);
            menu.removeItem(R.id.action_correct);
            menu.removeItem(R.id.action_late);
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
                            final EditClass editClass = new EditClass(getApplicationContext());
                            editClass.execute();

                            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    editClass.cancel(true);
                                    addEditClassDialog.dismiss();
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
            deleteClassesConfirm();
            return true;
        }
        else if (id == R.id.action_correct) {
            Intent attendanceListIntent = new Intent(getApplicationContext(), CorrectAttendance.class);

            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    attendanceListIntent.putExtra(StaticVariables.CLASS_OBJECT, classListAdapter.getClass(i));
                    fab.show();
                    supportInvalidateOptionsMenu();
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    break;
                }
            }
            optionsSelected = null;

            startActivity(attendanceListIntent);
            return true;
        }
        else if (id == R.id.action_late) {
            Intent attendanceListIntent = new Intent(getApplicationContext(), LateStudentAttendance.class);

            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    attendanceListIntent.putExtra(StaticVariables.CLASS_OBJECT, classListAdapter.getClass(i));
                    fab.show();
                    supportInvalidateOptionsMenu();
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    break;
                }
            }
            optionsSelected = null;

            startActivity(attendanceListIntent);
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

        private List<Class> classes = new ArrayList<>();

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
                DBClasses.TABLE_NAME,
                DBClasses.ALL_COLUMNS,
                DBClasses.COLUMN_COURSE + "=" + course.getCourseId(),
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
                        course,
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
            progress.dismiss();
            wakeLock.release();
            // Create the recycler and add it to a layout manager, then add the content to it
            recyclerView = (RecyclerView) findViewById(R.id.class_list_recycler);
            recyclerView.setHasFixedSize(false);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            classListAdapter = new ClassListAdapter(classes, context);
            recyclerView.setAdapter(classListAdapter);
            recyclerView.addOnItemTouchListener(new RecyclerClickListener(context, recyclerView, new RecyclerClickListener.OnClickListener() {

                @Override
                public void onClick(View view, int position) {
                    if (optionsSelected == null) {
                        // Open the attendance
                        Intent attendanceListIntent = new Intent(getApplicationContext(), TakeAttendance.class);
                        attendanceListIntent.putExtra(StaticVariables.CLASS_OBJECT, classListAdapter.getClass(position));
                        startActivity(attendanceListIntent);
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

    private void deleteClassesConfirm() {
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
                                deleteClass();
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

    private void deleteClass() {
        int options = optionsSelected.length;
        List<Class> classes = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            if (optionsSelected[i]) {
                classes.add(classListAdapter.getClass(i));
            }
        }
        options = classes.size();
        for (int i = 0; i < options; i++) {
            // Delete the attendance
            ubbDB.delete(
                DBAttendance.TABLE_NAME,
                DBAttendance.COLUMN_CLASS + "=" + classes.get(i).getClassId(),
                null
            );

            // Delete it from the database
            ubbDB.delete(
                DBClasses.TABLE_NAME,
                DBClasses.COLUMN_ID + "=" + classes.get(i).getClassId(),
                null
            );
            // Delete it from the adapter
            classListAdapter.deleteClass(classes.get(i));
        }
        ubbDB.close();
        sqlParser.close();
        recyclerView.setAdapter(classListAdapter);
        optionsSelected = null;
        fab.show();
        supportInvalidateOptionsMenu();
    }

    private void addClassListeners() {
        // This is showing is to fix a bug with the onDateChangeListener in older versions of android, the listener runs when I change the date programmatically, but it should only change if the user makes a change
        if (addEditClassDialog.isShowing()) {
            // Override the back button, but only on old versions of android
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                addEditClassDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                            dialog.cancel();
                            return true;
                        }
                        return false;
                    }
                });

                // Override the back button
                addEditClassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Override the standard dismiss onClick to make sure that the window does not close
                        DatePicker datePicker = (DatePicker) addEditClassView.findViewById(R.id.date_picker);

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        final Class classObject = new Class(course, calendar.getTimeInMillis());

                        sqlParser = new SQLParser(getApplicationContext());
                        thread = new Thread(new Runnable() {
                            public void run() {
                                ubbDB = sqlParser.getWritableDatabase();
                                threadHandler.post(new Runnable() {
                                    public void run() {
                                        final AddClass addClass = new AddClass(getApplicationContext());
                                        addClass.execute(classObject);

                                        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                addClass.cancel(true);
                                                addEditClassDialog.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                });
            }
            else {
                CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
                calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                    @Override
                    public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, month, day);
                        final Class classObject = new Class(course, calendar.getTimeInMillis());

                        sqlParser = new SQLParser(getApplicationContext());
                        thread = new Thread(new Runnable() {
                            public void run() {
                                ubbDB = sqlParser.getWritableDatabase();
                                threadHandler.post(new Runnable() {
                                    public void run() {
                                        final AddClass addClass = new AddClass(getApplicationContext());
                                        addClass.execute(classObject);

                                        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                addClass.cancel(true);
                                                addEditClassDialog.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                });
            }
        }
    }

    private void editClassListeners() {
        // This is showing is to fix a bug with the onDateChangeListener in older versions of android, the listener runs when I change the date programmatically, but it should only change if the user makes a change
        if (addEditClassDialog.isShowing()) {
            // Override the back button, but only on old versions of android
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                addEditClassDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                            dialog.cancel();
                            return true;
                        }
                        return false;
                    }
                });

                // Override the back button
                addEditClassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Override the standard dismiss onClick to make sure that the window does not close
                        DatePicker datePicker = (DatePicker) addEditClassView.findViewById(R.id.date_picker);

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        final Class classObject = new Class(course, calendar.getTimeInMillis());

                        sqlParser = new SQLParser(getApplicationContext());
                        thread = new Thread(new Runnable() {
                            public void run() {
                                ubbDB = sqlParser.getWritableDatabase();
                                threadHandler.post(new Runnable() {
                                    public void run() {
                                        final SaveEditClass saveEditClass = new SaveEditClass(getApplicationContext());
                                        saveEditClass.execute(classObject);

                                        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                saveEditClass.cancel(true);
                                                addEditClassDialog.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                });
            }
            else {
                CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
                calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                    @Override
                    public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, month, day);
                        final Class classObject = new Class(course, calendar.getTimeInMillis());

                        sqlParser = new SQLParser(getApplicationContext());
                        thread = new Thread(new Runnable() {
                            public void run() {
                                ubbDB = sqlParser.getWritableDatabase();
                                threadHandler.post(new Runnable() {
                                    public void run() {
                                        final SaveEditClass saveEditClass = new SaveEditClass(getApplicationContext());
                                        saveEditClass.execute(classObject);

                                        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                saveEditClass.cancel(true);
                                                addEditClassDialog.dismiss();
                                                finish();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                });
            }
        }
    }

    private class SaveEditClass extends AsyncTask<Class, Class, Boolean> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected SaveEditClass(Context context) {
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
        protected Boolean doInBackground(Class... classes) {
            for (Class classObject : classes) {
                if (isCancelled()) {
                    return false;
                }

                Cursor cursor = ubbDB.query(DBClasses.TABLE_NAME,
                    DBClasses.ALL_COLUMNS, DBClasses.COLUMN_DATE + "=" + classObject.getDate(),
                    null,
                    null,
                    null,
                    null,
                    "1");
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(DBClasses.COLUMN_DATE, classObject.getDate());
                    ubbDB.update(DBClasses.TABLE_NAME,
                        values,
                        DBClasses.COLUMN_ID + "=" + classObject.getClassId(),
                        null);
                }
                else {
                    return true;
                }
                cursor.close();

                ubbDB.close();
                sqlParser.close();
            }
            publishProgress(classes);

            return false;
        }

        @Override
        protected void onProgressUpdate(Class... classes) {
            // Add the class to the adapter
            for (Class classObject : classes) {
                int index;
                for (index = 0; index < optionsSelected.length; index++) {
                    if (optionsSelected[index]) {
                        break;
                    }
                }
                classListAdapter.updateClass(index, classObject);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Release the kraken errr wakelock
            if (result) {
                Toast.makeText(context, R.string.classes_class_exists, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.classes_edited, Toast.LENGTH_SHORT).show();
            }
            wakeLock.release();
            addEditClassDialog.dismiss();
            // Update the adapter list to show
            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    CardView cardView = (CardView) recyclerView.getChildAt(i);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            }
            recyclerView.swapAdapter(classListAdapter, true);
            optionsSelected = null;
            invalidateOptionsMenu();
            fab.show();
            eraseAddEditClasses();
        }
    }

    private void eraseAddEditClasses() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
            calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                    // This is a to fix a bug in older versions of android, remove the listener, then set the listener again
                }
            });
            calendarView.setDate(System.currentTimeMillis());
        }
        else {
            DatePicker datePicker = (DatePicker) addEditClassView.findViewById(R.id.date_picker);
            Calendar calendar = Calendar.getInstance();
            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        }
    }

    private class EditClass extends AsyncTask<Void, Void, Class> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected EditClass(Context context) {
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
        protected Class doInBackground(Void... variable) {
            if (isCancelled()) {
                return null;
            }

            Class classObject = new Class();

            for (int i = 0; i < optionsSelected.length; i++) {
                if (optionsSelected[i]) {
                    classObject = classListAdapter.getClass(i);
                }
            }

            Cursor cursor = ubbDB.query(
                DBClasses.TABLE_NAME,
                DBClasses.ALL_COLUMNS,
                DBClasses.COLUMN_ID + "=" + classObject.getClassId(),
                null,
                null,
                null,
                null,
                "1");

            cursor.moveToFirst();
            classObject = new Class(
                course,
                cursor.getInt(cursor.getColumnIndex(DBClasses.COLUMN_ID)),
                cursor.getLong(cursor.getColumnIndex(DBClasses.COLUMN_DATE))
            );
            cursor.close();

            // Close the database connection
            ubbDB.close();
            sqlParser.close();

            return classObject;
        }

        @Override
        protected void onPostExecute(Class classObject) {
            // Release the kraken errr wakelock
            wakeLock.release();
            addEditClassDialog.setTitle(R.string.classes_edit_class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                CalendarView calendarView = (CalendarView) addEditClassView.findViewById(R.id.calendar_view);
                calendarView.setDate(classObject.getDate());
            }
            else {
                DatePicker datePicker = (DatePicker) addEditClassView.findViewById(R.id.date_picker);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(classObject.getDate());
                datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            }

            addEditClassDialog.show();
            editClassListeners();
        }
    }

    private class AddClass extends AsyncTask<Class, Class, Boolean> {
        private Context context;
        private PowerManager.WakeLock wakeLock;

        protected AddClass(Context context) {
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
        protected Boolean doInBackground(Class... classes) {
            for (Class classObject : classes) {
                if (isCancelled()) {
                    return false;
                }

                Cursor cursor = ubbDB.query(DBClasses.TABLE_NAME,
                    DBClasses.ALL_COLUMNS, DBClasses.COLUMN_DATE + "=" + classObject.getDate() + " AND " + DBClasses.COLUMN_COURSE + "=" + classObject.getCourseId(),
                    null,
                    null,
                    null,
                    null,
                    "1");
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(DBClasses.COLUMN_DATE, classObject.getDate());
                    values.put(DBClasses.COLUMN_COURSE, classObject.getCourseId());
                    classObject.setClassId((int) ubbDB.insert(DBClasses.TABLE_NAME, null, values));
                }
                else {
                    return true;
                }
                cursor.close();

                ubbDB.close();
                sqlParser.close();
            }
            publishProgress(classes);

            return false;
        }

        @Override
        protected void onProgressUpdate(Class... classes) {
            // Add the classes to the adapter
            for (Class classObject : classes) {
                classListAdapter.addClass(classObject);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Release the kraken errr wakelock
            if (result) {
                Toast.makeText(context, R.string.classes_class_exists, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.classes_added, Toast.LENGTH_SHORT).show();
            }
            wakeLock.release();
            addEditClassDialog.dismiss();
            // Update the adapter list to show
            recyclerView.swapAdapter(classListAdapter, true);
            eraseAddEditClasses();
        }
    }
}
