package org.aitu.vulnerabilitiesmvp;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@Transactional
class TestDataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    TestDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existingUsers = jdbcTemplate.queryForObject("select count(*) from app_user", Integer.class);
        if (existingUsers != null && existingUsers > 0) {
            return;
        }

        Instant seededAt = Instant.parse("2026-01-01T00:00:00Z");
        Timestamp timestamp = Timestamp.from(seededAt);

        jdbcTemplate.update(
            "insert into app_user (id, username, password_hash, role, enabled, created_at) values (?, ?, ?, ?, ?, ?)",
            101L, "client_alice", passwordEncoder.encode("AlicePass123!"), "CLIENT", true, timestamp
        );
        jdbcTemplate.update(
            "insert into app_user (id, username, password_hash, role, enabled, created_at) values (?, ?, ?, ?, ?, ?)",
            102L, "client_bob", passwordEncoder.encode("BobPass123!"), "CLIENT", true, timestamp
        );
        jdbcTemplate.update(
            "insert into app_user (id, username, password_hash, role, enabled, created_at) values (?, ?, ?, ?, ?, ?)",
            103L, "operator_jane", passwordEncoder.encode("OperatorPass123!"), "OPERATOR", true, timestamp
        );

        jdbcTemplate.update(
            "insert into account (id, owner_user_id, balance, currency, version, created_at) values (?, ?, ?, ?, ?, ?)",
            1001L, 101L, "10500.00", "KZT", 0L, timestamp
        );
        jdbcTemplate.update(
            "insert into account (id, owner_user_id, balance, currency, version, created_at) values (?, ?, ?, ?, ?, ?)",
            1002L, 102L, "9500.00", "KZT", 0L, timestamp
        );

        jdbcTemplate.update(
            """
                insert into payment (
                    id, owner_user_id, owner_account_id, receiver_user_id, receiver_account_id,
                    amount, currency, status, description, flagged, created_at, confirmed_at, version
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            2001L, 101L, 1001L, 102L, 1002L,
            "7000.00", "KZT", "FLAGGED", "High amount transfer", true, timestamp, null, 0L
        );

        jdbcTemplate.update(
            "insert into fraud_flag (id, payment_id, risk_level, reason, flagged_by, manual, created_at) values (?, ?, ?, ?, ?, ?, ?)",
            4001L, 2001L, "HIGH", "Large amount threshold exceeded", null, false, timestamp
        );
    }
}
