package CProcess;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CGD {
    @Expose
    @SerializedName(value = "id")
    public String mId;
    @Expose
    @SerializedName(value = "file_name")
    public String mFileName;
    @Expose
    @SerializedName(value = "line_num")
    public String mLineNumber;
    @Expose
    @SerializedName(value = "label")
    public String mLabel;

    CGD(){}

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public String getLineNumber() {
        return mLineNumber;
    }

    public void setLineNumber(String lineNumber) {
        mLineNumber = lineNumber;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }
}
