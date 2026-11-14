package cz.zcu.kiv.caretracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

import cz.zcu.kiv.caretracker.repository.PictureRepository;
import cz.zcu.kiv.caretracker.service.PictureService;

/** The demo and local development start with the demo data when the database is empty. */
@Configuration
@Profile({"demo", "dev"})
public class DemoDataConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);

    @Bean
    public DemoDataSeeder demoDataSeeder(JdbcTemplate jdbcTemplate, PictureService pictureService, PictureRepository pictureRepository) {
        return new DemoDataSeeder(jdbcTemplate, pictureService, pictureRepository, DemoDataSeeder.PORTRAITS_LOCATION);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedEmptyDatabase(ApplicationReadyEvent event) {
        try {
            event.getApplicationContext().getBean(DemoDataSeeder.class).seedIfEmpty();
        } catch (RuntimeException e) {
            log.error("Demo data: seeding failed", e);
        }
    }
}
