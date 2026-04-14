package com.bookingcore.service.clientbooking;

import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ClientBookingSpecifications {

  private ClientBookingSpecifications() {}

  /** Tab: upcoming | past | cancelled */
  public static Specification<Booking> forClientMyBookingsTab(Long platformUserId, String tab) {
    String normalized = tab == null || tab.isBlank() ? "upcoming" : tab.trim().toLowerCase();
    return (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      preds.add(
          cb.equal(root.join("platformUser", JoinType.INNER).get("id"), platformUserId));
      LocalDateTime now = LocalDateTime.now();
      switch (normalized) {
        case "cancelled":
          preds.add(cb.equal(root.get("status"), BookingStatus.CANCELLED));
          break;
        case "past":
          preds.add(cb.lessThan(root.get("startAt"), now));
          preds.add(cb.notEqual(root.get("status"), BookingStatus.CANCELLED));
          break;
        case "upcoming":
        default:
          preds.add(cb.greaterThanOrEqualTo(root.get("startAt"), now));
          preds.add(cb.notEqual(root.get("status"), BookingStatus.CANCELLED));
          break;
      }
      query.distinct(true);
      return cb.and(preds.toArray(Predicate[]::new));
    };
  }
}
