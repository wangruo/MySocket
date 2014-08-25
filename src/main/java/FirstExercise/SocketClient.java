package FirstExercise;

import java.net.Socket;

public class SocketClient {
    public Socket socket;
    public String address;

    public SocketClient(String address, Socket socket) {
        this.address = address;
        this.socket = socket;
    }
}
