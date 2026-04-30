package reflect_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 实验二：JDK 动态代理
 *
 * 目标：不改 Calculator 类，在每次方法调用前后自动加日志
 *
 * JDK 动态代理有两个限制：
 *   1. 被代理的类必须实现接口
 *   2. 代理的是接口方法，不是类方法
 *
 * 【先读懂代码结构，再跑起来看输出】
 * 思考：
 *   1. InvocationHandler 的 invoke 方法什么时候被调用？
 *   2. method.invoke(target, args) 里的 target 是谁？
 *   3. 如果 Calculator 没有实现接口，这个方案还能用吗？
 */
public class Step2_JDKProxy {

    interface ICalculator {
        int add(int a, int b);
    }

    static class Calculator implements ICalculator {
        public int add(int a, int b) {
            return a + b;
        }
    }

    static class LogHandler implements InvocationHandler {
        private final Object target;

        LogHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long start = System.currentTimeMillis();
            System.out.println("[LOG] " + method.getName() + " 开始执行");

            Object result = method.invoke(target, args);

            long cost = System.currentTimeMillis() - start;
            System.out.println("[LOG] " + method.getName() + " 执行完毕，耗时 " + cost + "ms");
            return result;
        }
    }

    public static void main(String[] args) {
        Calculator real = new Calculator();

        ICalculator proxy = (ICalculator) Proxy.newProxyInstance(
            real.getClass().getClassLoader(),
            new Class[]{ICalculator.class},
            new LogHandler(real)
        );

        int result = proxy.add(3, 5);
        System.out.println("结果：" + result);

        // 打印代理对象的类名，看看它是什么
        System.out.println("proxy 的类型：" + proxy.getClass().getName());
    }
}
