package server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();

    public static void addClient(String username, PrintWriter out) {
        activeClients.put(username, out);
        System.out.println("[SERVER] Active client added: " + username);
    }

    public static void removeClient(String username) {
        activeClients.remove(username);
        System.out.println("[SERVER] Active client removed: " + username);
    }

    public static void sendToClient(String username, String message) {
        PrintWriter out = activeClients.get(username);
        if (out != null) {
            out.println(message);
            out.flush();
            System.out.println("[SERVER] Push sent to " + username + ": " + message);
        } else {
            System.out.println("[SERVER] User not online, push skipped for: " + username);
        }
    }

    public static boolean isOnline(String username) {
        return activeClients.containsKey(username);
    }
}

