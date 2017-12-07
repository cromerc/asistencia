package cl.cromer.ubb.attendance;

public class Student {
    private int studentId;
    private String run;
    private String firstName;
    private String secondName;
    private String firstLastName;
    private String secondLastName;
    private int major;
    private int enrolled;
    private String email;
    private byte[] photo;

    public Student() {}

    public Student(int studentId, String run, String firstName, String secondName, String firstLastName, String secondLastName, int major, int enrolled, String email, byte[] photo) {
        this.setStudentId(studentId);
        this.setRun(run);
        this.setFirstName(firstName);
        this.setSecondName(secondName);
        this.setFirstLastName(firstLastName);
        this.setSecondLastName(secondLastName);
        this.setMajor(major);
        this.setEnrolled(enrolled);
        this.setEmail(email);
        this.setPhoto(photo);
    }

    protected int getStudentId() {
        return studentId;
    }

    protected void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    protected String getRun() {
        return run;
    }

    protected void setRun(String run) {
        this.run = run;
    }

    protected String getFullName() {
        return getFirstName() + " " + getSecondName() + " " + getFirstLastName() + " " + getSecondLastName();
    }

    protected String getFirstName() {
        return firstName;
    }

    protected void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    protected String getSecondName() {
        return secondName;
    }

    protected void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    protected String getFirstLastName() {
        return firstLastName;
    }

    protected void setFirstLastName(String firstLastName) {
        this.firstLastName = firstLastName;
    }

    protected String getSecondLastName() {
        return secondLastName;
    }

    protected void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    protected int getMajor() {
        return major;
    }

    protected void setMajor(int major) {
        this.major = major;
    }

    protected int getEnrolled() {
        return enrolled;
    }

    protected void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }

    protected String getEmail() {
        return email;
    }

    protected void setEmail(String email) {
        this.email = email;
    }

    protected byte[] getPhoto() {
        return photo;
    }

    protected void setPhoto(byte[] photo) {
        this.photo = photo;
    }
}
