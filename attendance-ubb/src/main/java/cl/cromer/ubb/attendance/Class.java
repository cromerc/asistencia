package cl.cromer.ubb.attendance;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.util.Calendar;

public class Class extends Course implements Parcelable {
    private int classId;
    private long date;

    public Class() {}

    public Class(Course course, long date) {
        super(course.getCourseId(), course.getCourseSection(), course.getCourseSemester(), course.getYear());
        this.setDate(date);
    }

    public Class(Course course, int classId, long date) {
        super(course.getCourseId(), course.getCourseSection(), course.getCourseSemester(), course.getYear());
        this.setClassId(classId);
        this.setDate(date);
    }

    protected void setClass(Class classObject) {
        this.setClassId(classObject.getClassId());
        this.setDate(classObject.getDate());
    }

    protected int getClassId() {
        return classId;
    }

    protected void setClassId(int classId) {
        this.classId = classId;
    }

    protected long getDate() {
        return date;
    }

    protected void setDate(long date) {
        // Remove the time, I only need the date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTimeInMillis();

        this.date = date;
    }

    protected String getFormattedDate(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, context.getResources().getConfiguration().locale);
        return dateFormat.format(calendar.getTime());
    }

    protected String getFormattedShortDate(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, context.getResources().getConfiguration().locale);
        return dateFormat.format(calendar.getTime());
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
        out.writeInt(classId);
        out.writeLong(date);
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
        classId = in.readInt();
        date = in.readLong();
    }

    private Class(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Class> CREATOR = new Parcelable.Creator<Class>() {
        public Class createFromParcel(Parcel in) {
            return new Class(in);
        }

        public Class[] newArray(int size) {
            return new Class[size];
        }
    };
}
