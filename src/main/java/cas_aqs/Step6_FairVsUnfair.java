package cas_aqs;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验六：公平锁 vs 非公平锁 —— 先来的一定先得到吗？
 *
 * 关键场景：线程A 释放锁后立刻再次抢锁
 *   - 此时队列里已经有 B、C、D 在等待
 *   - 非公平锁：A 可能直接插队再次拿到锁（不用排队）
 *   - 公平锁：A 必须排到队尾，B 先拿到
 *
 * 【分别跑两次，切换 FAIR = true/false，观察每轮是谁拿到锁】
 * 思考：
 *   1. 非公平锁下，同一个线程会连续多次拿到锁吗？
 *   2. 公平锁下，获得锁的顺序规律是什么？
 *   3. 非公平锁吞吐量更高，但有什么副作用？
 */
public class Step6_FairVsUnfair {

    static final boolean FAIR = true; // 改成 true 再跑一次，对比输出

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(FAIR);
        System.out.println("锁模式：" + (FAIR ? "公平锁" : "非公平锁"));
        System.out.println("-----------------------------");

        // 先让 B、C、D 都在队列里等待
        Thread[] waiters = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final String name = "等待线程-" + (char)('B' + i);
            waiters[i] = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    lock.lock();
                    try {
                        System.out.println("  " + name + " 获得锁");
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lock.unlock();
                    }
                }
            }, name);
        }

        // 线程A：反复释放再立刻抢锁，模拟"插队"行为
        Thread threadA = new Thread(() -> {
            for (int i = 0; i < 6; i++) {
                lock.lock();
                try {
                    System.out.println("线程-A 获得锁（第" + (i + 1) + "次）");
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                    // 释放后立刻再抢，不公平锁下可能直接插队
                }
            }
        }, "线程-A");

        // 先启动等待线程，让它们进入队列
        for (Thread t : waiters) t.start();
        Thread.sleep(20); // 确保 B、C、D 都已在等待队列中
        threadA.start();  // A 后来，反复释放再抢

        threadA.join();
        for (Thread t : waiters) t.join();

        // 非公平锁：观察 线程-A 是否连续多次拿到锁（插队）
        // 公平锁：  观察 线程-A 是否只能按顺序轮到自己
    }
}
