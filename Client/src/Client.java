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
    private final int PORT = 9999;

    Encryption enc;

    private PublicKey serverPublicKey;
    private final PublicKey selfPublicKey;
    private final PrivateKey selfPrivateKey;

    private Socket socket;
    private ObjectOutputStream writer;
    private ObjectInputStream reader;

    Scanner sc;

    public Client() {
        this.sc = new Scanner(System.in);

        this.enc = new Encryption();
        this.selfPublicKey = enc.getPublicKey();
        this.selfPrivateKey = enc.getPrivateKey();

        try {
            this.socket = new Socket(
                    "localhost",
                    PORT
            );

            this.reader = new ObjectInputStream(socket.getInputStream());
            this.writer = new ObjectOutputStream(socket.getOutputStream());

        } catch (UnknownHostException e) {
            System.err.println("Сервер недоступен.");
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
            System.err.println("Не удается обменяться открытыми ключами.");
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

        System.out.println("ВЫберите действие: ");
        System.out.println("1. Авторизация");
        System.out.println("2. Регистрация");

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
                    System.out.println("При регистрации произошла ошибка!");
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("Нет такого действия");
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

        System.out.print("Введите логин: ");
        login = sc.next();

        System.out.print("Введите пароль: ");
        password = sc.next();

        System.out.print("Введите номер телефона: ");
        phoneNumber = sc.next();

        System.out.print("Введите электронную почту: ");
        email = sc.next();

        RegistrationPackage rgp = new RegistrationPackage(
                login,
                password,
                phoneNumber,
                email
        );

        sendPackage(rgp);

        if (checkResponse()) {
            System.out.println("Регистрация прошла успешно!");
            mainLoop();
        } else {
            System.err.println("Ошибка при регистрации");
        }
    }

    /**
     * Авторизация уже существующего аккаунта
     * Также запускает mainLoop()
     */
    private void authorization() throws Exception {
        String login;
        String password;

        System.out.print("Введите логин: ");
        login = sc.next();

        System.out.print("Введите пароль: ");
        password = sc.next();

        AuthorizationPackage pack = new AuthorizationPackage(login, password);

        try {
            sendPackage(pack);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkResponse()) {
            System.out.println("Все крута. Добро пожаловать!");
            mainLoop();
        } else {
            System.err.println("Что-то пошло не так");
            System.exit(1);
        }
    }

    private void mainLoop() throws Exception {
        while (true) {
            int choice;

            System.out.println("\nЧто сделать: ");
            System.out.println("1. Получить всю базу данных");
            System.out.println("2. Добавить запись");
            System.out.println("3. Удалить запись");
            System.out.println("4. Изменть запись");
            System.out.println("5. Получить запись");
            System.out.println("6. Остановить");

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
                default -> throw new Exception("Некорректное число");
            }
        }
    }

    /**
     * Добавдение записи в базу данных с паролями пользователя.
     * Если введенная строка-пароль == "_", то генерируется случайный.
     */
    private void addAuthorizeData() {
        String url;
        String login;
        String password;

        System.out.println("Введите url: ");
        url = sc.next();

        System.out.println("Введите логин: ");
        login = sc.next();

        System.out.println("Введите пароль: ");
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
                System.out.println("Добавлено!");
            } else {
                System.err.println("Какая-то ошибка");
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

        System.out.println("Введите url: ");
        del_url = sc.next();

        DataPackage pack = new DataPackage(del_url, PackageType.DELETE_AUTHORIZE_DATA);

        try {
            sendPackage(pack);

            if (checkResponse()) {
                System.out.println("Сделано!");
            } else {
                System.err.println("Что-то пошло не так");
            }
        } catch (Exception e) {
            System.err.println("Что-то пошло не так");
        }
    }

    private void getFullDataBase() {
        DataPackage pack = new DataPackage(PackageType.GET_FULL_DATA_BASE);

        try {
            sendPackage(pack);
            Thread.sleep(1500);
        } catch (Exception e) {
            System.err.println("Что-то пошло не так");
            e.printStackTrace();
        }

        DataPackage dataBase = (DataPackage) receivePackage();
        ArrayList<String> urls = (ArrayList<String>) dataBase.getObject();

        System.out.println("\n\nВсе записи: ");
        for (String url : urls) {
            System.out.println(url);
        }
        System.out.println("\n\n");
    }

    private void updatePassword() {
        String url;
        String password;

        System.out.println("Введите url");
        url = sc.next();

        System.out.println("Введите пароль");
        password = sc.next();

        if (password.equals("_")) {
            password = RandomPasswordGenerator.genPass();
        }

        DataPackage pack = new DataPackage(url, password, PackageType.MODIFY_AUTHORIZE_DATA);

        try {
            sendPackage(pack);

            if (checkResponse()) {
                System.out.println("Сделано!");
            } else {
                System.err.println("Что-то пошло не так");
            }
        } catch (Exception e) {
            System.err.println("Что-то пошло не так");
        }
    }

    private void getAuthorizeData() {
        System.out.print("Введите url: ");
        String url = sc.next();

        try {
            sendPackage(new DataPackage(
                    url,
                    PackageType.GET_AUTHORIZE_DATA));

            DataPackage pack = (DataPackage) receivePackage();

            ArrayList<String> data = (ArrayList<String>) pack.getObject();

            for (String s : data) {
                System.out.println(s);
            }

        } catch (Exception e) {
            System.err.println("Что-то пошло не так");
        }
    }
}