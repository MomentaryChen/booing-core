package com.bookingcore.service;

import com.bookingcore.api.ApiDtos.CreateMerchantRequest;
import com.bookingcore.common.ApiException;
import com.bookingcore.modules.customization.CustomizationConfig;
import com.bookingcore.modules.customization.CustomizationConfigRepository;
import com.bookingcore.modules.merchant.Merchant;
import com.bookingcore.modules.merchant.MerchantProfile;
import com.bookingcore.modules.merchant.MerchantProfileRepository;
import com.bookingcore.modules.merchant.MerchantRepository;
import org.springframework.stereotype.Service;

@Service
public class MerchantProvisioningService {
  private final MerchantRepository merchantRepository;
  private final MerchantProfileRepository profileRepository;
  private final CustomizationConfigRepository customizationConfigRepository;

  public MerchantProvisioningService(
      MerchantRepository merchantRepository,
      MerchantProfileRepository profileRepository,
      CustomizationConfigRepository customizationConfigRepository) {
    this.merchantRepository = merchantRepository;
    this.profileRepository = profileRepository;
    this.customizationConfigRepository = customizationConfigRepository;
  }

  public Merchant createMerchant(CreateMerchantRequest request) {
    merchantRepository.findBySlug(request.slug()).ifPresent(it -> {
      throw new ApiException("Slug already in use");
    });

    Merchant merchant = new Merchant();
    merchant.setName(request.name());
    merchant.setSlug(request.slug());
    Merchant saved = merchantRepository.save(merchant);

    MerchantProfile profile = new MerchantProfile();
    profile.setMerchant(saved);
    profileRepository.save(profile);

    CustomizationConfig customization = new CustomizationConfig();
    customization.setMerchant(saved);
    customizationConfigRepository.save(customization);
    return saved;
  }
}
