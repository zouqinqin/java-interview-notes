package cas_aqs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实验二：用 AtomicInteger 替换 int，问题解决了吗？
 *
 * 【跑一下，和 Step1 的输出对比】
 * 思考：
 *   1. AtomicInteger 和普通 int 的区别在哪里？
 *   2. 它是怎么做到不加锁也能保证原子性的？
 *   3. 底层到底发生了什么？（提示：看 getAndIncrement 的源码）
 */
public class Step2_AtomicFix {

    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.getAndIncrement();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("预期结果：10000");
        System.out.println("实际结果：" + counter.get());

        // 结果是不是每次都准确？
        // 下一步：去看一下 AtomicInteger.getAndIncrement() 的源码
        // 找到这行：unsafe.getAndAddInt(this, valueOffset, 1)
        // 再往下看 getAndAddInt 的实现，注意那个 do-while 循环
        // 思考：这个循环在干什么？什么情况下会循环多次？
    }
}
