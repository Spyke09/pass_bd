import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    Encryption enc;

    private PublicKey serverPublicKey;
    private final PublicKey selfPublicKey;
    private final PrivateKey selfPrivateKey;

    private ObjectOutputStream writer;
    private ObjectInputStream reader;

    Scanner sc;

    public Client() {
        this.sc = new Scanner(System.in);

        this.enc = new Encryption();
        this.selfPublicKey = enc.getPublicKey();
        this.selfPrivateKey = enc.getPrivateKey();

        try {
            Socket socket = new Socket(
                    "localhost",
                    9999
            );

            this.reader = new ObjectInputStream(socket.getInputStream());
            this.writer = new ObjectOutputStream(socket.getOutputStream());

        } catch (UnknownHostException e) {
            System.err.println("The server is unavailable.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Стартовая точка работы клиента.
     */
    public void run() throws Exception {
        // Обмен публичными ключами с сервером
        try {
            exchangeKeys();
        } catch (Exception e) {
            System.err.println("Public keys cannot be exchanged.");
            e.printStackTrace();
        }
        // Авторизация на сервере
        authorize();
    }

    /**
     * Проверяет ответ от сервера.
     */
    private boolean checkResponse() {
        PackageType responseType = receivePackage().getType();
        return responseType.equals(PackageType.SERVICE_ACCEPT);
    }

    /**
     * Принимает и расшифровывает пакет.
     */
    private Package receivePackage() {
        try {
            SendingPackage p = (SendingPackage) reader.readObject();
            return enc.decrypt(p.getData(), selfPrivateKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PackageError();
    }

    /**
     * Шифрует пакет и отправляет серверу.
     * @param pac - пакет
     */
    private void sendPackage(Package pac) throws Exception {
        byte[] sendingPackage = enc.encrypt(pac, serverPublicKey);

        SendingPackage p = new SendingPackage(sendingPackage);
        writer.writeObject(p);
        writer.flush();
    }

    /**
     * Обмен ключами с сервером
     * Сначала клиент отправляет свой ключ, затем принимает ключ сервера
     */
    private void exchangeKeys() throws IOException, ClassNotFoundException {
        writer.writeObject(selfPublicKey);
        writer.flush();

        serverPublicKey = (PublicKey) reader.readObject();
    }

    /**
     * Авторизация в приложении, либо регистрация, либо вход в уже существующий аккаунт.
     */
    private void authorize() throws Exception {
        int choice;

        System.out.println("Select an action:");
        System.out.println("1. Authorization");
        System.out.println("2. Registration");

        System.out.print("\n>: ");
        choice = sc.nextInt();

        switch (choice) {
            case 1:
                authorization();
                break;

            case 2:
                try {
                    registration();
                } catch (Exception e) {
                    System.out.println("An error occurred during registration!");
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("There is no such action.");
        }
    }

    /**
     * Также запускает mainLoop()
     * Регистрация нового аккаунта.
     */
    private void registration() throws Exception {
        String login;
        String password;
        String phoneNumber;
        String email;

        System.out.print("Enter your login: ");
        login = sc.next();

        System.out.print("Enter the password: ");
        password = sc.next();

        System.out.print("Enter your phone number: ");
        phoneNumber = sc.next();

        System.out.print("Enter your email address: ");
        email = sc.next();

        RegistrationPackage rgp = new RegistrationPackage(
                login,
                password,
                phoneNumber,
                email
        );

        sendPackage(rgp);

        if (checkResponse()) {
            System.out.println("Registration was successful!");
            mainLoop();
        } else {
            System.err.println("Error during registration.");
        }
    }

    /**
     * Авторизация уже существующего аккаунта
     * Также запускает mainLoop()
     */
    private void authorization() throws Exception {
        String login;
        String password;

        System.out.print("Enter your username: ");
        login = sc.next();

        System.out.print("Enter the password: ");
        password = sc.next();

        AuthorizationPackage pack = new AuthorizationPackage(login, password);

        try {
            sendPackage(pack);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkResponse()) {
            System.out.println("Welcome!");
            mainLoop();
        } else {
            System.err.println("Something went wrong.");
            System.exit(1);
        }
    }

    private void mainLoop() throws Exception {
        while (true) {
            int choice;

            System.out.println("\nAction:");
            System.out.println("1. Get the all database");
            System.out.println("2. Add an entry");
            System.out.println("3. Delete an entry");
            System.out.println("4. Edit entry");
            System.out.println("5. Get entry");
            System.out.println("6. Stop");

            System.out.print(">: ");
            choice = sc.nextInt();

            if (choice == 6) {
                break;
            }

            switch (choice) {
                case 1 -> getFullDataBase();
                case 2 -> addAuthorizeData();
                case 3 -> delAuthorizeData();
                case 4 -> updatePassword();
                case 5 -> getAuthorizeData();
                default -> throw new Exception("Invalid number");
            }
        }
    }

    /**
     * Добавление записи в базу данных с паролями пользователя.
     * Если введенная строка-пароль == "_", то генерируется случайный.
     */
    private void addAuthorizeData() {
        String url;
        String login;
        String password;

        System.out.println("Enter url: ");
        url = sc.next();

        System.out.println("Enter login: ");
        login = sc.next();

        System.out.println("Enter password: ");
        password = sc.next();

        if (password.equals("_")) {
            password = RandomPasswordGenerator.genPass();
        }

        DataPackage pack = new DataPackage(
                url,
                login,
                password,
                PackageType.ADD_AUTHORIZE_DATA
        );

        try {
            sendPackage(pack);

            if (checkResponse()) {
                System.out.println("Added!");
            } else {
                System.err.println("Some mistake.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление записи из базы данных с паролями пользователя
     */
    private void delAuthorizeData() {
        String del_url;

        System.out.println("Enter url: ");
        del_url = sc.next();

        DataPackage pack = new DataPackage(del_url, PackageType.DELETE_AUTHORIZE_DATA);

        try {
            sendPackage(pack);

            if (checkResponse()) {
                System.out.println("Done!");
            } else {
                System.err.println("Some mistake.");
            }
        } catch (Exception e) {
            System.err.println("Some mistake.");
        }
    }

    private void getFullDataBase() {
        DataPackage pack = new DataPackage(PackageType.GET_FULL_DATA_BASE);

        try {
            sendPackage(pack);
            Thread.sleep(1500);
        } catch (Exception e) {
            System.err.println("Some mistake.");
            e.printStackTrace();
        }

        DataPackage dataBase = (DataPackage) receivePackage();
        ArrayList<String> urls = dataBase.getObject();

        System.out.println("\nAll entries: ");
        for (String url : urls) {
            System.out.println(url);
        }
        System.out.println();
    }

    private void updatePassword() {
        String url;
        String password;

        System.out.println("Enter url");
        url = sc.next();

        System.out.println("Enter password");
        password = sc.next();

        if (password.equals("_")) {
            password = RandomPasswordGenerator.genPass();
        }

        DataPackage pack = new DataPackage(url, password, PackageType.MODIFY_AUTHORIZE_DATA);

        try {
            sendPackage(pack);

            if (checkResponse()) {
                System.out.println("Done!");
            } else {
                System.err.println("Some mistake.");
            }
        } catch (Exception e) {
            System.err.println("Some mistake.");
        }
    }

    private void getAuthorizeData() {
        System.out.print("Enter url: ");
        String url = sc.next();

        try {
            sendPackage(new DataPackage(
                    url,
                    PackageType.GET_AUTHORIZE_DATA));

            DataPackage pack = (DataPackage) receivePackage();

            ArrayList<String> data = pack.getObject();

            for (String s : data) {
                System.out.println(s);
            }

        } catch (Exception e) {
            System.err.println("Some mistake.");
        }
    }
}