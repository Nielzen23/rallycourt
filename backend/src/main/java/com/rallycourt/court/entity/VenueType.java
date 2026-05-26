package com.rallycourt.court.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class VenueType extends AbstractEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @OneToMany(mappedBy = "venueType")
    @JsonIgnore
    private Set<CourtTypeVenueType> allowedCourtTypes = new HashSet<>();
}
