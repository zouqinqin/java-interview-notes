package cas_aqs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实验三：手动模拟 CAS 的行为，理解"比较再交换"
 *
 * AtomicInteger 底层的 do-while 逻辑大致是这样的：
 *   do {
 *       current = get();
 *   } while (!compareAndSet(current, current + 1));
 *
 * 下面用可观察的方式把这个过程打印出来，感受一下"自旋"是什么意思
 *
 * 【跑一下，观察 retry 次数】
 * 思考：
 *   1. 线程越多，retry 次数会怎么变化？把 THREAD_COUNT 改大试试
 *   2. 如果竞争非常激烈，一直 CAS 失败，会有什么后果？
 *   3. 这种方式适合竞争激烈的场景吗？
 */
public class Step3_CASUnderHood {

    static AtomicInteger counter = new AtomicInteger(0);
    static AtomicInteger retryCount = new AtomicInteger(0); // 记录自旋重试次数

    static final int THREAD_COUNT = 10; // 试试改成 100，观察 retry 变化

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    // 手动模拟 CAS 自旋
                    int current;
                    do {
                        current = counter.get();
                        retryCount.incrementAndGet(); // 每次循环都算一次尝试
                    } while (!counter.compareAndSet(current, current + 1));
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int total = THREAD_COUNT * 1000;
        System.out.println("最终 counter：" + counter.get() + "（预期 " + total + "）");
        System.out.println("总尝试次数：" + retryCount.get());
        System.out.println("实际操作次数：" + total);
        System.out.println("额外自旋次数：" + (retryCount.get() - total));

        // 把 THREAD_COUNT 从 10 改到 100 再跑一次
        // 额外自旋次数增加了多少倍？
        // 这说明了 CAS 在什么情况下不适用？
    }
}
