import java.util.Random;

/**
 * Класс для генерации случайных паролей.
 */
public class RandomPasswordGenerator {
    private RandomPasswordGenerator() {}

    static private final String[] data = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m",
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "_"
    };

    static String genPass() {
        final int length = 15;

        String[] p = new String[length];

        Random r = new Random();
        for (int i = 0; i < length; i++) {
            p[i] = data[r.nextInt(data.length)];
        }

        StringBuilder password = new StringBuilder();

        for (String s : p) {
            password.append(s);
        }

        return password.toString();
    }
}
