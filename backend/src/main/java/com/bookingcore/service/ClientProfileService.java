package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.ClientPasswordUpdateRequest;
import com.bookingcore.api.ApiDtos.ClientPasswordUpdateResponse;
import com.bookingcore.api.ApiDtos.ClientProfilePreferencesResponse;
import com.bookingcore.api.ApiDtos.ClientProfilePreferencesUpdateRequest;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.client.ClientProfile;
import com.bookingcore.modules.client.ClientProfileRepository;
import com.bookingcore.modules.platform.PlatformUser;
import com.bookingcore.modules.platform.PlatformUserRepository;
import java.time.LocalDateTime;
import java.lang.reflect.Method;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientProfileService {
  private final MerchantAccessService merchantAccessService;
  private final ClientProfileRepository clientProfileRepository;
  private final PlatformUserRepository platformUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final PlatformAuditService platformAuditService;

  public ClientProfileService(
      MerchantAccessService merchantAccessService,
      ClientProfileRepository clientProfileRepository,
      PlatformUserRepository platformUserRepository,
      PasswordEncoder passwordEncoder,
      PlatformAuditService platformAuditService) {
    this.merchantAccessService = merchantAccessService;
    this.clientProfileRepository = clientProfileRepository;
    this.platformUserRepository = platformUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.platformAuditService = platformAuditService;
  }

  @Transactional(readOnly = true)
  public ClientProfilePreferencesResponse getPreferences() {
    PlatformUser user = requireCurrentClientUser();
    ClientProfile profile = clientProfileRepository.findByPlatformUserId(user.getId()).orElse(null);
    if (profile == null) {
      return new ClientProfilePreferencesResponse(null, null, null, null, null);
    }
    return new ClientProfilePreferencesResponse(
        profile.getLanguage(),
        profile.getTimezone(),
        profile.getCurrency(),
        profile.getEmailNotifications(),
        profile.getSmsNotifications());
  }

  @Transactional
  public ClientProfilePreferencesResponse updatePreferences(
      ClientProfilePreferencesUpdateRequest request) {
    PlatformUser user = requireCurrentClientUser();
    ClientProfile profile =
        clientProfileRepository
            .findByPlatformUserId(user.getId())
            .orElseGet(
                () -> {
                  ClientProfile created = new ClientProfile();
                  created.setPlatformUser(user);
                  return created;
                });
    profile.setLanguage(trimToNull(request.language()));
    profile.setTimezone(trimToNull(request.timezone()));
    profile.setCurrency(trimToNull(request.currency()));
    profile.setEmailNotifications(request.emailNotifications());
    profile.setSmsNotifications(request.smsNotifications());
    ClientProfile saved = clientProfileRepository.save(profile);
    platformAuditService.recordForCurrentUser(
        "client.profile.preferences.update",
        "client_profile",
        saved.getId(),
        "language="
            + saved.getLanguage()
            + ",timezone="
            + saved.getTimezone()
            + ",currency="
            + saved.getCurrency());
    return new ClientProfilePreferencesResponse(
        saved.getLanguage(),
        saved.getTimezone(),
        saved.getCurrency(),
        saved.getEmailNotifications(),
        saved.getSmsNotifications());
  }

  @Transactional
  public ClientPasswordUpdateResponse updatePassword(ClientPasswordUpdateRequest request) {
    PlatformUser user = requireCurrentClientUser();
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new ApiException(
          "Current password is incorrect",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "CLIENT_PASSWORD_INVALID_CURRENT");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new ApiException(
          "New password must be different from current password",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "CLIENT_PASSWORD_UNCHANGED");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setCredentialVersion(user.getCredentialVersion() + 1);
    LocalDateTime updatedAt = LocalDateTime.now();
    invokeIfPresent(user, "setPasswordUpdatedAt", LocalDateTime.class, updatedAt);
    invokeIfPresent(user, "setPasswordChangeRequired", Boolean.class, Boolean.FALSE);
    platformUserRepository.save(user);
    platformAuditService.recordForCurrentUser(
        "client.profile.password.update",
        "platform_user",
        user.getId(),
        "credentialVersion=" + user.getCredentialVersion());
    return new ClientPasswordUpdateResponse(updatedAt);
  }

  private PlatformUser requireCurrentClientUser() {
    return java.util.Optional.ofNullable(merchantAccessService.currentPlatformUserOrNull())
        .orElseThrow(() -> new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED));
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void invokeIfPresent(
      PlatformUser user, String methodName, Class<?> parameterType, Object argument) {
    try {
      Method method = PlatformUser.class.getMethod(methodName, parameterType);
      method.invoke(user, argument);
    } catch (NoSuchMethodException ignored) {
      // Keep service compile-safe across PlatformUser variants.
    } catch (ReflectiveOperationException ex) {
      throw new ApiException("Failed to update user password metadata", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
