package jvm.classloader;

public class ClassLoaderTest {

    public static void main(String[] args) throws Exception {

        // ===== 测试1：对比两种加载器 =====
        System.out.println("===== 测试1：正常 vs 打破双亲委派 =====");

        // 不打破：父加载器能找到的类，findClass 不会被调用
        NormalClassLoader normal = new NormalClassLoader("/tmp/classes/");
        System.out.println("正常加载器的父加载器: " + normal.getParent());

        // 打破：自己优先
        BreakParentDelegation breakLoader = new BreakParentDelegation("/tmp/classes/");
        System.out.println("打破委派加载器的父加载器: " + breakLoader.getParent());


        // ===== 测试2：模拟 Tomcat 两个应用加载同名类 =====
        System.out.println("\n===== 测试2：模拟 Tomcat 应用隔离 =====");

        // 应用A 和 应用B 各自有一个 WebAppClassLoader
        // 即使加载同一个类名，因为加载器不同，得到的是不同的 Class 对象
        WebAppClassLoader appA = new WebAppClassLoader("AppA", "/webapps/appA/WEB-INF/lib");
        WebAppClassLoader appB = new WebAppClassLoader("AppB", "/webapps/appB/WEB-INF/lib");

        System.out.println("AppA 加载器: " + appA.getAppId());
        System.out.println("AppB 加载器: " + appB.getAppId());

        // 关键结论：同一个类名，不同类加载器加载出来的 Class 对象不相等
        // appA.loadClass("com.example.Service") != appB.loadClass("com.example.Service")
        // 这就是 Tomcat 实现多版本隔离的原理


        // ===== 测试3：查看当前类的加载器链 =====
        System.out.println("\n===== 测试3：默认加载器链 =====");
        ClassLoader cl = ClassLoaderTest.class.getClassLoader();
        while (cl != null) {
            System.out.println(cl);
            cl = cl.getParent();
        }
        // 最后输出 null，代表 Bootstrap ClassLoader（C++ 实现，Java里看不到）
        System.out.println("null (Bootstrap ClassLoader)");
    }
}