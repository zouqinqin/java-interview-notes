package springboot.a04;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringValueResolver;

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


    }

}
