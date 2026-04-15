package com.bookingcore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bookingcore.common.ApiException;
import com.bookingcore.service.homepage.HomepageGuardrailService;
import org.junit.jupiter.api.Test;

class HomepageGuardrailServiceTest {

  private final HomepageGuardrailService guardrails = new HomepageGuardrailService();

  @Test
  void rejectsForbiddenChinesePhrases() {
    assertThrows(ApiException.class, () -> guardrails.assertCopyAllowed("我們會立即確認每一筆預約"));
  }

  @Test
  void rejectsForbiddenEnglishPhrases() {
    assertThrows(ApiException.class, () -> guardrails.assertCopyAllowed("We offer guaranteed approval"));
  }

  @Test
  void allowsNeutralResourceSlotCopy() {
    assertDoesNotThrow(() -> guardrails.assertCopyAllowed("Resources expose bookable slots computed from rules."));
  }
}
