package cas_aqs;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 实验九：读多写少的场景，ReentrantLock 够用吗？
 *
 * 场景：一个缓存，5个读线程 + 1个写线程
 *   - 读操作：耗时 50ms（模拟查询）
 *   - 写操作：耗时 100ms（模拟更新）
 *
 * 问题：用 ReentrantLock，读和读之间也会互相阻塞
 *       但读操作根本不修改数据，它们阻塞对方有意义吗？
 *
 * 【先跑 withReentrantLock()，记下总耗时】
 * 【再跑 withReadWriteLock()，对比耗时差距】
 *
 * 思考：
 *   1. 两种方案耗时差了多少？为什么？
 *   2. ReadWriteLock 的核心规则是什么？（读读/读写/写写 哪些能并发？）
 *   3. 写锁等待时，如果读线程源源不断进来，写线程会怎样？（写饥饿问题）
 */
public class Step9_ReadWriteLock {

    static volatile String cache = "初始值";
    static final ReentrantLock lock = new ReentrantLock();
    static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== 用 ReentrantLock（读也互斥）=====");
        long t1 = withReentrantLock();

        System.out.println("\n===== 用 ReadWriteLock（读读并发）=====");
        long t2 = withReadWriteLock();

        System.out.println("\n耗时对比：ReentrantLock=" + t1 + "ms，ReadWriteLock=" + t2 + "ms");
        System.out.println("ReadWriteLock 快了约 " + (t1 - t2) + "ms");

        // 思考：快了多少？能解释这个差值从哪里来吗？
    }

    static long withReentrantLock() throws InterruptedException {
        cache = "初始值";
        Thread[] readers = new Thread[5];
        Thread writer = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  [写] 开始更新...");
                Thread.sleep(100);
                cache = "新值";
                System.out.println("  [写] 更新完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        for (int i = 0; i < 5; i++) {
            final int id = i;
            readers[i] = new Thread(() -> {
                lock.lock();
                try {
                    Thread.sleep(50);
                    System.out.println("  [读" + id + "] 读到：" + cache);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            });
        }

        long start = System.currentTimeMillis();
        writer.start();
        for (Thread r : readers) r.start();
        writer.join();
        for (Thread r : readers) r.join();
        long cost = System.currentTimeMillis() - start;
        System.out.println("总耗时：" + cost + "ms");
        return cost;
    }

    static long withReadWriteLock() throws InterruptedException {
        cache = "初始值";
        Thread[] readers = new Thread[5];
        Thread writer = new Thread(() -> {
            rwLock.writeLock().lock();
            try {
                System.out.println("  [写] 开始更新...");
                Thread.sleep(100);
                cache = "新值";
                System.out.println("  [写] 更新完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rwLock.writeLock().unlock();
            }
        });

        for (int i = 0; i < 5; i++) {
            final int id = i;
            readers[i] = new Thread(() -> {
                rwLock.readLock().lock();  // <-- 读锁，多个读线程可以同时持有
                try {
                    Thread.sleep(50);
                    System.out.println("  [读" + id + "] 读到：" + cache);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.readLock().unlock();
                }
            });
        }

        long start = System.currentTimeMillis();
        writer.start();
        for (Thread r : readers) r.start();
        writer.join();
        for (Thread r : readers) r.join();
        long cost = System.currentTimeMillis() - start;
        System.out.println("总耗时：" + cost + "ms");
        return cost;
    }
}
