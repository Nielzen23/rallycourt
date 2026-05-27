package com.rallycourt.payment.repository;

import com.rallycourt.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationIdIn(Collection<Long> reservationIds);
}
