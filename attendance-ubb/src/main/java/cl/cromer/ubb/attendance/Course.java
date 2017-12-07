package cl.cromer.ubb.attendance;

import android.os.Parcel;
import android.os.Parcelable;

public class Course extends Subject implements Parcelable {
    protected int courseId;
    protected int courseSection;
    protected int courseSemester;
    protected int courseYear;

    protected Course() {}

    protected Course(int courseId, int courseSection, int courseSemester, int courseYear) {
        this.setCourseId(courseId);
        this.setCourseSection(courseSection);
        this.setCourseSemester(courseSemester);
        this.setCourseYear(courseYear);
    }

    protected Course(Subject subject) {
        super(subject.getSubjectId(), subject.getSubjectName(), subject.getSubjectCode());
    }

    protected Course(Subject subject, int courseSection, int courseSemester, int courseYear) {
        super(subject.getSubjectId(), subject.getSubjectName(), subject.getSubjectCode());
        this.setCourseSection(courseSection);
        this.setCourseSemester(courseSemester);
        this.setCourseYear(courseYear);
    }

    protected Course(Subject subject, int courseId, int courseSection, int courseSemester, int courseYear) {
        super(subject.getSubjectId(), subject.getSubjectName(), subject.getSubjectCode());
        this.setCourseId(courseId);
        this.setCourseSection(courseSection);
        this.setCourseSemester(courseSemester);
        this.setCourseYear(courseYear);
    }

    protected void setCourse(Course course) {
        this.setCourseId(course.getCourseId());
        this.setCourseSection(course.getCourseSection());
        this.setCourseSemester(course.getCourseSemester());
        this.setCourseYear(course.getYear());
    }

    protected int getCourseSection() {
        return courseSection;
    }

    protected void setCourseSection(int courseSection) {
        this.courseSection = courseSection;
    }

    protected int getCourseSemester() {
        return courseSemester;
    }

    protected void setCourseSemester(int courseSemester) {
        this.courseSemester = courseSemester;
    }

    protected int getYear() {
        return courseYear;
    }

    protected void setCourseYear(int courseYear) {
        this.courseYear = courseYear;
    }

    protected int getCourseId() {
        return courseId;
    }

    protected void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    // Parcelable
    @Override
    public int describeContents() {
        //Must be overridden, but I don't need it.
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(majorId);
        out.writeString(majorName);
        out.writeInt(majorCode);
        out.writeInt(subjectId);
        out.writeInt(subjectCode);
        out.writeString(subjectName);
        out.writeInt(courseId);
        out.writeInt(courseSection);
        out.writeInt(courseSemester);
        out.writeInt(courseYear);
    }

    private void readFromParcel(Parcel in) {
        majorId = in.readInt();
        majorName = in.readString();
        majorCode = in.readInt();
        subjectId = in.readInt();
        subjectCode = in.readInt();
        subjectName = in.readString();
        courseId = in.readInt();
        courseSection = in.readInt();
        courseSemester = in.readInt();
        courseYear = in.readInt();
    }

    protected Course(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Course> CREATOR = new Parcelable.Creator<Course>() {
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        public Course[] newArray(int size) {
            return new Course[size];
        }
    };
}