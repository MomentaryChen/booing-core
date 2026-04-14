package com.bookingcore.api;

import com.bookingcore.api.ApiDtos.AuthMeResponse;
import com.bookingcore.api.ApiDtos.ContextSelectRequest;
import com.bookingcore.api.ApiDtos.LoginRequest;
import com.bookingcore.api.ApiDtos.MerchantEnableRequest;
import com.bookingcore.api.ApiDtos.MerchantEnableResponse;
import com.bookingcore.api.ApiDtos.PublicRegisterRequest;
import com.bookingcore.api.ApiDtos.PublicRegisterResponse;
import com.bookingcore.api.ApiDtos.TokenResponse;
import com.bookingcore.security.LoginRateLimiter;
import com.bookingcore.service.AuthService;
import com.bookingcore.service.PublicRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

  private final AuthService authService;
  private final LoginRateLimiter loginRateLimiter;
  private final PublicRegistrationService publicRegistrationService;

  public AuthController(
      AuthService authService,
      LoginRateLimiter loginRateLimiter,
      PublicRegistrationService publicRegistrationService) {
    this.authService = authService;
    this.loginRateLimiter = loginRateLimiter;
    this.publicRegistrationService = publicRegistrationService;
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    loginRateLimiter.consume(httpRequest);
    return authService.login(request);
  }

  /** Unified public signup; {@code registerType} is allowlisted server-side. */
  @PostMapping("/register")
  public PublicRegisterResponse register(@Valid @RequestBody PublicRegisterRequest request) {
    return publicRegistrationService.register(request);
  }

  @GetMapping("/me")
  public AuthMeResponse me() {
    return authService.me(SecurityContextHolder.getContext().getAuthentication());
  }

  @PostMapping("/refresh")
  public TokenResponse refresh() {
    return authService.refresh(SecurityContextHolder.getContext().getAuthentication());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    authService.logout(SecurityContextHolder.getContext().getAuthentication());
  }

  @PostMapping("/context/select")
  public TokenResponse contextSelect(@RequestBody(required = false) ContextSelectRequest request) {
    return authService.selectContext(SecurityContextHolder.getContext().getAuthentication(), request);
  }

  @PostMapping("/context/switch")
  public TokenResponse contextSwitch(@RequestBody ContextSelectRequest request) {
    return authService.selectContext(SecurityContextHolder.getContext().getAuthentication(), request);
  }

  @PostMapping("/merchant/enable")
  public MerchantEnableResponse enableMerchant(@Valid @RequestBody MerchantEnableRequest request) {
    return authService.enableMerchant(SecurityContextHolder.getContext().getAuthentication(), request);
  }
}
