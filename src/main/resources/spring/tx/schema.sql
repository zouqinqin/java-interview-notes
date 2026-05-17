CREATE TABLE IF NOT EXISTS account (
    user_id BIGINT PRIMARY KEY,
    balance DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS record (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount  DOUBLE NOT NULL
);

INSERT INTO account(user_id, balance) VALUES(1, 1000.0);
