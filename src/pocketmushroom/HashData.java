package pocketmushroom;

import android.os.*;
import pocketmushroom.*;

public class HashData implements Parcelable
{
	public String passwordHash = "";
	public String version = "";
	public String encryptionMethod = "";
	public String passwordSalt = "";
	public String encryptionSalt = "";

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags)
	{
		out.writeString(passwordHash);
		out.writeString(version);
		out.writeString(encryptionMethod);
		out.writeString(passwordSalt);
		out.writeString(encryptionSalt);
	}

	public static final Parcelable.Creator<HashData> CREATOR
	= new Parcelable.Creator<HashData>() {
		public HashData createFromParcel(Parcel in) {
			return new HashData(in);
		}

		public HashData[] newArray(int size) {
			return new HashData[size];
		}
	};
	
	public HashData()
	{}

	private HashData(Parcel in) {
		passwordHash = in.readString();
		version = in.readString();
		encryptionMethod = in.readString();
		passwordSalt = in.readString();
		encryptionSalt = in.readString();
	}
}
