package vn.iostar.doan.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User1 implements Parcelable {
    private Long userId;
    private String email;
    private String otp;
    private String password;
    private String fullName;

    public User1(Long userId) {
        this.userId = userId;
    }

    public User1() {
    }

    public User1(Long userId, String email, String password, String fullName, String otp) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.otp = otp;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    protected User1(Parcel in) {
        userId = in.readLong();
        email = in.readString();
        password = in.readString();
        fullName = in.readString();
        otp =  in.readString();
    }

    public static final Creator<User1> CREATOR = new Creator<User1>() {
        @Override
        public User1 createFromParcel(Parcel in) {
            return new User1(in);
        }

        @Override
        public User1[] newArray(int size) {
            return new User1[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(userId);
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(fullName);
        dest.writeString(otp);

    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters và setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
