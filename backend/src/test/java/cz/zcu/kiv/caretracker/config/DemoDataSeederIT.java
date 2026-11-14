package cz.zcu.kiv.caretracker.config;

import cz.zcu.kiv.caretracker.repository.PictureRepository;
import cz.zcu.kiv.caretracker.service.PictureService;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import cz.zcu.kiv.caretracker.support.TestImages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeder of the demo and dev profiles, run on the empty test database. It is built here by hand, so the
 * shared test context (profile test, which never seeds) stays as it is.
 */
class DemoDataSeederIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PictureService pictureService;

    @Autowired
    private PictureRepository pictureRepository;

    @TempDir
    private Path portraits;

    private DemoDataSeeder seeder() throws IOException {
        Files.createDirectories(portraits.resolve("clients"));
        Files.createDirectories(portraits.resolve("employees"));
        Files.write(portraits.resolve("clients/010001.jpg"), TestImages.jpeg(640, 480));
        Files.write(portraits.resolve("employees/3.jpg"), TestImages.jpeg(600, 600));
        Files.write(portraits.resolve("clients/999999.jpg"), TestImages.jpeg(64, 64));
        return new DemoDataSeeder(jdbcTemplate, pictureService, pictureRepository, portraits.toUri().toString());
    }

    @Test
    void fillsAnEmptyDatabaseOnceWithBothOrganizations() throws IOException {
        DemoDataSeeder seeder = seeder();

        assertThat(seeder.seedIfEmpty()).isTrue();

        assertThat(count("SELECT COUNT(*) FROM organization")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM client WHERE organization_id = 1")).isGreaterThanOrEqualTo(340);
        assertThat(count("SELECT COUNT(*) FROM performed_task")).isPositive();
        assertThat(jdbcTemplate.queryForList("SELECT username FROM app_user WHERE username IS NOT NULL", String.class))
                .contains("superadmin", "admin", "coordinator", "caregiver", "client");

        Map<String, Object> coordinatorOf02 = jdbcTemplate.queryForMap("""
                SELECT e.role::text AS employee_role, u.role::text AS account_role, u.username
                FROM department d JOIN employee e ON e.id = d.coordinator_id JOIN app_user u ON u.employee_id = e.id
                WHERE d.organization_id = 1 AND d.department_number = 2""");
        assertThat(coordinatorOf02).containsEntry("employee_role", "MANAGER")
                .containsEntry("account_role", "ADMIN")
                .containsEntry("username", "admin");

        double activeWithPlan = count("SELECT COUNT(*) FROM client c WHERE c.organization_id = 1 AND c.active "
                + "AND EXISTS (SELECT 1 FROM individual_plan ip WHERE ip.client_id = c.id)");
        double active = count("SELECT COUNT(*) FROM client WHERE organization_id = 1 AND active");
        assertThat(activeWithPlan / active).isBetween(0.55, 0.65);

        assertThat(count("SELECT COUNT(*) FROM picture")).isEqualTo(2);
        assertThat(count("""
                SELECT COUNT(*) FROM client c JOIN picture p ON p.id = c.picture_id
                WHERE c.personal_number = '010001' AND c.organization_id = 1
                  AND p.data IS NOT NULL AND p.thumbnail IS NOT NULL""")).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*) FROM employee e JOIN picture p ON p.id = e.picture_id
                WHERE e.id = 3 AND p.data IS NOT NULL AND p.thumbnail IS NOT NULL""")).isEqualTo(1);

        long clients = count("SELECT COUNT(*) FROM client");
        long performedTasks = count("SELECT COUNT(*) FROM performed_task");

        assertThat(seeder.seedIfEmpty()).isFalse();

        assertThat(count("SELECT COUNT(*) FROM organization")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM client")).isEqualTo(clients);
        assertThat(count("SELECT COUNT(*) FROM performed_task")).isEqualTo(performedTasks);
        assertThat(count("SELECT COUNT(*) FROM picture")).isEqualTo(2);
    }

    private long count(String query) {
        return jdbcTemplate.queryForObject(query, Long.class);
    }
}
