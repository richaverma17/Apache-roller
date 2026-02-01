package org.apache.roller.weblogger.checker;  // ← adjust package to match your structure

import org.apache.roller.weblogger.business.*;
import org.apache.roller.weblogger.pojos.*;
import org.apache.roller.weblogger.util.RollerMessages;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-file "smoke + core health check" for Apache Roller backend.
 * Validates that:
 *   - Spring context loads (beans, no cyclic deps, persistence.xml ok)
 *   - WebloggerFactory / core managers initialize
 *   - Basic configuration is readable
 *   - Minimal create-user → create-weblog → create-post flow works
 *
 * Requirements in pom.xml (test scope):
 *   - org.springframework:spring-test
 *   - org.junit.jupiter:junit-jupiter
 *   - com.h2database:h2
 *   - org.mockito:mockito-core   (optional, not used here)
 *
 * Place roller-custom.properties in src/test/resources with H2 overrides:
 *   database.configurationType = jdbc
 *   database.jdbc.url = jdbc:h2:mem:roller_test;DB_CLOSE_DELAY=-1;MODE=MySQL
 *   database.jdbc.driverClassName = org.h2.Driver
 *   database.jdbc.username = sa
 *   database.jdbc.password =
 *   hibernate.dialect = org.hibernate.dialect.H2Dialect
 *   hibernate.hbm2ddl.auto = create-drop
 *   roller.setup.db=false   (if present — skip installer checks)
 *
 * Run with:
 *   mvn clean test -Dtest=RollerSmokeHealthCheck
 *   or
 *   mvn test -Dtest=RollerSmokeHealthCheck#*
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        // Most common locations in Apache Roller (check your web.xml / RollerContext)
        "classpath:/applicationContext.xml",
        "classpath:/spring/applicationContext-business.xml",
        "classpath:/spring/applicationContext-persistence.xml",
        "classpath:/spring/applicationContext-security.xml",
        // If your version uses different names/paths, adjust here.
        // Alternative wildcard (sometimes works): "classpath*:/spring/**/*.xml"
})
@TestPropertySource(locations = "classpath:roller-custom.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Apache Roller Core Smoke / Health Check")
class RollerSmokeHealthCheck {

    @Autowired
    private Weblogger weblogger;  // main facade — should be wired if context is correct

    private User testUser;
    private Weblog testWeblog;

    @Test
    @Order(1)
    @DisplayName("1. Spring ApplicationContext loads successfully")
    void contextLoads() {
        assertNotNull(weblogger, "Weblogger facade bean should be injected → context is alive");
    }

    @Test
    @Order(2)
    @DisplayName("2. Core managers are available via Weblogger facade")
    void coreManagersArePresent() {
        assertNotNull(weblogger.getUserManager());
        assertNotNull(weblogger.getWeblogManager());
        assertNotNull(weblogger.getWeblogEntryManager());
        assertNotNull(weblogger.getPlanetManager());  // optional — but good to know planet subsystem ok
    }

    @Test
    @Order(3)
    @DisplayName("3. Configuration system is readable")
    void configurationIsAccessible() {
        // Most basic check — if this throws → roller.properties / custom props broken
        String siteName = weblogger.getConfig().getProperty("site.name");
        assertNotNull(siteName, "site.name property should be resolvable");
        // You can add more property assertions if desired
    }

    @Test
    @Order(4)
    @DisplayName("4. Minimal user → weblog → post creation smoke test")
    void minimalBusinessFlowWorks() throws Exception {
        // 4.1 Create user
        User user = new User();
        user.setUserName("smokeuser_" + System.currentTimeMillis());
        user.setScreenName("Smoke Tester");
        user.setFullName("Smoke Test User");
        user.setEmailAddress("smoke@example.local");
        user.setPassword("password");           // plain text ok for test DB
        user.setDateCreated(new Date());
        user.setEnabled(true);
        user.setActivated(true);

        testUser = weblogger.getUserManager().addUser(user);
        assertNotNull(testUser.getId(), "User should have received an ID");

        // 4.2 Create weblog (blog)
        Weblog weblog = new Weblog();
        weblog.setName("Smoke Test Blog");
        weblog.setHandle("smoke-" + System.currentTimeMillis());
        weblog.setCreator(testUser);
        weblog.setDateCreated(new Date());
        weblog.setEnabled(true);
        weblog.setActive(true);

        testWeblog = weblogger.getWeblogManager().addWeblog(weblog);
        assertNotNull(testWeblog.getId(), "Weblog should have received an ID");

        // 4.3 Create one entry/post
        WeblogEntry entry = new WeblogEntry();
        entry.setWeblog(testWeblog);
        entry.setCreator(testUser);
        entry.setTitle("First Smoke Post");
        entry.setText("This is an automated smoke test post.");
        entry.setPubTime(new Date());
        entry.setStatus(WeblogEntry.PubStatus.PUBLISHED);
        entry.setLocale("en");

        WeblogEntry savedEntry = weblogger.getWeblogEntryManager().saveWeblogEntry(entry);
        weblogger.getWeblogEntryManager().release();  // flush if needed

        assertNotNull(savedEntry.getId(), "WeblogEntry should have received an ID");
        assertEquals("First Smoke Post", savedEntry.getTitle());
    }

    @AfterEach
    void cleanup() throws Exception {
        // Optional: try to remove test data (best-effort — failures here are not critical)
        if (testWeblog != null) {
            try { weblogger.getWeblogManager().removeWeblog(testWeblog); } catch (Exception ignored) {}
        }
        if (testUser != null) {
            try { weblogger.getUserManager().removeUser(testUser); } catch (Exception ignored) {}
        }
    }
}