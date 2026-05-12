package springboot;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class LifeCycleBean {

    private static final Logger logger = LoggerFactory.getLogger(LifeCycleBean.class);

    public LifeCycleBean() {
        logger.info("LifeCycleBean 构造方法");
    }

    @Autowired
    public void autowire(@Value("${JAVA_HOME}") String home) {
        logger.info("依赖注入：{}",home);
    }

    @PostConstruct
    public void init(){
        logger.info("初始化");
    }

    @PreDestroy
    public void destroy(){
        logger.info("销毁");
    }

}
