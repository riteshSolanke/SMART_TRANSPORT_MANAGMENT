ALTER TABLE otp_verifications
    MODIFY purpose ENUM(
        'REGISTRATION',
        'LOGIN',
        'RESET_PASSWORD',
        'EMAIL_VERIFY',
        'EMAIL_VERIFICATION'
    ) NOT NULL;

UPDATE otp_verifications
SET purpose = 'EMAIL_VERIFICATION'
WHERE purpose = 'EMAIL_VERIFY';

ALTER TABLE otp_verifications
    MODIFY purpose ENUM(
        'REGISTRATION',
        'LOGIN',
        'RESET_PASSWORD',
        'EMAIL_VERIFICATION'
    ) NOT NULL;

CREATE INDEX idx_otp_lookup
    ON otp_verifications(identifier, purpose, is_used, created_at);

CREATE UNIQUE INDEX uk_refresh_token_hash
    ON refresh_tokens(token);

CREATE INDEX idx_refresh_user
    ON refresh_tokens(user_id);
