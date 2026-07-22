package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager
{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    private static NetworkManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private NetworkManager() {}

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public synchronized void connect() throws IOException
    {
        if (connected && socket != null && !socket.isClosed()) {
            return;
        }
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        System.out.println("[CLIENT] Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
    }

    public synchronized String sendRequest(String jsonRequest)
    {
        try
        {
            if (!connected || socket == null || socket.isClosed()) {
                connect();
            }
            out.println(jsonRequest);
            return in.readLine();
        }
        catch (IOException e)
        {
            connected = false;
            return "{\"success\": false, \"message\": \"Connection lost: " + e.getMessage() + "\"}";
        }
    }

    public synchronized void disconnect() {
        try {
            if (socket != null) socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally {
            connected = false;
        }
    }
}