package springboot;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Component
public class LifecycleBean implements BeanNameAware, ApplicationContextAware {

    public LifecycleBean() {
        System.out.println("1. 构造器执行");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("3. BeanNameAware.setBeanName: " + name);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("4. ApplicationContextAware.setApplicationContext");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("5. @PostConstruct");
    }
}
