package cl.cromer.ubb.attendance;

public interface TakeAttendanceListener {

    // Called when an attendance is saved to the database
    void onSaveComplete();
}
