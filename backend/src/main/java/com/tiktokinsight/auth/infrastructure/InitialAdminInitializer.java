package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.domain.PasswordPolicy;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminInitializer.class);
    private final AuthProperties properties;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public InitialAdminInitializer(
            AuthProperties properties,
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.InitialAdmin admin = properties.initialAdmin();
        if (admin == null || !admin.isConfigured()) {
            LOGGER.info("event=initial_admin_skipped message=Initial administrator is not configured");
            return;
        }
        if (!PasswordPolicy.isValid(admin.password())) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD does not satisfy the password policy");
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            LOGGER.info("event=initial_admin_exists message=Initial administrator already exists");
            return;
        }
        userRepository.create(
                email,
                admin.username().trim(),
                passwordEncoder.encode(admin.password()),
                UserRole.ADMIN,
                UserStatus.ENABLED,
                clock.instant()
        );
        LOGGER.info("event=initial_admin_created message=Initial administrator created");
    }
}
