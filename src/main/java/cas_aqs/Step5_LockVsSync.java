package cas_aqs;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验五：从 synchronized 迁移到 ReentrantLock
 *
 * 先用 synchronized 实现一个计数器，再用 ReentrantLock 改写
 * 功能上等价，但 ReentrantLock 多了哪些能力？
 *
 * 【跑一下，确认两种写法结果一致】
 * 思考：
 *   1. ReentrantLock 必须手动 unlock，忘了会怎样？
 *   2. 为什么 unlock 要放在 finally 里？
 *   3. 既然功能一样，ReentrantLock 存在的意义是什么？（看 Step6、Step7）
 */
public class Step5_LockVsSync {

    static int counterA = 0; // 用 synchronized 保护
    static int counterB = 0; // 用 ReentrantLock 保护
    static final Object lockObj = new Object();
    static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];

        // synchronized 版本
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    synchronized (lockObj) {
                        counterA++;
                    }
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // ReentrantLock 版本
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    lock.lock();
                    try {
                        counterB++;
                    } finally {
                        lock.unlock(); // 思考：如果不写 finally，业务异常时会怎样？
                    }
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("synchronized 结果：" + counterA);
        System.out.println("ReentrantLock  结果：" + counterB);
        System.out.println("两者一致：" + (counterA == counterB));

        // 结果一样，但 ReentrantLock 可以做 synchronized 做不到的事
        // 去看 Step6（公平性）和 Step7（Condition）
    }
}
