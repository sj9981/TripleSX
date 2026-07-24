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
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public void connect() throws IOException
    {
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("[CLIENT] Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
    }

    public String sendRequest(String jsonRequest)
    {
        try
        {
            out.println(jsonRequest);
            return in.readLine();
        }
        catch (IOException e)
        {
            return "{\"success\": false, \"message\": \"Connection lost: " + e.getMessage() + "\"}";
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}