package FirstExercise;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

public class SocketService extends ServiceBase {

    private final String host;
    private final int port;
    private final int maxConnections;
    private ServerSocket serverSocket;
    ConcurrentHashMap<String, SocketClient> listClientSocket;
    private Thread acceptThread;

    public SocketService(String host, int port, int maxConnections) throws IOException {
        this.host = host;
        this.port = port;
        this.maxConnections = maxConnections;
        // 初始化map桶为最大连接数的二倍，使负载值最大达到0.5
        this.listClientSocket = new ConcurrentHashMap<>(maxConnections * 2 + 1);
        acceptThread = new Thread(new AcceptThread());
    }

    @Override
    public void start() {
        try {
            // 来不及响应的，积压的连接请求数量设置为1000
            serverSocket = new ServerSocket(port, 1000, InetAddress.getByName(host));

            // 父类的start放在这里，防止上面的处理出现异常，为下面提供运行标志
            super.start();
            acceptThread.start();
            System.out.println("SocketService has been started!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (!isRunning()) return;

        // 父类的stop放在最前面，如果其他线程检查运行标志停止就会
        super.stop();

        // 关闭监听套接字，如果正在监听中，会引起监听线程异常，然后退出
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 如果正在等待读取数据，关闭客户端套接字，客户数据读取线程就会退出
        listClientSocket.forEach((x, y) -> {
            try {
                if (y != null) y.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(x + " socket has closed manually!");
        });
        listClientSocket.clear();
        serverSocket = null;

        // 等待线程都退出
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Service has stopped!");
    }

    class AcceptThread implements Runnable {
        @Override
        public void run() {
            while (isRunning()) {
                try {
                    if (listClientSocket.mappingCount() < maxConnections) {
                        Socket socket = serverSocket.accept();
                        SocketClient client = new SocketClient(socket.getRemoteSocketAddress().toString(), socket);
                        listClientSocket.put(client.address, client);
                        new Thread(new ReceiveThread(client)).start();
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (SocketException e) {
                    System.err.println("AcceptThread SocketException Message:" + e.getMessage());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("AcceptThread Exit!");
        }
    }

    /*
    * 接收数据线程，接收数据并打印到控制台
    * */
    class ReceiveThread implements Runnable {

        private SocketClient client;

        public ReceiveThread(SocketClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            System.out.println(client.address + " ReceiveThread has started!");
            final int BufferSize = 2048;
            byte[] buffer = new byte[BufferSize];
            while (isRunning()) {
                try {
                    InputStream stream = client.socket.getInputStream();
                    int count = stream.read(buffer, 0, BufferSize);
                    if (count <= 0) break;
                    client.socket.getOutputStream().write(buffer, 0, count);
                } catch (IOException e) {
                    System.out.printf("ReceiveThread Exception %s %s\n", client.address, e.getMessage());
                }
            }

            try {
                if (client.socket != null) client.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            listClientSocket.remove(client.address);
            System.out.println(client.address + " ReceiveThread has exit!");
        }
    }
}
