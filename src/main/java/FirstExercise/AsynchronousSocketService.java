package FirstExercise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by LxG on 2014/8/26.
 */
public class AsynchronousSocketService extends ServiceBase {

    private final String host;
    private final int port;
    private AsynchronousServerSocketChannel asynchronousServerSocketChannel;

    ConcurrentHashMap<String, AsynchronousSocketClient> clientMap;

    public AsynchronousSocketService(String host, int port) {
        this.host = host;
        this.port = port;

        clientMap = new ConcurrentHashMap<>();
    }

    public void start() {
        try {
            SocketAddress localAddress = new InetSocketAddress(host, port);
            asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
            asynchronousServerSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            asynchronousServerSocketChannel.bind(localAddress, 100);
            asynchronousServerSocketChannel.accept(null, new AcceptCompletionHandler());

            super.start();
            System.out.println("Start AsynchronousSocketService " + localAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (asynchronousServerSocketChannel != null) {
                asynchronousServerSocketChannel.close();
                asynchronousServerSocketChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        clientMap.forEach((x, y) -> {
            try {
                if (y != null) y.getAsynchronousSocketChannel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        clientMap.clear();

        System.out.println("Stop AsynchronousSocketService");
    }

    class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            try {
                AsynchronousSocketClient client = new AsynchronousSocketClient(
                        result.getRemoteAddress().toString(), result);
                clientMap.put(client.getAddress(), client);
                result.read(client.getByteBuffer(), client, new ReadCompletionHandler());
                System.out.println("Accept Client\t\t" + client.getAddress());

                asynchronousServerSocketChannel.accept(null, new AcceptCompletionHandler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("Accept Exception");
        }
    }

    class ReadCompletionHandler implements CompletionHandler<Integer, Object> {

        @Override
        public void completed(Integer result, Object attachment) {
            AsynchronousSocketClient client = (AsynchronousSocketClient) attachment;
            AsynchronousSocketChannel channel = client.getAsynchronousSocketChannel();
            ByteBuffer byteBuffer = client.getByteBuffer();

            if (result <= 0) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Close Client " + client.getAddress());
                clientMap.remove(client.getAddress());
                return;
            }

            System.out.printf("Read\t%d\tfrom\t%s\n", result, client.getAddress());

            byteBuffer.flip();
            channel.write(byteBuffer, client, new WriteCompletionHandler());

            byteBuffer.clear();
            channel.read(byteBuffer, client, new ReadCompletionHandler());
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            AsynchronousSocketClient client = (AsynchronousSocketClient) attachment;
            System.out.printf("Read Exception %s\n", client.getAddress());
        }
    }

    class WriteCompletionHandler implements CompletionHandler<Integer, Object> {

        @Override
        public void completed(Integer result, Object attachment) {
            AsynchronousSocketClient client = (AsynchronousSocketClient) attachment;
            System.out.printf("Write\t%d\tto\t\t%s\n", result, client.getAddress());
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            AsynchronousSocketClient client = (AsynchronousSocketClient) attachment;
            System.out.printf("Write Exception %s\n", client.getAddress());
            exc.printStackTrace();
        }
    }
}