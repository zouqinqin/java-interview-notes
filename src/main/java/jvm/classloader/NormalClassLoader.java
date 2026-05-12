package jvm.classloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NormalClassLoader extends ClassLoader {

    private String classPath;

    public NormalClassLoader(String classPath) {
        this.classPath = classPath;
    }

    /**
     * 只重写 findClass，不动 loadClass
     * 双亲委派逻辑完全保留
     * 父加载器找不到时，才会调用这个方法
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String filePath = classPath + name.replace(".", "/") + ".class";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("找不到类文件: " + filePath);
        }
    }
}