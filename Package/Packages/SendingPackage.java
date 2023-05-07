import java.io.Serializable;

public class SendingPackage implements Serializable {
    private final byte[] data;

    SendingPackage(byte[] o) {
        this.data = o;
    }

    public byte[] getData() {
        return data;
    }
}
