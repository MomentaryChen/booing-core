package com.bookingcore.service.clientbooking;

public interface ClientBookingValidationStrategy {
  String resourceType();

  void validate(ClientBookingValidationContext context);
}
