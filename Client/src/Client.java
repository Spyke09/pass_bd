import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

public class Client {
    private final int PORT = 9999;

    Encryption enc;

    private PublicKey serverPublicKey;
    private PublicKey selfPublicKey;
    private PrivateKey selfPrivateKey;

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

            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.reader = new ObjectInputStream(socket.getInputStream());

        } catch (UnknownHostException e) {
            System.err.println("Server is not avaliable");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void run() {
        /*
         * Стартовая точка работы клиента
         * */

        // Обмен публичными ключами с сервером
        try {
            exchangeKeys();
        } catch (Exception e) {
            System.err.println("Cannot echange public keys");
            e.printStackTrace();
        }

        // Авторизация на сервере
        authorize();
    }

    private boolean checkResponse() {
        PackageType responseType = recievePackage().getType();

        if (responseType.equals(PackageType.SERVICE_ACCEPT)) {
            return true;
        } else {
            return false;
        }
    }

    private Package recievePackage() {
        try {
            SendingPackage p = (SendingPackage) reader.readObject();
            Package pac = enc.decrypt(p.getData(), selfPrivateKey);

            return pac;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Package(PackageType.SERVICE_ERROR);
    }

    private void sendPackage(Package pac) throws Exception {
        byte[] sendingPackage = enc.encrypt(pac, serverPublicKey);

        SendingPackage p = new SendingPackage(sendingPackage);
        writer.writeObject(p);
        writer.flush();
    }

    private void exchangeKeys() throws IOException, ClassNotFoundException {
        /*
         * Обмен ключами с сервером
         *
         * Сначала клиент отправляет свой ключ,
         * затем принимает ключ сервера
         * */

        writer.writeObject(selfPublicKey);
        writer.flush();

        serverPublicKey = (PublicKey) reader.readObject();
        System.out.println("Client public key: " + serverPublicKey);
    }

    private void authorize() {
        /*
         * Авторизация в приложении
         *
         * Либо регистрация
         * Либо вход в уже сущесвтующий аккаунт
         * */

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

    private void registration() throws Exception {
        /*
         * Регистрация нового аккаунта
         * */

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
        } else {
            System.err.println("Ошибка при регистрации");
        }
    }

    private void authorization() {
        String login;
        String password;

        System.out.println("Введите логин: ");
        login = sc.next();

        System.out.println("Введите пароль: ");
        password = sc.next();

        AuthorizationPackage pack = new AuthorizationPackage(login, password);

        try {
            sendPackage(pack);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkResponse()) {
            System.out.println("Все крута, ты уже там есть");
        } else {
            System.err.println("Что-то пошло не так");
        }
    }
}