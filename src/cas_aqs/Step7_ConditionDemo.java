package cas_aqs;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有界队列：生产者 + 消费者
 * 容量 3，生产者放10个，消费者取10个
 *
 * 代码有几处问题，找到并修复，让程序正常运行输出10条生产 + 10条消费
 */
public class Step7_ConditionDemo {

    static final int CAPACITY = 3;
    static Queue<Integer> queue = new LinkedList<>();
    static ReentrantLock lock = new ReentrantLock();
    static Condition notFull  = lock.newCondition();
    static Condition notEmpty = lock.newCondition();

    public static void main(String[] args) {
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                lock.lock();
                try {
                    while (queue.size() == CAPACITY) {
//                        notEmpty.await();
                        notFull.await();
                    }
                    queue.offer(i); //队列中添加线程
                    System.out.println("[生产者] 放入：" + i + "，当前队列：" + queue);
                    notEmpty.signal();
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
                        notEmpty.await();
                    }
                    int val = queue.poll();
                    System.out.println("[消费者] 取出：" + val + "，当前队列：" + queue);
                    notFull.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }, "Consumer");

        consumer.start();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        producer.start();
    }
}
