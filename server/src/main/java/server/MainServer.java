package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServer
{

    private static final int PORT = 5000;

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args)
    {
        System.out.println("=========================================");
        System.out.println("    Starting AP404 Twitter-Clone Server  ");
        System.out.println("=========================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            System.out.println("Server successfully started on port: " + PORT);
            System.out.println("Waiting for clients to connect...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[INFO] New client connected from: " + clientSocket.getRemoteSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }

        }
        catch (IOException e)
        {
            System.err.println("[ERROR] Server encountered an exception: " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            threadPool.shutdown();
            System.out.println("Server shutdown complete.");
        }
    }
}