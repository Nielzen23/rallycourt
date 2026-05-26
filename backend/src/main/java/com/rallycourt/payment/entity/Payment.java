package com.rallycourt.payment.entity;

import com.rallycourt.common.entity.AbstractEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Payment extends AbstractEntity {

    private Long reservationId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
