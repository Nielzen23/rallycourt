package com.rallycourt.reservation.entity;

import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Reservation extends AbstractEntity {

    private Long courtId;
    private String reservedBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
}
