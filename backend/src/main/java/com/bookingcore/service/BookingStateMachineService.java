package com.bookingcore.service;

import com.bookingcore.common.ApiException;
import com.bookingcore.modules.booking.Booking;
import com.bookingcore.modules.booking.BookingStatus;
import com.bookingcore.modules.booking.BookingTransitionEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BookingStateMachineService {

  public BookingStatus transition(Booking booking, BookingTransitionEvent event) {
    BookingStatus current = booking.getStatus();
    BookingStatus next = nextState(current, event);
    if (next == null) {
      throw new ApiException(
          "Illegal booking transition: " + current + " -> " + event,
          HttpStatus.BAD_REQUEST);
    }
    booking.setStatus(next);
    return next;
  }

  private BookingStatus nextState(BookingStatus current, BookingTransitionEvent event) {
    return switch (event) {
      case SUBMIT -> (current == BookingStatus.DRAFT || current == BookingStatus.PENDING)
          ? BookingStatus.PENDING_PAYMENT
          : null;
      case PAY_SUCCESS -> current == BookingStatus.PENDING_PAYMENT ? BookingStatus.CONFIRMED : null;
      case PAY_TIMEOUT -> current == BookingStatus.PENDING_PAYMENT ? BookingStatus.EXPIRED : null;
      case CHECK_IN -> current == BookingStatus.CONFIRMED ? BookingStatus.CHECKED_IN : null;
      case START_SERVICE -> (current == BookingStatus.CONFIRMED || current == BookingStatus.CHECKED_IN)
          ? BookingStatus.IN_SERVICE
          : null;
      case COMPLETE -> (current == BookingStatus.IN_SERVICE || current == BookingStatus.CHECKED_IN)
          ? BookingStatus.COMPLETED
          : null;
      case CANCEL -> (current == BookingStatus.DRAFT
          || current == BookingStatus.PENDING_PAYMENT
          || current == BookingStatus.PENDING
          || current == BookingStatus.CONFIRMED)
          ? BookingStatus.CANCELLED
          : null;
      case MARK_NO_SHOW -> current == BookingStatus.CONFIRMED ? BookingStatus.NO_SHOW : null;
      case REFUND -> current == BookingStatus.CANCELLED ? BookingStatus.REFUNDED : null;
    };
  }
}
