package cas_aqs;

/**
 * 实验一：先不用任何锁，看看会发生什么
 *
 * 任务：10个线程，每个线程对 counter 自增 1000 次
 * 预期结果应该是多少？
 *
 * 【跑一下，观察输出】
 * 思考：为什么结果不是预期值？i++ 到底做了几件事？
 */
public class Step1_RaceCondition {

    static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter++;  // <-- 看起来只是一行代码，但真的是原子操作吗？
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("预期结果：10000");
        System.out.println("实际结果：" + counter);
        System.out.println("差了多少：" + (10000 - counter));

        // 多跑几次，结果一样吗？
        // 思考：i++ 在字节码层面是哪几步操作？哪个环节出了问题？
    }
}
