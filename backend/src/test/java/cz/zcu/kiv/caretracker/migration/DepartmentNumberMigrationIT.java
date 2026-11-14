package cz.zcu.kiv.caretracker.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V30 makes department_number required; invalid legacy data must fail with a message that names it. */
class DepartmentNumberMigrationIT {

    private static PostgreSQLContainer postgres;
    private JdbcTemplate jdbc;
    private Flyway beforeV30;

    @BeforeAll
    static void startDatabase() {
        postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"));
        postgres.start();
    }

    @AfterAll
    static void stopDatabase() {
        postgres.stop();
    }

    @BeforeEach
    void migrateToV29() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        beforeV30 = Flyway.configure().dataSource(dataSource).cleanDisabled(false).target("29").load();
        beforeV30.clean();
        beforeV30.migrate();
        jdbc.execute("TRUNCATE department CASCADE");
    }

    private Flyway toLatest() {
        return Flyway.configure().dataSource(jdbc.getDataSource()).load();
    }

    private long organization() {
        return jdbc.queryForObject("INSERT INTO organization (name) VALUES ('Org') RETURNING id", Long.class);
    }

    private long department(long organizationId, Integer number) {
        return jdbc.queryForObject("""
                INSERT INTO department (street, city, postal_code, organization_id, department_number, active)
                VALUES ('Ulice 1', 'Plzeň', '301 00', ?, ?, true) RETURNING id""", Long.class, organizationId, number);
    }

    @Test
    void validLegacyDataIsNumberedAfterTheHighestOne() {
        long org = organization();
        long numbered = department(org, 5);
        long first = department(org, null);
        long second = department(org, null);

        toLatest().migrate();

        assertThat(numberOf(numbered)).isEqualTo(5);
        assertThat(numberOf(first)).isEqualTo(6);
        assertThat(numberOf(second)).isEqualTo(7);
    }

    @Test
    void numberOutsideRangeFailsNamingTheDepartment() {
        long org = organization();
        long bad = department(org, 100);

        assertThatThrownBy(() -> toLatest().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("outside 1-99")
                .hasMessageContaining("department " + bad);
    }

    @Test
    void duplicateNumberFailsNamingTheOrganization() {
        long org = organization();
        department(org, 3);
        department(org, 3);

        assertThatThrownBy(() -> toLatest().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("duplicate department_number")
                .hasMessageContaining("organization " + org);
    }

    @Test
    void numberingThatWouldExceedNinetyNineFailsNamingTheOrganization() {
        long org = organization();
        department(org, 99);
        department(org, null);

        assertThatThrownBy(() -> toLatest().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("would exceed 99")
                .hasMessageContaining("organization " + org);
    }

    private int numberOf(long departmentId) {
        return jdbc.queryForObject("SELECT department_number FROM department WHERE id = ?", Integer.class, departmentId);
    }
}
