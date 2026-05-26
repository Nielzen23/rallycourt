package com.rallycourt.court.repository;

import com.rallycourt.court.entity.CourtType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtTypeRepository extends JpaRepository<CourtType, Long> {

    Optional<CourtType> findByCode(String code);
}
