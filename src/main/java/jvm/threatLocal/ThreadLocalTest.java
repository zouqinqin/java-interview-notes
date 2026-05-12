package jvm.threatLocal;

public class ThreadLocalTest {
    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            UserContext.set("张三");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            // 1秒后读取，看是不是还是张三
            System.out.println("线程A 读到：" + UserContext.get());
            UserContext.remove();
        });

        Thread t2 = new Thread(() -> {
            UserContext.set("李四");
            System.out.println("线程B 读到：" + UserContext.get());
            UserContext.remove();
        });

        t1.start();
        t2.start();
    }
}