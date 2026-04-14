package com.bookingcore.api.support;

import com.bookingcore.api.ApiDtos.ResourceItemSummary;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.service.ServiceItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResourceItemMediaResolver {

  private final ObjectMapper objectMapper;

  public ResourceItemMediaResolver(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ResourceItemSummary toSummary(ResourceItem resource, long merchantId, Map<Long, ServiceItem> serviceById) {
    String imageUrl = resolveCoverImageUrl(resource, merchantId, serviceById);
    return new ResourceItemSummary(
        resource.getId(),
        resource.getName(),
        resource.getType(),
        resource.getCategory(),
        resource.getCapacity(),
        resource.getActive(),
        resource.getPrice(),
        imageUrl);
  }

  public Map<Long, ServiceItem> indexById(Iterable<ServiceItem> services) {
    Map<Long, ServiceItem> map = new HashMap<>();
    for (ServiceItem s : services) {
      if (s.getId() != null) {
        map.put(s.getId(), s);
      }
    }
    return map;
  }

  private String resolveCoverImageUrl(ResourceItem resource, long merchantId, Map<Long, ServiceItem> serviceById) {
    if (resource.getMerchant() == null || !Objects.equals(resource.getMerchant().getId(), merchantId)) {
      return null;
    }
    String raw = resource.getServiceItemsJson();
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(raw);
      if (!node.isArray()) {
        return null;
      }
      for (JsonNode idNode : node) {
        if (idNode == null || !idNode.canConvertToLong()) {
          continue;
        }
        long serviceId = idNode.longValue();
        ServiceItem service = serviceById.get(serviceId);
        if (service == null) {
          continue;
        }
        if (!Objects.equals(service.getMerchant().getId(), merchantId)) {
          continue;
        }
        if (StringUtils.hasText(service.getImageUrl())) {
          return service.getImageUrl();
        }
      }
      return null;
    } catch (Exception ignored) {
      return null;
    }
  }
}
