package jvm.classloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BreakParentDelegation extends ClassLoader {

    private String classPath;

    public BreakParentDelegation(String classPath) {
        this.classPath = classPath;
    }

    /**
     * 重写 loadClass，打破双亲委派
     * 正常流程是先委托父加载器，这里改成先自己加载
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {

            // 第一步：检查是否已经加载过
            Class<?> c = findLoadedClass(name);

            if (c == null) {
                // 关键：java. 开头的核心类还是交给父加载器
                // 否则会报 SecurityException，JVM 不允许自定义 java.lang 下的类
                if (name.startsWith("java.")) {
                    c = getParent().loadClass(name);
                } else {
                    // 其他类：先自己尝试加载，加载不到再委托父加载器
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        // 自己找不到，才委托父加载器
                        c = getParent().loadClass(name);
                    }
                }
            }

            if (resolve) resolveClass(c);
            return c;
        }
    }

    /**
     * 实际从磁盘读取 .class 文件字节码
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String filePath = classPath + name.replace(".", "/") + ".class";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            // defineClass 把字节码转成 Class 对象
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("找不到类文件: " + filePath);
        }
    }
}