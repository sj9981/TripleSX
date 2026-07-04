package server;

public class Main
{
    public static void main(String[] args)
    {
        String testUser = "ali_test";
        String testPass = "mySecret123";

        boolean registered = DatabaseManager.registerUser(
                testUser, "ali@test.com", testPass, "Ali", "Hello world!", "avatar.png"
        );

        if (registered)
        {
            System.out.println(" کاربر با موفقیت ثبت‌نام شد.");
        }
        else
        {
            System.out.println(" ثبت‌نام ناموفق بود (شاید کاربر قبلاً وجود داشت).");
        }

        boolean loggedIn = DatabaseManager.loginUser(testUser, testPass);

        if (loggedIn)
        {
            System.out.println(" ورود موفقیت‌آمیز بود!");
        }
        else
        {
            System.out.println(" ورود ناموفق بود.");
        }
    }
}
