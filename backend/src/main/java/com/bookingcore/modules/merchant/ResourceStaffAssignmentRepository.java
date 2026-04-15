package com.bookingcore.modules.merchant;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceStaffAssignmentRepository extends JpaRepository<ResourceStaffAssignment, Long> {
  Optional<ResourceStaffAssignment> findByMerchantIdAndBookingIdAndStatusIn(
      UUID merchantId, UUID bookingId, Collection<ResourceStaffAssignmentStatus> statuses);

  List<ResourceStaffAssignment>
      findByMerchantIdAndStaffUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
          UUID merchantId,
          UUID staffUserId,
          Collection<ResourceStaffAssignmentStatus> statuses,
          LocalDateTime endAt,
          LocalDateTime startAt);
}

