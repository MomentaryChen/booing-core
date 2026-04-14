package com.bookingcore.service.clientbooking;

import com.bookingcore.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StrictHalfHourClientBookingValidationStrategy implements ClientBookingValidationStrategy {
  @Override
  public String resourceType() {
    return "STRICT_HALF_HOUR";
  }

  @Override
  public void validate(ClientBookingValidationContext context) {
    int minute = context.startAt().getMinute();
    if (minute != 0 && minute != 30) {
      throw new ApiException(
          "Booking validation rejected by strategy",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "BOOKING_RULE_REJECTED");
    }
  }
}
