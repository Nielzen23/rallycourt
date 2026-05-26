package com.rallycourt.court.repository;

import com.rallycourt.court.entity.VenueType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueTypeRepository extends JpaRepository<VenueType, Long> {

    Optional<VenueType> findByCode(String code);
}
