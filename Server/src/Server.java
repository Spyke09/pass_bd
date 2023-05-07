import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket socket;

    Server() {
        try {
            // Порт, на котором работает сервер
            int PORT = 9999;
            socket = new ServerSocket(PORT);
            System.out.println("Server started on port: " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Здесь сервер начинает свою работу.
     * Создает для каждого нового клиента отдельный обработчик в новом потоке.
     */
    public void run() {
        while (true) {
            try {
                Socket s = socket.accept();
                ClientHandler client = new ClientHandler(s);

                new Thread(client).start();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}