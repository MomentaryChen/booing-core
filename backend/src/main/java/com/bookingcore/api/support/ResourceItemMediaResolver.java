package com.bookingcore.api.support;

import com.bookingcore.api.ApiDtos.ResourceItemSummary;
import com.bookingcore.api.ApiDtos.ResourceOperationalStatus;
import com.bookingcore.modules.merchant.ResourceItem;
import com.bookingcore.modules.service.ServiceItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResourceItemMediaResolver {

  private final ObjectMapper objectMapper;

  public ResourceItemMediaResolver(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ResourceItemSummary toSummary(
      ResourceItem resource,
      UUID merchantId,
      Map<UUID, ServiceItem> serviceById,
      ResourceOperationalStatus status) {
    String imageUrl = resolveCoverImageUrl(resource, merchantId, serviceById);
    return new ResourceItemSummary(
        resource.getId(),
        resource.getName(),
        resource.getType(),
        resource.getCategory(),
        resource.getCapacity(),
        resource.getActive(),
        resource.getPrice(),
        parseUuidArray(resource.getAssignedStaffIdsJson()),
        resource.getServiceItemsJson(),
        imageUrl,
        status,
        resource.getBusinessHoursJson());
  }

  public Map<UUID, ServiceItem> indexById(Iterable<ServiceItem> services) {
    Map<UUID, ServiceItem> map = new HashMap<>();
    for (ServiceItem s : services) {
      if (s.getId() != null) {
        map.put(s.getId(), s);
      }
    }
    return map;
  }

  private String resolveCoverImageUrl(ResourceItem resource, UUID merchantId, Map<UUID, ServiceItem> serviceById) {
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
        UUID serviceId = parseUuidNode(idNode);
        if (serviceId == null) {
          continue;
        }
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

  private static UUID parseUuidNode(JsonNode idNode) {
    if (idNode == null || idNode.isNull()) {
      return null;
    }
    if (idNode.isTextual()) {
      try {
        return UUID.fromString(idNode.asText());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    if (idNode.canConvertToLong()) {
      return null;
    }
    return null;
  }

  private List<UUID> parseUuidArray(String raw) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    try {
      JsonNode node = objectMapper.readTree(raw);
      if (!node.isArray()) {
        return List.of();
      }
      LinkedHashSet<UUID> ids = new LinkedHashSet<>();
      for (JsonNode idNode : node) {
        UUID id = parseUuidNode(idNode);
        if (id != null) {
          ids.add(id);
        }
      }
      return List.copyOf(ids);
    } catch (Exception ignored) {
      return List.of();
    }
  }
}
