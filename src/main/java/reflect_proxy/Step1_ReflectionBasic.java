package reflect_proxy;

import java.lang.reflect.Method;

/**
 * 实验一：反射能干什么？
 *
 * 场景：有一个 Calculator 类，里面有个 private 方法 add
 * 正常情况下外部无法调用 private 方法
 * 反射可以绕过访问限制，强行调用
 *
 * 【直接跑，看报错信息，根据报错找到问题并修复】
 *
 * 思考：
 *   1. 报的是什么异常？为什么？
 *   2. 修复之后，反射调用 private 方法的完整步骤是什么？
 *   3. int 和 Integer 在反射里是同一个类型吗？
 */
public class Step1_ReflectionBasic {

    static class Calculator {
        private int add(int a, int b) {
            return a + b;
        }
    }

    public static void main(String[] args) throws Exception {
        Calculator calc = new Calculator();
        Long start = System.currentTimeMillis();

        // 用反射获取 add 方法
        Method method = Calculator.class.getDeclaredMethod("add", int.class, int.class);

        method.setAccessible(true);
        // 调用方法
        Object result = method.invoke(calc, 3, 5);

        System.out.println("结果：" + result);
    }
}
