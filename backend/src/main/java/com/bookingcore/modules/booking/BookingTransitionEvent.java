package com.bookingcore.modules.booking;

public enum BookingTransitionEvent {
  SUBMIT,
  PAY_SUCCESS,
  PAY_TIMEOUT,
  CHECK_IN,
  START_SERVICE,
  COMPLETE,
  CANCEL,
  MARK_NO_SHOW,
  REFUND
}
