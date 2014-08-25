package FirstExercise;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws IOException {
        System.out.println("Welcome to use SocketService!");
        SocketService service = new SocketService("localhost", 5000, 2000);
        // 包装系统标准输入
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Please input command to start/stop SocketService or exit program(start/stop/exit):");
        boolean bRun = true;
        while (bRun) {
            String line = reader.readLine(); // 读取输入
            if (line == null || line.length() == 0) continue; // 输入有误则继续下一循环，否则处理输入
            switch (line) {
                case "start":
                    service.start();
                    break;
                case "stop":
                    service.stop();
                    break;
                case "exit":
                    service.stop();
                    bRun = false;
                    break;
                default:
                    System.out.println("Input error!!");
            }
        }

        System.out.println("Exit Success!");
    }
}

