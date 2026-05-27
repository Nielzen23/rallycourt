package com.rallycourt.payment.repository;

import com.rallycourt.payment.entity.Payment;
import com.rallycourt.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationIdIn(Collection<Long> reservationIds);

    @Query("""
            select count(distinct p.reservationId)
            from Payment p
            where p.status = :status
            """)
    long countDistinctReservationIdByStatus(PaymentStatus status);
}
