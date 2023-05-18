import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private final Encryption enc;

    private PublicKey userPublicKey;
    private final PublicKey selfPublicKey;
    private final PrivateKey selfPrivateKey;

    private ObjectOutputStream objectWriter;
    private ObjectInputStream objectReader;

    private final DataBaseHandler dbHandler;

    private String login;

    private boolean isAuthorized;

    public ClientHandler(Socket socket) {
        System.out.println("Client has been connected!");

        isAuthorized = false;

        dbHandler = new DataBaseHandler();

        enc = new Encryption();

        this.selfPublicKey = enc.getPublicKey();
        this.selfPrivateKey = enc.getPrivateKey();

        try {
            this.objectWriter = new ObjectOutputStream(socket.getOutputStream());
            this.objectReader = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        exchangeKeys();
        controller();
    }

    /**
     * Обмен ключами с клиентом.
     * Сначала принимает сервер клиентский ключ, затем отправляет клиенту свой
     */
    private void exchangeKeys() {


        try {
            userPublicKey = (PublicKey) objectReader.readObject();

            objectWriter.writeObject(selfPublicKey);
            objectWriter.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает принимаемые пакеты
     */
    private void controller() {
        boolean stop_q = false;
        while (!stop_q) {
            Package pack = receivePackage();
            PackageType type = pack.getType();

            switch (type) {
                case REGISTRATION:
                    RegistrationPackage rgp = (RegistrationPackage) pack;
                    System.out.println(rgp);
                    registration(rgp);
                    break;

                case AUTHORIZATION:
                    AuthorizationPackage autPack = (AuthorizationPackage) pack;
                    System.out.println(pack);
                    authorization(autPack);
                    break;

                case ADD_AUTHORIZE_DATA:
                    if (isAuthorized) {
                        DataPackage aadp = (DataPackage) pack;
                        System.out.println(aadp);

                        addAuthorizationData(
                                aadp.getUrl(),
                                aadp.getLogin(),
                                aadp.getPassword()
                        );
                    }
                    break;

                case DELETE_AUTHORIZE_DATA:
                    if (isAuthorized) {
                        DataPackage dadp = (DataPackage) pack;
                        System.out.println(dadp);

                        delAuthorizeData(dadp.getUrl());
                    }
                    break;

                case MODIFY_AUTHORIZE_DATA:
                    if (isAuthorized) {
                        DataPackage dadp = (DataPackage) pack;
                        System.out.println(dadp);

                        updatePassword(dadp.getUrl(), dadp.getPassword());
                    }
                    break;

                case GET_AUTHORIZE_DATA:
                    if (isAuthorized) {
                        DataPackage data_p = (DataPackage) pack;
                        System.out.println(data_p);

                        sendAuthorizeData(data_p.getUrl());
                    }
                    break;

                case GET_FULL_DATA_BASE:
                    if (isAuthorized) {
                        sendAllDataBase();
                    }

                default:
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        stop_q = true;
                    }
            }
        }
    }

    /**
     * Принимает и расшифровывает пакет
     */
    private Package receivePackage() {


        try {
            SendingPackage p = (SendingPackage) objectReader.readObject();

            return enc.decrypt(p.getData(), selfPrivateKey);

        } catch (SocketException | EOFException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PackageAccept();
    }


    /**
     * Шифрует пакет и потправляет пользователю
     */
    private void sendPackage(Package pac) {
        try {
            byte[] sendPack = enc.encrypt(pac, userPublicKey);
            SendingPackage p = new SendingPackage(sendPack);

            objectWriter.writeObject(p);
            objectWriter.flush();

        } catch (Exception e) {
            System.err.println("Cannot to send package to user");
            e.printStackTrace();
        }
    }


    /**
     * Регистрирует пользователя.
     * Записывает логин и хеш пароля в базу со всеми пользователями.
     */
    private void registration(RegistrationPackage rgp) {
        try {
            dbHandler.addUser(
                    rgp.getLogin(),
                    enc.makeHash(rgp.getPassword()),
                    rgp.getPhoneNumber(),
                    rgp.getEmail()
            );
            this.login = rgp.getLogin();

            sendPackage(new PackageAccept());
            isAuthorized = true;

        } catch (SQLException | NoSuchAlgorithmException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }


    /**
     * Проверяет наличие пользователя в базе.
     * Если пользователь есть, то сравнивает хеши паролей и принимает решение давать доступ или нет.
     */
    private void authorization(AuthorizationPackage pack) {
        try {
            String hashedPassword = enc.makeHash(pack.getPassword());

            if (dbHandler.checkUser(pack.getLogin(), hashedPassword)) {
                sendPackage(new PackageAccept());
                this.login = pack.getLogin();
                isAuthorized = true;
            } else {
                sendPackage(new PackageError());
            }
        } catch (SQLException | NoSuchAlgorithmException | NullPointerException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }


    /**
     * Добавление записи в базу данных с паролями пользователя
     */
    private void addAuthorizationData(String url, String login, String password) {
        PasswordDBHandler dbHand = new PasswordDBHandler(this.login);

        try {
            dbHand.addRow(
                    url,
                    login,
                    password
            );
            sendPackage(new PackageAccept());

        } catch (SQLException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }


    /**
     * Удаление записи из базы данных с паролями пользователя
     */
    private void delAuthorizeData(String url) {
        PasswordDBHandler dbHand = new PasswordDBHandler(this.login);

        try {
            dbHand.deleteRow(url);
            sendPackage(new PackageAccept());
        } catch (SQLException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }


    /**
     * Обновить пароль по url на новый password
     * @param password - новый пароль
     */
    private void updatePassword(String url, String password) {
        PasswordDBHandler dbHand = new PasswordDBHandler(this.login);

        try {
            dbHand.updatePassword(url, password);
            sendPackage(new PackageAccept());
        } catch (SQLException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }

    /**
     * Послать данные авторизации.
     */
    private void sendAuthorizeData(String url) {
        PasswordDBHandler dbHand = new PasswordDBHandler(this.login);

        try {
            ArrayList<String> data = dbHand.getAuthorizeData(url);

            sendPackage(new DataPackage(data, PackageType.GET_AUTHORIZE_DATA));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет всю базу данных пользователю
     */
    private void sendAllDataBase() {
        PasswordDBHandler dbHand = new PasswordDBHandler(this.login);

        try {
            ArrayList<String> urls = dbHand.getFullDataBase();

            DataPackage pack = new DataPackage(urls, PackageType.GET_FULL_DATA_BASE);

            sendPackage(pack);
        } catch (SQLException e) {
            sendPackage(new PackageError());
            e.printStackTrace();
        }
    }
}
