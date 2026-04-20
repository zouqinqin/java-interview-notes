package jvm.classloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 Tomcat WebAppClassLoader
 * 不同的 appId 对应不同的类加载器实例，实现类隔离
 */
public class WebAppClassLoader extends ClassLoader {

    private String appId;
    private String webInfLibPath;

    // 模拟每个应用自己的类缓存
    private Map<String, Class<?>> classCache = new HashMap<>();

    public WebAppClassLoader(String appId, String webInfLibPath) {
        // 注意：父加载器传 null，意味着直接使用 Bootstrap
        super(null);
        this.appId = appId;
        this.webInfLibPath = webInfLibPath;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {

            // 1. 检查本应用缓存
            Class<?> c = classCache.get(name);
            if (c != null) {
                System.out.println("[" + appId + "] 命中缓存: " + name);
                return c;
            }

            // 2. java. 开头的类必须走 Bootstrap，不能自己加载
            if (name.startsWith("java.") || name.startsWith("javax.")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }

            // 3. 优先从自己的 WEB-INF/lib 加载（打破双亲委派的核心）
            try {
                c = findClass(name);
                classCache.put(name, c);
                System.out.println("[" + appId + "] 自己加载成功: " + name);
                if (resolve) resolveClass(c);
                return c;
            } catch (ClassNotFoundException e) {
                // 4. 自己找不到，再委托系统类加载器
                System.out.println("[" + appId + "] 自己找不到，委托系统加载: " + name);
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String filePath = webInfLibPath + "/" + name.replace(".", "/") + ".class";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    public String getAppId() { return appId; }
}