package com.bookingcore.service.clientbooking;

import com.bookingcore.modules.merchant.ResourceItem;
import java.time.LocalDateTime;

public record ClientBookingValidationContext(ResourceItem resource, LocalDateTime startAt) {}
