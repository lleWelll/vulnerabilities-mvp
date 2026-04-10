INSERT INTO app_user (id, username, password_hash, role, enabled, created_at)
SELECT 1, 'test_client_1', '$2y$12$lVusIz6xzVEtLbrnkyVz.Oggj7.2vvryouxUv3yTA/HlGveRNEKre', 'CLIENT', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = 1);

INSERT INTO app_user (id, username, password_hash, role, enabled, created_at)
SELECT 2, 'test_client_2', '$2y$12$FvNFf05lrp2NJbKn.dsF1uxLL7NU0lyFXdJ8OMv0tU5spqe/8Ko/W', 'CLIENT', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = 2);

INSERT INTO app_user (id, username, password_hash, role, enabled, created_at)
SELECT 3, 'test_operator', '$2y$12$WMedfDmdCTVp5xE30TqKTOe77ISYpc6OJrDePjjRMbd23AwfGb.Wa', 'OPERATOR', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE id = 3);

INSERT INTO account (id, owner_user_id, balance, currency, version, created_at)
SELECT 1001, 1, 10500.00, 'KZT', 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM account WHERE id = 1001);

INSERT INTO account (id, owner_user_id, balance, currency, version, created_at)
SELECT 1002, 2, 9500.00, 'KZT', 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM account WHERE id = 1002);

INSERT INTO account (id, owner_user_id, balance, currency, version, created_at)
SELECT 1003, 1, 500.00, 'USD', 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM account WHERE id = 1003);

INSERT INTO payment (
    id, owner_user_id, owner_account_id, receiver_user_id, receiver_account_id,
    amount, currency, status, description, flagged, created_at, confirmed_at, version
)
SELECT
    2001, 1, 1001, 2, 1002,
    1000.00, 'KZT', 'CONFIRMED', 'Seed confirmed transfer', FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM payment WHERE id = 2001);

INSERT INTO payment (
    id, owner_user_id, owner_account_id, receiver_user_id, receiver_account_id,
    amount, currency, status, description, flagged, created_at, confirmed_at, version
)
SELECT
    2002, 1, 1001, 2, 1002,
    7000.00, 'KZT', 'FLAGGED', 'High amount transfer', TRUE,
    CURRENT_TIMESTAMP, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM payment WHERE id = 2002);

INSERT INTO payment (
    id, owner_user_id, owner_account_id, receiver_user_id, receiver_account_id,
    amount, currency, status, description, flagged, created_at, confirmed_at, version
)
SELECT
    2003, 2, 1002, 1, 1001,
    1500.00, 'KZT', 'CONFIRMED', 'Bob to Alice test transfer', FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM payment WHERE id = 2003);

INSERT INTO fraud_flag (id, payment_id, risk_level, reason, flagged_by, manual, created_at)
SELECT 3001, 2002, 'HIGH', 'Large amount threshold exceeded', NULL, FALSE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM fraud_flag WHERE id = 3001);

ALTER TABLE app_user ALTER COLUMN id RESTART WITH 100;
ALTER TABLE account ALTER COLUMN id RESTART WITH 2000;
ALTER TABLE payment ALTER COLUMN id RESTART WITH 3000;
ALTER TABLE fraud_flag ALTER COLUMN id RESTART WITH 4000;
ALTER TABLE audit_event ALTER COLUMN id RESTART WITH 5000;
