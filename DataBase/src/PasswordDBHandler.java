import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Обработчик баз данных, отвечающих за хранения паролей пользователей на разных сайтах.
 */
public class PasswordDBHandler {
    private Connection connection;

    PasswordDBHandler(String login) {
        String path = "DataBase/users_db/" + login + ".db";

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);

            // Запрос, создающий таблицу data с полями [url, login, password, last_update_time]
            String request = """
                     CREATE TABLE IF NOT EXISTS data (
                      url TEXT NOT NULL,
                     login TEXT NOT NULL,
                     password TEXT NOT NULL,
                     last_update_time TEXT NOT NULL
                    );""";


            Statement statement = connection.createStatement();
            statement.executeUpdate(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Добавление записи в базу данных с паролями пользователя.
     */
    public void addRow(String url, String login, String password) throws SQLException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        java.util.Date date = new Date(System.currentTimeMillis());
        String format_date = formatter.format(date); // Уже отформатированное время и дата

        String responce = "INSERT INTO data (url, login, password, last_update_time) " +
                "VALUES ('" + url + "', '" + login + "', '" + password + "', '" + format_date + "')";

        Statement statement = connection.createStatement();
        statement.executeUpdate(responce);
    }

    /**
     * Удаление записи из базы данных с паролями пользователя.
     */
    public void deleteRow(String url) throws SQLException {
        String request = "DELETE FROM data WHERE url = ?;";

        PreparedStatement statement = connection.prepareStatement(request);
        statement.setString(1, url);
        statement.executeUpdate();
    }

    /**
     * Обновление пароля для данного url.
     * @param password - новый пароль.
     */
    public void updatePassword(String url, String password) throws SQLException {
        String request = "UPDATE data SET password = '" + password + "' WHERE url = ?;";

        PreparedStatement statement = connection.prepareStatement(request);
        statement.setString(1, url);
        statement.executeUpdate();
    }

    /**
     * Выдает список (url, login, password) пользователя по данному url.
     */
    public ArrayList<String> getAuthorizeData(String url) throws SQLException {
        String request = "SELECT * FROM data WHERE url = '" + url + "';";

        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(request);

        ArrayList<String> data = new ArrayList<>();
        while(result.next()){
            String url_ = result.getString(1);
            String login_ = result.getString(2);
            String password_ = result.getString(3);
            data.add("(" + url_ + ", " + login_ + ", " + password_ + ")");
        }


        return data;
    }

    /**
     * Считывает и возвращает всю базу данных пользователя.
     */
    public ArrayList<String> getFullDataBase() throws SQLException {
        String request = "SELECT * FROM data";

        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(request);

        ArrayList<String> urls = new ArrayList<>();

        while (result.next()) {
            String url_ = result.getString(1);
            String last_update_ = result.getString(4);
            urls.add(url_ + "\t(Last update: " + last_update_ + ")");
        }

        return urls;
    }
}
