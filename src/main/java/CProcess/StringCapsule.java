package CProcess;

import java.util.UUID;

public class StringCapsule {
    String mString;
    UUID mUUID;

    private StringCapsule(String s) {
        mString = s;
        mUUID = UUID.randomUUID();
    }

    public static StringCapsule newInstance(String s) {
        return new StringCapsule(s);
    }

    public String getString() {
        return mString;
    }

    public String toString() {
        return mString;
    }
}
