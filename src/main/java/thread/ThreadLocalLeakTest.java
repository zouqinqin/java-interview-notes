package thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalLeakTest {

    // 模拟一个大对象
    static class BigObject {
        private byte[] data = new byte[1024 * 1024]; // 1MB
        private String name;

        public BigObject(String name){
            this.name= name;
        }

    }

    static List<ThreadLocal<BigObject>> leakList = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        // 模拟线程池，只有一个线程，会被复用
        ExecutorService pool = Executors.newFixedThreadPool(1);

        for (int i = 0; i < 200; i++) {
            final int taskId = i;
            pool.execute(() -> {
                // 每次任务创建一个新的 ThreadLocal
                ThreadLocal<BigObject> t1 = new ThreadLocal<>();
                t1.set(new BigObject("Task" + taskId + "Big Object"));

                leakList.add(t1);

                System.out.println("任务" + taskId + " 执行，存入1MB数据");

                //故意不remove

            });
            Thread.sleep(50);
        }

        pool.shutdown();
    }
}