package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.entity.StatutoryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatutoryTaskRepository extends JpaRepository<StatutoryTask, Long> {

    Optional<StatutoryTask> findByCode(String code);

    List<StatutoryTask> findAllByOrderByIdAsc();
}
