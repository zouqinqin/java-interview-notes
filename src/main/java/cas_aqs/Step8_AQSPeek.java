package cas_aqs;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验八：感受 AQS 队列的存在 —— 线程是怎么排队的？
 *
 * ReentrantLock 底层是 AQS，抢不到锁的线程会被放入一个等待队列
 * 这个实验让你能"看到"这个队列
 *
 * 【跑一下，观察每次打印的"等待线程数"变化】
 * 思考：
 *   1. 等待线程数是怎么变化的？和你预期的一样吗？
 *   2. getQueueLength() 返回的是什么？这些线程在哪里等待？
 *   3. 线程被 park（挂起）和 sleep 有什么本质区别？
 *   4. 锁释放后，AQS 怎么决定唤醒哪个线程？
 */
public class Step8_AQSPeek {

    static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {

        // 先让一个线程持有锁不放
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("[持有者] 拿到锁，持有 3 秒...");
                Thread.sleep(3000);
                System.out.println("[持有者] 释放锁");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "LockHolder");
        holder.start();
        Thread.sleep(200); // 确保 holder 先拿到锁

        // 再启动5个线程来抢锁
        for (int i = 0; i < 5; i++) {
            final int id = i;
            new Thread(() -> {
                System.out.println("  线程" + id + " 尝试获取锁...");
                lock.lock();
                try {
                    System.out.println("  线程" + id + " 获得锁");
                } finally {
                    lock.unlock();
                }
            }, "Waiter-" + i).start();
            Thread.sleep(100);
        }

        // 主线程观察等待队列长度
        for (int i = 0; i < 10; i++) {
            Thread.sleep(300);
            System.out.println(">>> 当前等待队列长度：" + lock.getQueueLength()
                + "，锁被持有：" + lock.isLocked());
        }

        // 观察完整过程后思考：
        // AQS 里的这个"队列"，就是那些被 park 住的线程的集合
        // 去翻 AbstractQueuedSynchronizer 源码，找 acquireQueued 方法
        // 看看线程进入队列之后是怎么自旋+park的
    }
}
