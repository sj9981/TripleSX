package client;

import org.json.JSONObject;
import java.util.Scanner;

public class MainClient
{
    public static void main(String[] args)
    {
        NetworkManager networkManager = NetworkManager.getInstance();
        Scanner scanner = new Scanner(System.in);

        try
        {
            networkManager.connect();
            System.out.println("--- Welcome to X-Clone Terminal ---");

            while (true)
            {
                System.out.println("\nChoose action: 1. Register  2. Login  3. Exit");
                String choice = scanner.nextLine();

                if (choice.equals("3")) break;

                JSONObject request = new JSONObject();
                if (choice.equals("1"))
                {
                    request.put("action", "register");
                    System.out.print("Username: "); request.put("username", scanner.nextLine());
                    System.out.print("Email: ");    request.put("email", scanner.nextLine());
                    System.out.print("Password: "); request.put("password", scanner.nextLine());
                    request.put("bio", "Hello, I'm new here!");
                    request.put("avatar", "default.png");
                    request.put("banner", "default_banner.png");

                } else if (choice.equals("2"))
                {
                    request.put("action", "login");
                    System.out.print("Username: "); request.put("username", scanner.nextLine());
                    System.out.print("Password: "); request.put("password", scanner.nextLine());
                }

                String responseJson = networkManager.sendRequest(request.toString());
                JSONObject response = new JSONObject(responseJson);

                System.out.println("\n[SERVER RESPONSE]: " + response.getString("message"));
                System.out.println("Status: " + (response.getBoolean("success") ? "SUCCESS" : "FAILED"));
            }

        }
        catch (Exception e)
        {
            System.err.println("Client Error: " + e.getMessage());
        }
        finally
        {
            networkManager.disconnect();
        }
    }
}