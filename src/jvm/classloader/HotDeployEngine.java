package jvm.classloader;

import java.lang.reflect.Method;

/**
 * 热部署引擎
 * 用反射调用目标类，这样不依赖编译期的类型，才能实现运行时替换
 */
public class HotDeployEngine {

    private String classDir;
    private String className;

    // 当前使用的类加载器和类实例
    private HotDeployClassLoader currentLoader;
    private Object currentInstance;
    private Class<?> currentClass;

    public HotDeployEngine(String classDir, String className) {
        this.classDir = classDir;
        this.className = className;
    }

    /**
     * 加载或重新加载类
     */
    public void reload() throws Exception {
        System.out.println("\n>>> 开始加载: " + className);

        // 每次 reload 都创建全新的 ClassLoader 实例
        // 旧的 ClassLoader 没有引用后会被 GC 回收
        currentLoader = new HotDeployClassLoader(classDir);

        // 用新加载器加载类
        currentClass = currentLoader.loadClass(className);

        // 创建新实例
        currentInstance = currentClass.getDeclaredConstructor().newInstance();

        System.out.println(">>> 加载完成，使用的加载器: " + currentInstance.getClass().getClassLoader());
    }

    /**
     * 调用目标类的方法
     */
    public Object invoke(String methodName) throws Exception {
        if (currentInstance == null) {
            throw new IllegalStateException("请先调用 reload()");
        }
        Method method = currentClass.getMethod(methodName);
        return method.invoke(currentInstance);
    }
}