package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.Settlement;
import com.alvexo.adminportal.entity.SettlementStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Grouped totals for the reports, built with the Criteria API so they apply
 * exactly the same optional filters (as a {@link Specification}) as the row
 * queries — the totals can never disagree with the rows on screen.
 */
@Repository
public class ReportQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Rows of [status, count] over every booking matching the filter. */
    public List<Object[]> bookingStatusCounts(Specification<Booking> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Booking> b = query.from(Booking.class);
        query.multiselect(b.get("status"), cb.count(b))
                .where(filter.toPredicate(b, query, cb))
                .groupBy(b.get("status"));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new Object[]{t.get(0, BookingStatus.class), t.get(1, Long.class)})
                .toList();
    }

    /**
     * Rows of [status, count, advanceCollected, advanceHandlingFee, netPay, tcsDeduction, finalPayout]
     * over every settlement matching the filter.
     */
    public List<Object[]> settlementStatusTotals(Specification<Settlement> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Settlement> s = query.from(Settlement.class);
        query.multiselect(s.get("status"), cb.count(s),
                        sum(cb, s, "advanceCollected"), sum(cb, s, "advanceHandlingFee"), sum(cb, s, "netPay"),
                        sum(cb, s, "tcsDeduction"), sum(cb, s, "finalPayout"))
                .where(filter.toPredicate(s, query, cb))
                .groupBy(s.get("status"));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new Object[]{t.get(0, SettlementStatus.class), t.get(1, Long.class),
                        t.get(2, BigDecimal.class), t.get(3, BigDecimal.class), t.get(4, BigDecimal.class),
                        t.get(5, BigDecimal.class), t.get(6, BigDecimal.class)})
                .toList();
    }

    /** Number of distinct workshops with a settlement matching the filter. */
    public long countSettlementWorkshops(Specification<Settlement> filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Settlement> s = query.from(Settlement.class);
        query.select(cb.countDistinct(s.get("mechanic").get("id"))).where(filter.toPredicate(s, query, cb));
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * One page of [mechanicId, bookings, advanceCollected, advanceHandlingFee, netPay, tcsDeduction,
     * finalPayout, paidAmount, outstandingAmount, failedOrOnHold], largest final payout first.
     */
    public List<Object[]> settlementsByWorkshop(Specification<Settlement> filter, int offset, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Settlement> s = query.from(Settlement.class);
        Expression<Long> mechanicId = s.get("mechanic").get("id");
        Expression<SettlementStatus> status = s.get("status");
        Expression<BigDecimal> finalPayout = s.get("finalPayout");

        Expression<BigDecimal> paid = cb.sum(cb.<BigDecimal>selectCase()
                .when(cb.equal(status, SettlementStatus.PAID), finalPayout)
                .otherwise(BigDecimal.ZERO));
        Expression<BigDecimal> outstanding = cb.sum(cb.<BigDecimal>selectCase()
                .when(status.in(SettlementStatus.AWAITING_PAYOUT, SettlementStatus.PROCESSING,
                        SettlementStatus.ON_HOLD, SettlementStatus.FAILED), finalPayout)
                .otherwise(BigDecimal.ZERO));
        Expression<Integer> problem = cb.sum(cb.<Integer>selectCase()
                .when(status.in(SettlementStatus.ON_HOLD, SettlementStatus.FAILED), 1)
                .otherwise(0));
        Expression<BigDecimal> totalPayout = cb.sum(finalPayout);

        query.multiselect(mechanicId, cb.count(s),
                        sum(cb, s, "advanceCollected"), sum(cb, s, "advanceHandlingFee"), sum(cb, s, "netPay"),
                        sum(cb, s, "tcsDeduction"), totalPayout, paid, outstanding, problem)
                .where(filter.toPredicate(s, query, cb))
                .groupBy(mechanicId)
                .orderBy(cb.desc(totalPayout), cb.asc(mechanicId));
        return entityManager.createQuery(query).setFirstResult(offset).setMaxResults(limit).getResultList().stream()
                .map(t -> new Object[]{t.get(0, Long.class), t.get(1, Long.class),
                        t.get(2, BigDecimal.class), t.get(3, BigDecimal.class), t.get(4, BigDecimal.class),
                        t.get(5, BigDecimal.class), t.get(6, BigDecimal.class), t.get(7, BigDecimal.class),
                        t.get(8, BigDecimal.class), ((Number) t.get(9)).longValue()})
                .toList();
    }

    private static Expression<BigDecimal> sum(CriteriaBuilder cb, Root<Settlement> s, String attribute) {
        return cb.sum(s.<BigDecimal>get(attribute));
    }
}
