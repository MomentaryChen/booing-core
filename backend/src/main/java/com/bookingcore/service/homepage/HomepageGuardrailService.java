package com.bookingcore.service.homepage;

import com.bookingcore.common.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HomepageGuardrailService {

  private static final List<String> FORBIDDEN_SUBSTRINGS =
      List.of(
          "立即確認",
          "必定核准",
          "100% 成功",
          "instant confirmation",
          "guaranteed approval",
          "100% success");

  public void assertCopyAllowed(String text) {
    if (text == null || text.isEmpty()) {
      return;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    for (String phrase : FORBIDDEN_SUBSTRINGS) {
      if (text.contains(phrase) || normalized.contains(phrase.toLowerCase(Locale.ROOT))) {
        throw new ApiException(
            "Homepage copy violates state legality guardrails",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "STATE_GUARDRAIL_VIOLATION");
      }
    }
  }

  public void assertCopyAllowed(Iterable<String> texts) {
    for (String t : texts) {
      assertCopyAllowed(t);
    }
  }
}
