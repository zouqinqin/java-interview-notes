package jvm.threatLocal;

public class UserContext {

    // 每个线程有自己独立的 currentUser
    private static ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void set(String user) {
        currentUser.set(user);
    }

    public static String get() {
        return currentUser.get();
    }

    public static void remove() {
        currentUser.remove();
    }
}