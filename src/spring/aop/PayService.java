package spring.aop;

import org.springframework.stereotype.Service;

@Service
public class PayService implements IPayService{

    public void pay(long userId, double amount) {
        System.out.println("执行支付: userId=" + userId + ", amount=" + amount);
    }

    public double queryBalance(long userId) {
        System.out.println("查询余额: userId=" + userId);
        return 1000.0;
    }

    public void payAndCheck(long userId, double amount) {
        System.out.println("payAndCheck 开始");
        this.pay(userId, amount);  // 注意：用 this 调用
    }
}
