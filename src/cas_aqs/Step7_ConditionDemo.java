package cas_aqs;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验七：Condition —— 比 wait/notify 更精准的线程协作
 *
 * 场景：有界队列（容量3），一个生产者，一个消费者
 *   - 队列满了，生产者必须等待
 *   - 队列空了，消费者必须等待
 *
 * 用 Condition 可以分别唤醒生产者或消费者，而不是 notifyAll 唤醒所有
 *
 * 【跑一下，观察生产和消费的交替顺序】
 * 思考：
 *   1. notFull.await() 和 Object.wait() 有什么相似之处？有什么不同？
 *   2. 为什么这里用两个 Condition（notFull / notEmpty），而不是一个？
 *   3. 如果用 synchronized + notifyAll 实现，会有什么额外开销？
 *   4. await() 执行时，锁有没有被释放？（很重要！）
 */
public class Step7_ConditionDemo {

    static final int CAPACITY = 3;
    static Queue<Integer> queue = new LinkedList<>();
    static ReentrantLock lock = new ReentrantLock();
    static Condition notFull  = lock.newCondition(); // 队列不满（通知生产者可以放）
    static Condition notEmpty = lock.newCondition(); // 队列不空（通知消费者可以取）

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                lock.lock();
                try {
                    while (queue.size() == CAPACITY) {
                        System.out.println("  [生产者] 队列满了，等待...");
                        notFull.await(); // 等待消费者消费，await 期间锁会释放
                    }
                    queue.offer(i);
                    System.out.println("[生产者] 放入：" + i + "，当前队列：" + queue);
                    notEmpty.signal(); // 只唤醒消费者，不唤醒其他生产者
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        System.out.println("  [消费者] 队列空了，等待...");
                        notEmpty.await(); // 等待生产者放入，await 期间锁会释放
                    }
                    int val = queue.poll();
                    System.out.println("[消费者] 取出：" + val + "，当前队列：" + queue);
                    notFull.signal(); // 只唤醒生产者
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }, "Consumer");

        // 故意先启动消费者，看它会怎么处理"队列为空"的情况
        consumer.start();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        producer.start();

        // 最后思考：
        // 如果把 notFull.signal() 和 notEmpty.signal() 都换成 lock 自带的一个 Condition
        // 用 condition.signalAll() 来替代，会有什么问题？
    }
}
