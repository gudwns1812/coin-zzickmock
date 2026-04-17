CREATE TABLE member_credentials (
    member_id VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    member_email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    address_detail VARCHAR(255) NOT NULL,
    invest_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_credentials PRIMARY KEY (member_id),
    CONSTRAINT fk_member_credentials_account
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);
