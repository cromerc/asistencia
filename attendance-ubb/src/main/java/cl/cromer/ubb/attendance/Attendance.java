package cl.cromer.ubb.attendance;

public class Attendance extends Student {
    private int status;
    // 0 - uninitialized
    // 1 - present
    // 2 - justified
    // 3 - absent
    // 4 - late

    private int present;
    private int justified;
    private int absent;
    private int late;

    public Attendance(int status, int studentId, String run, String firstName, String secondName, String firstLastName, String secondLastName, int major, int enrolled, String email, byte[] photo) {
        super(studentId, run, firstName, secondName, firstLastName, secondLastName, major, enrolled, email, photo);
        this.setStatus(status);
    }

    protected int getStatus() {
        return status;
    }

    protected void setStatus(int status) {
        this.status = status;
    }

    protected int getPresent() {
        return present;
    }

    protected void setPresent(int present) {
        this.present = present;
    }

    protected int getJustified() {
        return justified;
    }

    protected void setJustified(int justified) {
        this.justified = justified;
    }

    protected int getAbsent() {
        return absent;
    }

    protected void setAbsent(int absent) {
        this.absent = absent;
    }

    protected int getLate() {
        return late;
    }

    protected void setLate(int late) {
        this.late = late;
    }
}
