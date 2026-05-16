package springboot.a04;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DigINAutowired {
    public static void main(String[] args) throws Throwable {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("bean2", new Bean2());
        beanFactory.registerSingleton("bean3", new Bean3());
        // 解析 @ Value 注解中${JAVA_HOME} 值
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        beanFactory.addEmbeddedValueResolver(new StandardEnvironment()::resolvePlaceholders);

        //1 查找属性 方法加了 @Autowired
        AutowiredAnnotationBeanPostProcessor postProcessor = new AutowiredAnnotationBeanPostProcessor();
        postProcessor.setBeanFactory(beanFactory);

        Bean1 bean1 = new Bean1();
      /*  System.out.println(bean1);

        postProcessor.postProcessProperties(null,bean1,"bean1");
        System.out.println(bean1);*/

        Method findAutowiringMetadata =  AutowiredAnnotationBeanPostProcessor.class.getDeclaredMethod("findAutowiringMetadata", String.class, Class.class, PropertyValues.class);
        findAutowiringMetadata.setAccessible(true);

        InjectionMetadata metadata = (InjectionMetadata) findAutowiringMetadata.invoke(postProcessor, "bean1", Bean1.class, null);

        System.out.println(metadata);

        //2. 调用InjectionMetadata 方法进行依赖注入，注入时先按类型查找
        metadata.inject(bean1,"bean1",null);
        System.out.println(bean1);

        // 3.按类型查找值
        Field bean3 = Bean1.class.getDeclaredField("bean3");
        DependencyDescriptor descriptor = new DependencyDescriptor(bean3, false);
        Object object = beanFactory.doResolveDependency(descriptor, null, null, null);
        System.out.println(object);

        Method bean2 = Bean1.class.getDeclaredMethod("setBean2", Bean2.class);
        MethodParameter methodParameter = new MethodParameter(bean2, 0);
        DependencyDescriptor descriptor1 = new DependencyDescriptor(methodParameter, false);
        Object object1 = beanFactory.doResolveDependency(descriptor1, null, null, null);
        System.out.println(object1);

        Method setHome = Bean1.class.getDeclaredMethod("setHome", String.class);
        DependencyDescriptor descriptor2 = new DependencyDescriptor(new MethodParameter(setHome, 0), true);
        Object object2 = beanFactory.doResolveDependency(descriptor2, null, null, null);
        System.out.println(object2);



    }

}
