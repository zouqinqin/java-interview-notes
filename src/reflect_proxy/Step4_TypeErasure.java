package reflect_proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * 实验四：泛型擦除
 *
 * Java 的泛型只在编译期有效，运行时类型信息会被擦除
 *
 * 【直接跑，看输出，带着问题思考】
 *
 * 思考：
 *   1. listA 和 listB 的运行时类型一样吗？
 *   2. 第30行能编译通过吗？为什么？
 *   3. 既然运行时类型一样，能不能强行往 listA 里塞一个 Integer？
 */
public class Step4_TypeErasure {

    public static void main(String[] args) throws Exception {
        List<String> listA = new ArrayList<>();
        List<Integer> listB = new ArrayList<>();

        // 问题1：运行时类型一样吗？
        System.out.println("listA 的类型：" + listA.getClass());
        System.out.println("listB 的类型：" + listB.getClass());
        System.out.println("类型相同：" + (listA.getClass() == listB.getClass()));

        // 问题2：这行能编译通过吗？
//         listA.add(123);

        // 问题3：强行塞一个 Integer 进去
        listA.getClass().getMethod("add", Object.class).invoke(listA, 123);
        System.out.println("listA 的内容：" + listA);
        System.out.println("取出第一个元素：" + listA.get(0));

        // 问题4：下面这行会不会报错？
        String s = listA.get(0);
        System.out.println(s);
    }
}
