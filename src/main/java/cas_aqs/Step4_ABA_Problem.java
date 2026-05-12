package cas_aqs;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * 实验四：ABA 问题 —— CAS 看不见的陷阱
 *
 * 场景：账户余额 100 元
 *   - 线程A 读到 100，准备 CAS 改成 50（扣款50）
 *   - 线程A 被挂起
 *   - 线程B 把 100 改成 50，然后又充值把 50 改回 100
 *   - 线程A 恢复，CAS 发现"还是100"，认为没人动过，扣款成功
 *
 * 【跑一下，观察两个版本的输出有什么不同】
 * 思考：
 *   1. 普通 AtomicInteger 的 CAS 为什么发现不了 ABA？
 *   2. AtomicStampedReference 加了什么东西来解决这个问题？
 *   3. 现实中哪些业务场景可能受 ABA 影响？
 */
public class Step4_ABA_Problem {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== 复现 ABA 问题 =====");
        withoutStamp();

        System.out.println("\n===== 用版本号解决 ABA =====");
        withStamp();
    }

    static void withoutStamp() throws InterruptedException {
        AtomicInteger balance = new AtomicInteger(100);

        // 线程A：读到100，准备扣款50
        int expectedByA = balance.get(); // A 读到 100
        System.out.println("线程A 读到余额：" + expectedByA);

        // 模拟线程A被挂起期间，线程B做了 100->50->100
        Thread threadB = new Thread(() -> {
            balance.compareAndSet(100, 50);  // 扣款
            System.out.println("线程B 扣款后：" + balance.get());
            balance.compareAndSet(50, 100);  // 充值
            System.out.println("线程B 充值后：" + balance.get());
        },"threadB");
        threadB.start();
        threadB.join();

        // 线程A 恢复，执行 CAS
        boolean success = balance.compareAndSet(expectedByA, expectedByA - 50);
        System.out.println("线程A CAS 结果：" + (success ? "成功，余额变成 " + balance.get() : "失败"));
        // 思考：线程A 是否应该成功？它感知到了 B 的操作吗？
    }

    static void withStamp() throws InterruptedException {
        AtomicStampedReference<Integer> balance = new AtomicStampedReference<>(100, 0);

        // 线程A 读值的同时记录 stamp（版本号）
        int[] stampHolder = new int[1];
        int expectedByA = balance.get(stampHolder);
        int stampByA = stampHolder[0];
        System.out.println("线程A 读到余额：" + expectedByA + "，版本号：" + stampByA);

        // 线程B 做 100->50->100，每次操作版本号+1
        Thread threadB = new Thread(() -> {
            int[] s = new int[1];
            int v = balance.get(s);
            balance.compareAndSet(v, 50, s[0], s[0] + 1);
            System.out.println("线程B 扣款后：" + balance.getReference() + "，版本号：" + balance.getStamp());
            v = balance.get(s);
            balance.compareAndSet(v, 100, s[0], s[0] + 1);
            System.out.println("线程B 充值后：" + balance.getReference() + "，版本号：" + balance.getStamp());
        });
        threadB.start();
        threadB.join();

        // 线程A 用旧的 stamp 尝试 CAS
        boolean success = balance.compareAndSet(expectedByA, expectedByA - 50, stampByA, stampByA + 1);
        System.out.println("线程A CAS 结果：" + (success ? "成功" : "失败，检测到版本变化，当前版本：" + balance.getStamp()));
        // 思考：stamp 是怎么让 CAS 感知到"值虽然一样但被动过"的？
    }
}
