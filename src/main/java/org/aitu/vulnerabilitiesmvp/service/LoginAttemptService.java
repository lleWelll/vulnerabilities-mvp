package org.aitu.vulnerabilitiesmvp.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    // OWASP-10: Authentication Failures - без ограничения попыток входа пароль можно подбирать онлайн.
    // Исправление: ведём in-memory счётчик неудачных попыток и временно блокируем повторные проверки пароля.
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailedLoginAttempts;
    private final int loginLockoutMinutes;

    public LoginAttemptService(AppProperties appProperties) {
        this.maxFailedLoginAttempts = appProperties.getSecurity().getMaxFailedLoginAttempts();
        this.loginLockoutMinutes = appProperties.getSecurity().getLoginLockoutMinutes();
    }

    public boolean isBlocked(String username) {
        AttemptState state = attempts.get(username);
        if (state == null || state.blockedUntil == null) {
            return false;
        }
        if (state.blockedUntil.isAfter(Instant.now())) {
            return true;
        }
        attempts.remove(username, state);
        return false;
    }

    public void recordFailure(String username) {
        attempts.compute(username, (key, state) -> {
            AttemptState nextState = state == null ? new AttemptState() : state;
            if (nextState.blockedUntil != null && nextState.blockedUntil.isAfter(Instant.now())) {
                return nextState;
            }
            nextState.failures++;
            if (nextState.failures >= maxFailedLoginAttempts) {
                nextState.blockedUntil = Instant.now().plusSeconds(loginLockoutMinutes * 60L);
            }
            return nextState;
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    private static final class AttemptState {
        private int failures;
        private Instant blockedUntil;
    }
}
