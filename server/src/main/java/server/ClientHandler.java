package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{

    private final Socket socket;
    private final RequestProcessor requestProcessor;

    public ClientHandler(Socket socket)
    {
        this.socket = socket;
        this.requestProcessor = new RequestProcessor();
    }

    @Override
    public void run()
    {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
        ) {
            String message;

            while ((message = in.readLine()) != null)
            {
                System.out.println("[DEBUG] Received raw message: " + message);

                String responseMessage = requestProcessor.process(message);

                out.println(responseMessage);
            }

        } catch (IOException e)
        {
            System.err.println("[INFO] Client connection closed: " + socket.getRemoteSocketAddress() + " (" + e.getMessage() + ")");
        }
        finally
        {
            try
            {
                if (socket != null && !socket.isClosed())
                {
                    socket.close();
                }
                System.out.println("[INFO] Socket clean up finished for: " + socket.getRemoteSocketAddress());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}