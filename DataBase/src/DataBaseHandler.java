import java.sql.*;

/**
 * Обработчик для главной базы данных с пользователями.
 * Добавляет или удаляет авторизационные данные из базы пользователей.
 */
public class DataBaseHandler {
    private Connection co;

    DataBaseHandler() {
        String pathDbFile = "DataBase/users.db";

        try {
            Class.forName("org.sqlite.JDBC");
            co = DriverManager.getConnection("jdbc:sqlite:" + pathDbFile);

            // Запрос, создающий таблицу users с полями [login, password, phone_number, email]
            String request = """
                     CREATE TABLE IF NOT EXISTS users (
                      login TEXT UNIQUE NOT NULL,
                     password TEXT NOT NULL,
                     phone_number TEXT,
                     email TEXT
                    );""";


            Statement statement = co.createStatement();
            statement.execute(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Функция добавления строки в базу.
     */
    public synchronized void addUser(String login,
                                     String password,
                                     String phoneNumber,
                                     String email
    ) throws SQLException {
        String response = "INSERT INTO users (login, password, phone_number, email) " +
                "VALUES('" + login + "', '" + password + "', '" + phoneNumber + "', '" + email + "')";

        Statement statement = co.createStatement();
        statement.executeUpdate(response);
    }

    /**
     * Проверка юзера на существование по логину и паролю.
     */
    public synchronized boolean checkUser(String login, String password) throws SQLException {
        String request = "SELECT login, password FROM users WHERE login = ?;')";

        PreparedStatement ps = co.prepareStatement(request);
        ps.setString(1, login);

        ResultSet result = ps.executeQuery();

        String response_login = result.getString("login");
        String response_password = result.getString("password");

        try {
            return response_login.equals(login) && response_password.equals(password);
        } catch (NullPointerException e) {
            throw new NullPointerException();
        }
    }
}