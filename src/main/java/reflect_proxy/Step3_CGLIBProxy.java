package reflect_proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * 实验三：CGLIB 动态代理
 *
 * 场景：Calculator 没有接口，JDK 动态代理用不了
 * CGLIB 通过继承目标类生成子类来实现代理
 *
 * 【跑起来，对比和 JDK 代理的输出有什么不同】
 *
 * 思考：
 *   1. proxy 的类型是什么？和 JDK 代理的 $Proxy0 有什么区别？
 *   2. MethodInterceptor 和 JDK 的 InvocationHandler 作用一样吗？
 *   3. invokeSuper 和 method.invoke 有什么区别？
 *   4. 如果把 Calculator 改成 final 类，会发生什么？
 */
public class Step3_CGLIBProxy {

    static final class Calculator {
        public int add(int a, int b) {
            return a + b;
        }
    }

    static class LogInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            long start = System.currentTimeMillis();
            System.out.println("[LOG] " + method.getName() + " 开始执行");

            Object result = proxy.invokeSuper(obj, args);

            long cost = System.currentTimeMillis() - start;
            System.out.println("[LOG] " + method.getName() + " 执行完毕，耗时 " + cost + "ms");
            return result;
        }
    }

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Calculator.class);
        enhancer.setCallback(new LogInterceptor());

        Calculator proxy = (Calculator) enhancer.create();

        int result = proxy.add(3, 5);
        System.out.println("结果：" + result);

        System.out.println("proxy 的类型：" + proxy.getClass().getName());
        System.out.println("proxy 的父类：" + proxy.getClass().getSuperclass().getName());
    }
}
