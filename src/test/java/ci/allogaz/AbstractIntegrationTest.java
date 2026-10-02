package ci.allogaz;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import ci.allogaz.identity.application.port.out.AccessTokenIssuer;
import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.identity.domain.Role;
import ci.allogaz.identity.domain.User;
import ci.allogaz.support.TestBeans;

/**
 * Base des tests d'intégration : PostgreSQL/PostGIS et Redis réels via Testcontainers.
 * Les conteneurs sont partagés entre toutes les classes de test (démarrés une seule fois).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBeans.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(System.nanoTime() % 10_000_000L);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccessTokenIssuer accessTokenIssuer;

    @Autowired
    protected ci.allogaz.support.Fixtures fixtures;

    /** Numéro mobile unique par test (évite les collisions de compteurs Redis). */
    protected static String uniquePhone() {
        return "+22507" + String.format("%08d", PHONE_SEQUENCE.incrementAndGet() % 100_000_000L);
    }

    protected User createUser(Role... roles) {
        EnumSet<Role> set = EnumSet.of(Role.BUYER);
        set.addAll(java.util.List.of(roles));
        return userRepository.save(new User(UUID.randomUUID(), new PhoneNumber(uniquePhone()), "Test", set, Instant.now()));
    }

    protected String bearer(User user) {
        return "Bearer " + accessTokenIssuer.issue(user).value();
    }
}
