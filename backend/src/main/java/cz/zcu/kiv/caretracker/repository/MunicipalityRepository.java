package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {
    List<Municipality> findByDepartmentIdOrderByNameAsc(Long departmentId);
    boolean existsByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);
    Optional<Municipality> findByIdAndDepartmentId(Long id, Long departmentId);
}
