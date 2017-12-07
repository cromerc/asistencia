package cl.cromer.ubb.attendance;

import android.os.Parcel;
import android.os.Parcelable;

public class Major implements Parcelable {
    protected int majorId;
    protected String majorName;
    protected int majorCode;

    protected Major() {}

    protected Major(String majorName, int majorCode) {
        this.setMajorName(majorName);
        this.setMajorCode(majorCode);
    }

    protected Major(int majorId, String majorName, int majorCode) {
        this.setMajorId(majorId);
        this.setMajorName(majorName);
        this.setMajorCode(majorCode);
    }

    public int getMajorId() {
        return majorId;
    }

    public void setMajorId(int majorId) {
        this.majorId = majorId;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public int getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(int majorCode) {
        this.majorCode = majorCode;
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
    }

    private void readFromParcel(Parcel in) {
        majorId = in.readInt();
        majorName = in.readString();
        majorCode = in.readInt();
    }

    protected Major(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Major> CREATOR = new Parcelable.Creator<Major>() {
        public Major createFromParcel(Parcel in) {
            return new Major(in);
        }

        public Major[] newArray(int size) {
            return new Major[size];
        }
    };
}
