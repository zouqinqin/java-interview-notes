package springboot;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if ("lifecycleBean".equals(beanName)) {
            System.out.println(">>> BeanPostProcessor.Before");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("lifecycleBean".equals(beanName)) {
            System.out.println(">>> BeanPostProcessor.After");
        }
        return bean;
    }
}