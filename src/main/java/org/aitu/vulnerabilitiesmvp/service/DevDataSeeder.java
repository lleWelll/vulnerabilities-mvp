package org.aitu.vulnerabilitiesmvp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.aitu.vulnerabilitiesmvp.entity.Account;
import org.aitu.vulnerabilitiesmvp.entity.FraudFlag;
import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.aitu.vulnerabilitiesmvp.entity.User;
import org.aitu.vulnerabilitiesmvp.enums.CurrencyCode;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;
import org.aitu.vulnerabilitiesmvp.enums.RiskLevel;
import org.aitu.vulnerabilitiesmvp.enums.Role;
import org.aitu.vulnerabilitiesmvp.repository.AccountRepository;
import org.aitu.vulnerabilitiesmvp.repository.FraudFlagRepository;
import org.aitu.vulnerabilitiesmvp.repository.PaymentRepository;
import org.aitu.vulnerabilitiesmvp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("dev")
@Transactional
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final List<SeedUserSpec> SEED_USERS = List.of(
        new SeedUserSpec("test_client_1", "SEED_USER_TEST_CLIENT_1_PASSWORD", Role.CLIENT),
        new SeedUserSpec("test_client_2", "SEED_USER_TEST_CLIENT_2_PASSWORD", Role.CLIENT),
        new SeedUserSpec("test_operator", "SEED_USER_TEST_OPERATOR_PASSWORD", Role.OPERATOR)
    );

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DevDataSeeder(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PaymentRepository paymentRepository,
        FraudFlagRepository fraudFlagRepository,
        PasswordEncoder passwordEncoder,
        Environment environment
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.fraudFlagRepository = fraudFlagRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean anySeedUserExists = SEED_USERS.stream().anyMatch(spec -> userRepository.existsByUsername(spec.username()));
        boolean allSeedUsersExist = SEED_USERS.stream().allMatch(spec -> userRepository.existsByUsername(spec.username()));

        if (allSeedUsersExist) {
            return;
        }

        if (anySeedUserExists) {
            throw new IllegalStateException(
                "Partial dev seed data detected. Reset the database or remove existing seed users before rerunning the dev seeder."
            );
        }

        Instant seededAt = Instant.now();
        List<String> createdUsernames = new ArrayList<>();

        User testClient1 = createUser(SEED_USERS.get(0), createdUsernames);
        User testClient2 = createUser(SEED_USERS.get(1), createdUsernames);
        createUser(SEED_USERS.get(2), createdUsernames);

        Account testClient1Kzt = createAccount(testClient1, amount("10500.00"), CurrencyCode.KZT, seededAt);
        Account testClient2Kzt = createAccount(testClient2, amount("9500.00"), CurrencyCode.KZT, seededAt);
        createAccount(testClient1, amount("500.00"), CurrencyCode.USD, seededAt);

        createPayment(
            testClient1,
            testClient1Kzt,
            testClient2,
            testClient2Kzt,
            amount("1000.00"),
            CurrencyCode.KZT,
            PaymentStatus.CONFIRMED,
            "Seed confirmed transfer",
            false,
            seededAt,
            seededAt
        );
        Payment flaggedTransfer = createPayment(
            testClient1,
            testClient1Kzt,
            testClient2,
            testClient2Kzt,
            amount("7000.00"),
            CurrencyCode.KZT,
            PaymentStatus.FLAGGED,
            "High amount transfer",
            true,
            seededAt,
            null
        );
        createPayment(
            testClient2,
            testClient2Kzt,
            testClient1,
            testClient1Kzt,
            amount("1500.00"),
            CurrencyCode.KZT,
            PaymentStatus.CONFIRMED,
            "Bob to Alice test transfer",
            false,
            seededAt,
            seededAt
        );

        createFraudFlag(flaggedTransfer, seededAt);

        log.warn("Created dev seed users: {}", createdUsernames);
    }

    private User createUser(SeedUserSpec spec, List<String> createdUsernames) {
        User user = new User();
        user.setUsername(spec.username());
        user.setPasswordHash(passwordEncoder.encode(readRequiredPassword(spec.envVariable())));
        user.setRole(spec.role());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        createdUsernames.add(savedUser.getUsername());
        return savedUser;
    }

    private Account createAccount(User owner, BigDecimal balance, CurrencyCode currency, Instant createdAt) {
        Account account = new Account();
        account.setOwner(owner);
        account.setBalance(balance);
        account.setCurrency(currency);
        account.setCreatedAt(createdAt);
        return accountRepository.save(account);
    }

    private Payment createPayment(
        User ownerUser,
        Account ownerAccount,
        User receiverUser,
        Account receiverAccount,
        BigDecimal amount,
        CurrencyCode currency,
        PaymentStatus status,
        String description,
        boolean flagged,
        Instant createdAt,
        Instant confirmedAt
    ) {
        Payment payment = new Payment();
        payment.setOwnerUser(ownerUser);
        payment.setOwnerAccount(ownerAccount);
        payment.setReceiverUser(receiverUser);
        payment.setReceiverAccount(receiverAccount);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setDescription(description);
        payment.setFlagged(flagged);
        payment.setCreatedAt(createdAt);
        payment.setConfirmedAt(confirmedAt);
        return paymentRepository.save(payment);
    }

    private FraudFlag createFraudFlag(Payment payment, Instant createdAt) {
        FraudFlag fraudFlag = new FraudFlag();
        fraudFlag.setPayment(payment);
        fraudFlag.setRiskLevel(RiskLevel.HIGH);
        fraudFlag.setReason("Large amount threshold exceeded");
        fraudFlag.setFlaggedBy(null);
        fraudFlag.setManual(false);
        fraudFlag.setCreatedAt(createdAt);
        return fraudFlagRepository.save(fraudFlag);
    }

    private String readRequiredPassword(String envVariable) {
        String password = System.getenv(envVariable);
        if (!StringUtils.hasText(password)) {
            password = environment.getProperty(envVariable);
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException(
                "Missing required seed password '" + envVariable + "'. Define it in the project-root secret.env file."
            );
        }
        return password;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private record SeedUserSpec(String username, String envVariable, Role role) {
    }
}
