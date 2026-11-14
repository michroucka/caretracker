package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.entity.SupportedHousing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportedHousingRepository extends JpaRepository<SupportedHousing, Long> {
    List<SupportedHousing> findByDepartmentIdOrderByNameAsc(Long departmentId);
    Optional<SupportedHousing> findByIdAndDepartmentId(Long id, Long departmentId);
}
