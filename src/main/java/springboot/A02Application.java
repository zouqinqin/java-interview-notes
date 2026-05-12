package springboot;

import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class A02Application {
    public static void main(String[] args) {
//      TestClassPathXmlApplicationContext();
//        TestFileSystemXmlApplicationContext();
//        TestAnnotationConfigApplicationContext();

//        TestAnnotationConfigServletWebServerApplicationContext();
      /*
        // XML 配置生效的原理，其实是配置了BeanFactory
       System.out.println("获取配置前");
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        for (String definitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }

        System.out.println("获取配置后===");
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions(new FileSystemResource("src\\main\\resources\\b1.xml"));

        for (String definitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }*/



    }

   /* private static void TestAnnotationConfigServletWebServerApplicationContext(){
        AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(WebConfig.class);
        for (String definitionName : context.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }

    }*/




   //较为经典的容器，基于java配置类来创建
    private static void TestAnnotationConfigApplicationContext(){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

        for (String definitionName : context.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
    //基于磁盘路径下的xml 格式的配置文件来创建
    private static void TestFileSystemXmlApplicationContext(){
        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext("src\\main\\resources\\b1.xml");

        for (String definitionName : context.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }
    // 较为经典的容器，基于classPath下的xml 格式的配置文件来创建
    private static void TestClassPathXmlApplicationContext(){
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("b1.xml");

        for (String definitionName : context.getBeanDefinitionNames()) {
            System.out.println(definitionName);
        }

        System.out.println(context.getBean(Bean2.class).getBean1());
    }

  /*  @Configuration
    static class WebConfig{
        @Bean
        public ServletWebServerFactory servletFactory(){
            return new TomcatServletWebServerFactory();
        }
        @Bean
        public DispatcherServlet dispatcherServlet(){
            return new DispatcherServlet();
        }

        @Bean
        public DispatcherServletRegistrationBean servletRegistrationBean(DispatcherServlet dispatcherServlet){
            return new DispatcherServletRegistrationBean(dispatcherServlet,"/");
        }

        @Bean("/hello")
        public Controller controller1(){
            return (request, response) -> {
                response.getWriter().print("hello");
                return null;
            };
        }


    }*/


    @Configuration
    static class Config{

        @Bean
        public Bean1 getBean1(){
            return new Bean1();
        }

        @Bean
        public Bean2 getBean2(Bean1 bean1){
            Bean2 bean2 = new Bean2();
            bean2.setBean1(bean1);
            return bean2;
        }

    }


    static class Bean1{

    }
    static class Bean2{

       private Bean1 bean1;

       public void setBean1(Bean1 bean1){
           this.bean1 = bean1;
       }

       public Bean1 getBean1(){
           return bean1;
       }
    }

}

