package cas_aqs;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 实验四b：CAS 只能操作单个变量？用 AtomicReference 解决
 *
 * 场景：转账操作，需要同时修改"金额"和"最后操作人"两个字段
 *
 * 问题：AtomicInteger 只能保证一个字段的原子性
 *       如果两个字段分开更新，中间可能被其他线程插入，导致数据不一致
 *
 * 【先看 withoutAtomicReference()，想想它的问题在哪里】
 * 【再看 withAtomicReference()，对比两种方式的区别】
 *
 * 思考：
 *   1. withoutAtomicReference 中，金额和操作人是同时更新的吗？
 *   2. AtomicReference 是怎么把多个字段变成"一个变量"来操作的？
 *   3. 这种方式有没有缺点？（提示：每次更新都要创建新对象）
 */
public class Step4b_AtomicReference {

    // 账户快照：把需要原子更新的字段封装成一个不可变对象
    static class AccountSnapshot {
        final int balance;
        final String lastOperator;

        AccountSnapshot(int balance, String lastOperator) {
            this.balance = balance;
            this.lastOperator = lastOperator;
        }

        public String toString() {
            return "余额=" + balance + ", 操作人=" + lastOperator;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== 分开更新两个字段（有问题）=====");
        withoutAtomicReference();

        System.out.println("\n===== 用 AtomicReference 原子更新 =====");
        withAtomicReference();
    }

    // 错误示范：两个字段分开更新，不是原子的
    static void withoutAtomicReference() throws InterruptedException {
        int[] balance = {1000};
        String[] lastOperator = {"无"};

        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final String name = "操作人" + i;
            threads[i] = new Thread(() -> {
                // 这两行不是原子的，中间可能被插入
                balance[0] -= 100;
                // ↑ 扣款和记录操作人之间，其他线程可能已经又改了 balance
                lastOperator[0] = name;
                System.out.println(name + " 操作后：余额=" + balance[0] + ", 操作人=" + lastOperator[0]);
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("最终状态：余额=" + balance[0] + ", 操作人=" + lastOperator[0]);
        // 思考：最终的"操作人"和"余额"对应的是同一次操作吗？能保证吗？
    }

    // 正确做法：用 AtomicReference 把多个字段封装成一个对象，整体原子替换
    static void withAtomicReference() throws InterruptedException {
        AtomicReference<AccountSnapshot> account =
            new AtomicReference<>(new AccountSnapshot(1000, "无"));

        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final String name = "操作人" + i;
            threads[i] = new Thread(() -> {
                AccountSnapshot current, next;
                do {
                    current = account.get();
                    // 创建新快照（不可变对象，不修改旧的）
                    next = new AccountSnapshot(current.balance - 100, name);
                } while (!account.compareAndSet(current, next)); // 整体原子替换

                System.out.println(name + " 操作成功：" + account.get());
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("最终状态：" + account.get());
        // 现在余额和操作人一定是同一次操作的结果，不会错位
    }
}
