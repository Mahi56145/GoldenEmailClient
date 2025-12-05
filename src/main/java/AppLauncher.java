public class AppLauncher {
    public static void main(String[] args) {
        // THIS LINE IS CRITICAL FOR GMAIL SPEED
        System.setProperty("java.net.preferIPv4Stack", "true");

        EmailGUI.main(args);
    }
}