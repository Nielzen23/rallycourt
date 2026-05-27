package com.rallycourt.court.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rallycourt.auth.entity.User;
import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Court extends AbstractEntity {

    private String name;
    private String location;
    private Double latitude;
    private Double longitude;
    @Enumerated(EnumType.STRING)
    private CourtStatus status;

    @Getter(onMethod_ = @JsonIgnore)
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "court_type_id", nullable = false)
    private CourtType courtType;

    @Getter(onMethod_ = @JsonIgnore)
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "venue_type_id", nullable = false)
    private VenueType venueType;

    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal hourlyRate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @JsonProperty("courtType")
    public String getCourtTypeCode() {
        return courtType != null ? courtType.getCode() : null;
    }

    @JsonProperty("venueType")
    public String getVenueTypeCode() {
        return venueType != null ? venueType.getCode() : null;
    }

    @PrePersist
    void initializeDefaults() {
        if (status == null) {
            status = CourtStatus.AVAILABLE;
        }
        if (hourlyRate == null) {
            hourlyRate = BigDecimal.valueOf(750);
        }
    }
}
