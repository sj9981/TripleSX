package server;

public class TestConnection
{
    public static void main(String[] args)
    {
        boolean success = DatabaseManager.registerUser(
                "ali_dev",
                "ali@example.com",
                "hashed_password_123",
                "Ali Developer",
                "at work",
                "/uploads/avatars/ali.jpg",
                "/uploads/banners/ali.jpg"
        );


        if (success)
        {
            System.out.println(" کاربر با موفقیت در دیتابیس ثبت شد!");
        }
        else
        {
            System.out.println(" خطا در ثبت.");
        }
    }
}



