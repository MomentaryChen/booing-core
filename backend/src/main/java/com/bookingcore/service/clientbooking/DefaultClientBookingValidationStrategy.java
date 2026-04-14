package com.bookingcore.service.clientbooking;

import org.springframework.stereotype.Component;

@Component
public class DefaultClientBookingValidationStrategy implements ClientBookingValidationStrategy {
  @Override
  public String resourceType() {
    return "ROOM";
  }

  @Override
  public void validate(ClientBookingValidationContext context) {
    // No extra constraints for standard ROOM resources.
  }
}
