import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Сетевой пакет, который содержит всю необходимю информацию для передачи по сети.
 */
public class Package implements Serializable {
    protected PackageType type;
    protected String sendingTime;

    Package(PackageType type) {
        this.type = type;
        this.sendingTime = generateTime();
    }

    public PackageType getType() {
        return this.type;
    }

    protected String generateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }

    @Override
    public String toString() {
        return "[ " + sendingTime + " ]: " + type;
    }
}

