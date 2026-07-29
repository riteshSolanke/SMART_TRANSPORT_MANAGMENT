CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    mobile_number VARCHAR(15) NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    password_hash VARCHAR(255),
    role ENUM('ADMIN','CONDUCTOR','DISPATCHER','PASSENGER','TRANSPORT_MANAGER') NOT NULL,
    is_mobile_verified BIT(1) DEFAULT b'0',
    is_email_verified BIT(1) DEFAULT b'0',
    is_active BIT(1) DEFAULT b'1',
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_mobile UNIQUE (mobile_number),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS otp_verifications (
    otp_id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(150) NOT NULL,
    otp_code VARCHAR(255) NOT NULL,
    purpose ENUM('REGISTRATION','LOGIN','RESET_PASSWORD','EMAIL_VERIFICATION') NOT NULL,
    is_used BIT(1) DEFAULT b'0',
    attempt_count INT DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (otp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    is_revoked BIT(1) DEFAULT b'0',
    created_at DATETIME(6),
    PRIMARY KEY (token_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
