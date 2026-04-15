package com.bookingcore.service;

import java.util.UUID;
import com.bookingcore.api.ApiDtos.BookingAssignmentCommandRequest;
import com.bookingcore.api.ApiDtos.BookingAssignmentSummary;
import com.bookingcore.api.ApiDtos.StaffCandidateSummary;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingRepository;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.merchant.ResourceItemRepository;
import com.bookingcore.modules.merchant.ResourceStaffAssignment;
import com.bookingcore.modules.merchant.ResourceStaffAssignmentRepository;
import com.bookingcore.modules.merchant.ResourceStaffAssignmentStatus;
import com.bookingcore.modules.merchant.TeamMember;
import com.bookingcore.modules.merchant.TeamMemberRepository;
import com.bookingcore.modules.merchant.TeamMemberStatus;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantBookingAssignmentService {
  private static final Set<ResourceStaffAssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
      EnumSet.of(ResourceStaffAssignmentStatus.RESERVED, ResourceStaffAssignmentStatus.CONFIRMED);

  private final BookingRepository bookingRepository;
  private final ResourceItemRepository resourceItemRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final PlatformUserRepository platformUserRepository;
  private final ResourceStaffAssignmentRepository assignmentRepository;
  private final PlatformAuditService platformAuditService;
  private final MerchantAccessService merchantAccessService;
  private final ObjectMapper objectMapper;

  public MerchantBookingAssignmentService(
      BookingRepository bookingRepository,
      ResourceItemRepository resourceItemRepository,
      TeamMemberRepository teamMemberRepository,
      PlatformUserRepository platformUserRepository,
      ResourceStaffAssignmentRepository assignmentRepository,
      PlatformAuditService platformAuditService,
      MerchantAccessService merchantAccessService,
      ObjectMapper objectMapper) {
    this.bookingRepository = bookingRepository;
    this.resourceItemRepository = resourceItemRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.platformUserRepository = platformUserRepository;
    this.assignmentRepository = assignmentRepository;
    this.platformAuditService = platformAuditService;
    this.merchantAccessService = merchantAccessService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public BookingAssignmentSummary assign(UUID merchantId, UUID bookingId, BookingAssignmentCommandRequest request) {
    Booking booking = requireBooking(merchantId, bookingId);
    assertAssignableBookingStatus(booking.getStatus());
    ResourceItem resource = requireResource(merchantId, request.resourceId());
    UUID staffId = request.staffId();
    if (staffId == null) {
      throw new ApiException("staffId is required", HttpStatus.BAD_REQUEST);
    }
    PlatformUser staff = requireStaffInResourceScope(merchantId, resource, staffId);

    assignmentRepository
        .findByMerchantIdAndBookingIdAndStatusIn(merchantId, bookingId, ACTIVE_ASSIGNMENT_STATUSES)
        .ifPresent(
            existing -> {
              throw new ApiException("Booking already has an active assignment", HttpStatus.CONFLICT);
            });

    assertNoStaffSlotConflict(merchantId, booking, staff.getId(), null);
    ResourceStaffAssignment assignment = new ResourceStaffAssignment();
    assignment.setMerchant(booking.getMerchant());
    assignment.setBooking(booking);
    assignment.setResource(resource);
    assignment.setStaffUser(staff);
    assignment.setStatus(ResourceStaffAssignmentStatus.RESERVED);
    assignment.setStartAt(booking.getStartAt());
    assignment.setEndAt(booking.getEndAt());
    assignment.setReason(trimToNull(request.reason()));
    String actor = merchantAccessService.currentUsername();
    assignment.setCreatedBy(actor);
    assignment.setUpdatedBy(actor);
    ResourceStaffAssignment saved = assignmentRepository.save(assignment);
    platformAuditService.recordForCurrentUser(
        "booking.assignment.assign",
        "resource_staff_assignment",
        saved.getId(),
        "bookingId=" + bookingId + ",staffId=" + staffId + ",resourceId=" + resource.getId());
    return toSummary(saved);
  }

  @Transactional
  public BookingAssignmentSummary reassign(
      UUID merchantId, UUID bookingId, BookingAssignmentCommandRequest request) {
    String reason = trimToNull(request.reason());
    if (reason == null) {
      throw new ApiException("reason is required", HttpStatus.BAD_REQUEST);
    }
    Booking booking = requireBooking(merchantId, bookingId);
    assertAssignableBookingStatus(booking.getStatus());
    ResourceItem resource = requireResource(merchantId, request.resourceId());
    if (request.newStaffId() == null) {
      throw new ApiException("newStaffId is required", HttpStatus.BAD_REQUEST);
    }
    PlatformUser newStaff = requireStaffInResourceScope(merchantId, resource, request.newStaffId());
    ResourceStaffAssignment current =
        assignmentRepository
            .findByMerchantIdAndBookingIdAndStatusIn(merchantId, bookingId, ACTIVE_ASSIGNMENT_STATUSES)
            .orElseThrow(() -> new ApiException("Active assignment not found", HttpStatus.CONFLICT));
    assertNoStaffSlotConflict(merchantId, booking, newStaff.getId(), current.getId());

    String actor = merchantAccessService.currentUsername();
    current.setStatus(ResourceStaffAssignmentStatus.RELEASED);
    current.setReason(reason);
    current.setUpdatedBy(actor);
    current.setUpdatedAt(LocalDateTime.now());
    assignmentRepository.save(current);

    ResourceStaffAssignment replacement = new ResourceStaffAssignment();
    replacement.setMerchant(booking.getMerchant());
    replacement.setBooking(booking);
    replacement.setResource(resource);
    replacement.setStaffUser(newStaff);
    replacement.setStatus(ResourceStaffAssignmentStatus.RESERVED);
    replacement.setStartAt(booking.getStartAt());
    replacement.setEndAt(booking.getEndAt());
    replacement.setReason(reason);
    replacement.setCreatedBy(actor);
    replacement.setUpdatedBy(actor);
    ResourceStaffAssignment saved = assignmentRepository.save(replacement);
    platformAuditService.recordForCurrentUser(
        "booking.assignment.reassign",
        "resource_staff_assignment",
        saved.getId(),
        "fromStaffId=" + current.getStaffUser().getId() + ",toStaffId=" + newStaff.getId() + ",reason=" + reason);
    return toSummary(saved);
  }

  @Transactional
  public BookingAssignmentSummary release(
      UUID merchantId, UUID bookingId, BookingAssignmentCommandRequest request) {
    String reason = trimToNull(request.reason());
    if (reason == null) {
      throw new ApiException("reason is required", HttpStatus.BAD_REQUEST);
    }
    ResourceStaffAssignment current =
        assignmentRepository
            .findByMerchantIdAndBookingIdAndStatusIn(merchantId, bookingId, ACTIVE_ASSIGNMENT_STATUSES)
            .orElseThrow(() -> new ApiException("Active assignment not found", HttpStatus.CONFLICT));
    current.setStatus(ResourceStaffAssignmentStatus.RELEASED);
    current.setReason(reason);
    current.setUpdatedBy(merchantAccessService.currentUsername());
    current.setUpdatedAt(LocalDateTime.now());
    ResourceStaffAssignment saved = assignmentRepository.save(current);
    platformAuditService.recordForCurrentUser(
        "booking.assignment.release",
        "resource_staff_assignment",
        saved.getId(),
        "bookingId=" + bookingId + ",staffId=" + saved.getStaffUser().getId() + ",reason=" + reason);
    return toSummary(saved);
  }

  @Transactional(readOnly = true)
  public List<StaffCandidateSummary> listStaffCandidates(
      UUID merchantId, UUID resourceId, LocalDateTime startAt, LocalDateTime endAt) {
    if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
      throw new ApiException("startAt/endAt is invalid", HttpStatus.BAD_REQUEST);
    }
    ResourceItem resource = requireResource(merchantId, resourceId);
    List<UUID> staffIds = parseAssignedStaffIds(resource.getAssignedStaffIdsJson());
    if (staffIds.isEmpty()) {
      return List.of();
    }
    Set<UUID> activeStaffIds =
        teamMemberRepository
            .findByMerchantIdAndPlatformUserIdInAndStatus(merchantId, staffIds, TeamMemberStatus.ACTIVE)
            .stream()
            .map(member -> member.getPlatformUser().getId())
            .collect(java.util.stream.Collectors.toSet());

    return staffIds.stream()
        .map(
            staffId -> {
              PlatformUser user =
                  platformUserRepository
                      .findById(staffId)
                      .orElse(null);
              String username = user == null ? "#" + staffId : user.getUsername();
              boolean active = activeStaffIds.contains(staffId);
              if (!active) {
                return new StaffCandidateSummary(staffId, username, false, false, "STAFF_NOT_ACTIVE");
              }
              boolean conflict =
                  !assignmentRepository
                      .findByMerchantIdAndStaffUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                          merchantId, staffId, ACTIVE_ASSIGNMENT_STATUSES, endAt, startAt)
                      .isEmpty();
              return new StaffCandidateSummary(
                  staffId, username, true, !conflict, conflict ? "STAFF_SLOT_CONFLICT" : null);
            })
        .toList();
  }

  private Booking requireBooking(UUID merchantId, UUID bookingId) {
    return bookingRepository
        .findByIdAndMerchantId(bookingId, merchantId)
        .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
  }

  private ResourceItem requireResource(UUID merchantId, UUID resourceId) {
    return resourceItemRepository
        .findByIdAndMerchantId(resourceId, merchantId)
        .orElseThrow(() -> new ApiException("Resource not found", HttpStatus.NOT_FOUND));
  }

  private PlatformUser requireStaffInResourceScope(UUID merchantId, ResourceItem resource, UUID staffId) {
    List<UUID> assignableIds = parseAssignedStaffIds(resource.getAssignedStaffIdsJson());
    if (!assignableIds.contains(staffId)) {
      throw new ApiException("Staff is not assignable for this resource", HttpStatus.BAD_REQUEST);
    }
    List<TeamMember> activeMembers =
        teamMemberRepository.findByMerchantIdAndPlatformUserIdInAndStatus(
            merchantId, List.of(staffId), TeamMemberStatus.ACTIVE);
    if (activeMembers.isEmpty()) {
      throw new ApiException("Staff not found in this merchant", HttpStatus.NOT_FOUND);
    }
    return activeMembers.get(0).getPlatformUser();
  }

  private void assertNoStaffSlotConflict(
      UUID merchantId, Booking booking, UUID staffId, Long ignoreAssignmentId) {
    List<ResourceStaffAssignment> conflicts =
        assignmentRepository.findByMerchantIdAndStaffUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            merchantId, staffId, ACTIVE_ASSIGNMENT_STATUSES, booking.getEndAt(), booking.getStartAt());
    boolean hasConflict =
        conflicts.stream()
            .anyMatch(
                existing ->
                    !existing.getBooking().getId().equals(booking.getId())
                        && (ignoreAssignmentId == null || !existing.getId().equals(ignoreAssignmentId)));
    if (hasConflict) {
      throw new ApiException("Staff slot conflict", HttpStatus.CONFLICT);
    }
  }

  private void assertAssignableBookingStatus(BookingStatus status) {
    Set<BookingStatus> allowed =
        EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.IN_SERVICE);
    if (!allowed.contains(status)) {
      throw new ApiException("Booking status does not allow assignment command", HttpStatus.CONFLICT);
    }
  }

  private List<UUID> parseAssignedStaffIds(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      List<UUID> parsed = objectMapper.readValue(raw, new TypeReference<List<UUID>>() {});
      return parsed.stream()
          .filter(id -> id != null)
          .collect(
              java.util.stream.Collectors.collectingAndThen(
                  java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private BookingAssignmentSummary toSummary(ResourceStaffAssignment assignment) {
    return new BookingAssignmentSummary(
        assignment.getId(),
        assignment.getBooking().getId(),
        assignment.getMerchant().getId(),
        assignment.getResource().getId(),
        assignment.getStaffUser().getId(),
        assignment.getStaffUser().getUsername(),
        assignment.getStatus(),
        assignment.getReason(),
        assignment.getStartAt(),
        assignment.getEndAt());
  }
}

