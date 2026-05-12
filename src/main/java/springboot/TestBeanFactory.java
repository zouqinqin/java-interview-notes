package springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class TestBeanFactory {

//    public static void main(String[] args) {
//        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//
//        //bean 的定义 （class，scope,初始化，销毁）
//        AbstractBeanDefinition beanDefinition =
//                BeanDefinitionBuilder.genericBeanDefinition(Config.class).setScope("singleton").getBeanDefinition();
//
//        beanFactory.registerBeanDefinition("config", beanDefinition);
//
//        //给BeanFactory 添加一些常用的后置处理器
//        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
//
//        //BeanFactory 后置处理器主要功能，补充了一些bean定义
//        beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values().forEach(beanFactoryPostProcessor -> {
//            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
//        });
//
//        for (String name : beanFactory.getBeanDefinitionNames()) {
//            System.out.println(name);
//        }
//        beanFactory.getBeansOfType(BeanPostProcessor.class).values().forEach(beanFactory::addBeanPostProcessor);
//
//        beanFactory.preInstantiateSingletons();
//
//        System.out.println("==================");
//
//        System.out.println(beanFactory.getBean(Bean1.class).getBean2());
//
//
//    }
/*
    @Configuration
    static class Config{

        @Bean
        public Bean1 bean1(){
            return new Bean1();
        }
        @Bean
        public Bean2 bean2(){
            return new Bean2();
        }

    }

    static class Bean1{
        private static final Logger log = LoggerFactory.getLogger(Bean1.class);

        public Bean1(){
            log.debug("Bean1 构造器");
        }
        @Autowired
        private Bean2 bean2;

        public Bean2 getBean2() {
            return bean2;
        }
    }

    static class Bean2{
        private static final Logger log = LoggerFactory.getLogger(Bean2.class);

        public Bean2(){
            log.debug("Bean2 构造器");
        }
        @Autowired
        private Bean1 bean1;

        public Bean1 getBean1() {
            return bean1;
        }
    }*/

}
