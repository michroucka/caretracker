package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.entity.StatutoryHourlyLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutoryHourlyLimitRepository extends JpaRepository<StatutoryHourlyLimit, Long> {

    Optional<StatutoryHourlyLimit> findFirstByOrderByIdAsc();
}
