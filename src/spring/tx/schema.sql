CREATE TABLE account (
    user_id BIGINT PRIMARY KEY,
    balance DECIMAL(10, 2)
);

CREATE TABLE record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    amount DECIMAL(10, 2)
);

INSERT INTO account(user_id, balance) VALUES(1, 1000.00);
