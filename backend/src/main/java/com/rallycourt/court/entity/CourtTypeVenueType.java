package com.rallycourt.court.entity;

import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class CourtTypeVenueType extends AbstractEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_type_id", nullable = false)
    private CourtType courtType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venue_type_id", nullable = false)
    private VenueType venueType;
}
