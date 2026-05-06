package spring.aop;

public interface IPayService {
    void pay(long userId, double amount);
    double queryBalance(long userId);
}

