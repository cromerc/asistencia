package cl.cromer.ubb.attendance;

import android.os.Parcel;
import android.os.Parcelable;

public class Subject extends Major implements Parcelable  {
    protected int subjectId;
    protected int subjectCode;
    protected String subjectName;

    protected Subject() {}

    protected Subject(int subjectId, String subjectName, int subjectCode) {
        this.setSubjectId(subjectId);
        this.setSubjectName(subjectName);
        this.setSubjectCode(subjectCode);
    }

    protected Subject(Major major, String subjectName, int subjectCode) {
        super(major.getMajorId(), major.getMajorName(), major.getMajorCode());
        this.setSubjectName(subjectName);
        this.setSubjectCode(subjectCode);
    }

    protected Subject(Major major, int subjectId, String subjectName, int subjectCode) {
        super(major.getMajorId(), major.getMajorName(), major.getMajorCode());
        this.setSubjectId(subjectId);
        this.setSubjectName(subjectName);
        this.setSubjectCode(subjectCode);
    }

    protected void setSubject(Subject subject) {
        this.setMajorId(subject.getMajorId());
        this.setMajorName(subject.getMajorName());
        this.setMajorCode(subject.getMajorCode());
        this.setSubjectId(subject.getSubjectId());
        this.setSubjectName(subject.getSubjectName());
        this.setSubjectCode(subject.getSubjectCode());
    }

    protected int getSubjectId() {
        return subjectId;
    }

    protected void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    protected int getSubjectCode() {
        return subjectCode;
    }

    protected void setSubjectCode(int code) {
        this.subjectCode = code;
    }

    protected String getSubjectName() {
        return subjectName;
    }

    protected void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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
    }

    private void readFromParcel(Parcel in) {
        majorId = in.readInt();
        majorName = in.readString();
        majorCode = in.readInt();
        subjectId = in.readInt();
        subjectCode = in.readInt();
        subjectName = in.readString();
    }

    protected Subject(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Subject> CREATOR = new Parcelable.Creator<Subject>() {
        public Subject createFromParcel(Parcel in) {
            return new Subject(in);
        }

        public Subject[] newArray(int size) {
            return new Subject[size];
        }
    };
}
