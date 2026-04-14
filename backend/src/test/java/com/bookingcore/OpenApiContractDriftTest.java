package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractDriftTest {

  private static final Set<String> HTTP_METHODS =
      Set.of("get", "post", "put", "patch", "delete", "options", "head", "trace");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void p0ContractOperationsMustMatchRuntimeOpenApiGuardrails() throws Exception {
    Path contractPath = Path.of("..", "doc", "api", "booking-core-p0.openapi.yaml").normalize();
    assertThat(Files.exists(contractPath))
        .as("Normative OpenAPI contract file should exist: %s", contractPath)
        .isTrue();

    Map<String, OperationSignature> contractOperations = loadContractOperations(contractPath);
    JsonNode runtimeOpenApi =
        objectMapper.readTree(
            mockMvc
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    Map<String, OperationSignature> runtimeOperations = loadRuntimeOperations(runtimeOpenApi);
    Set<String> missing = new HashSet<>(contractOperations.keySet());
    missing.removeAll(runtimeOperations.keySet());

    assertThat(missing)
        .as(
            "Runtime OpenAPI is missing contract operations. Missing (path#method): %s",
            String.join(", ", missing))
        .isEmpty();

    for (Map.Entry<String, OperationSignature> entry : contractOperations.entrySet()) {
      String operationKey = entry.getKey();
      OperationSignature contract = entry.getValue();
      OperationSignature runtime = runtimeOperations.get(operationKey);

      assertThat(runtime).as("Runtime operation should exist: %s", operationKey).isNotNull();
      assertThat(runtime.parameters)
          .as("Runtime parameters must include all contract parameters for %s", operationKey)
          .containsAll(contract.parameters);
      assertThat(runtime.responseCodes)
          .as("Runtime responses must include all contract success status codes for %s", operationKey)
          .containsAll(contract.successResponseCodes);
      if (contract.hasRequestBody) {
        assertThat(runtime.hasRequestBody)
            .as("Runtime operation must declare requestBody for %s", operationKey)
            .isTrue();
        if (contract.requestBodyRequired) {
          assertThat(runtime.requestBodyRequired)
              .as("Runtime requestBody should be required for %s", operationKey)
              .isTrue();
        }
        if (contract.requestBodySchemaRef != null) {
          assertThat(runtime.requestBodySchemaRef)
              .as("Runtime requestBody schema ref drift on %s", operationKey)
              .isEqualTo(contract.requestBodySchemaRef);
        }
      }
      assertThat(runtime.successResponseSchemaRefs.entrySet())
          .as("Runtime success response schema refs must include contract refs for %s", operationKey)
          .containsAll(contract.successResponseSchemaRefs.entrySet());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, OperationSignature> loadContractOperations(Path contractPath) throws Exception {
    Yaml yaml = new Yaml();
    try (InputStream inputStream = Files.newInputStream(contractPath)) {
      Map<String, Object> root = yaml.load(inputStream);
      Map<String, Object> paths = (Map<String, Object>) root.get("paths");
      Map<String, OperationSignature> operations = new LinkedHashMap<>();
      if (paths == null) {
        return operations;
      }
      for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
        String path = pathEntry.getKey();
        if (!(pathEntry.getValue() instanceof Map<?, ?> pathConfig)) {
          continue;
        }
        for (Object methodKey : pathConfig.keySet()) {
          String method = String.valueOf(methodKey).toLowerCase(Locale.ROOT);
          if (HTTP_METHODS.contains(method)) {
            Object opConfig = pathConfig.get(methodKey);
            if (!(opConfig instanceof Map<?, ?> opMapRaw)) {
              operations.put(path + "#" + method, OperationSignature.empty());
              continue;
            }
            Map<String, Object> operationMap = (Map<String, Object>) opMapRaw;
            Set<String> params = collectContractParameters(operationMap.get("parameters"));
            Set<String> responses = collectContractResponses(operationMap.get("responses"));
            RequestBodyInfo requestBodyInfo = collectContractRequestBodyInfo(operationMap.get("requestBody"));
            Map<String, String> successSchemaRefs =
                collectContractSuccessResponseSchemaRefs(operationMap.get("responses"));
            operations.put(
                path + "#" + method,
                new OperationSignature(
                    params,
                    responses,
                    collectSuccessCodes(responses),
                    successSchemaRefs,
                    requestBodyInfo.hasRequestBody,
                    requestBodyInfo.required,
                    requestBodyInfo.schemaRef));
          }
        }
      }
      return operations;
    }
  }

  private Map<String, OperationSignature> loadRuntimeOperations(JsonNode runtimeOpenApi) {
    Map<String, OperationSignature> operations = new LinkedHashMap<>();
    JsonNode paths = runtimeOpenApi.path("paths");
    if (!paths.isObject()) {
      return operations;
    }
    paths
        .fields()
        .forEachRemaining(
            pathEntry -> {
              String path = pathEntry.getKey();
              JsonNode methods = pathEntry.getValue();
              if (!methods.isObject()) {
                return;
              }
              methods
                  .fieldNames()
                  .forEachRemaining(
                      methodName -> {
                        String method = methodName.toLowerCase(Locale.ROOT);
                        if (HTTP_METHODS.contains(method)) {
                          JsonNode operationNode = methods.path(methodName);
                          Set<String> parameters = collectRuntimeParameters(operationNode.path("parameters"));
                          Set<String> responses = collectRuntimeResponses(operationNode.path("responses"));
                          RequestBodyInfo requestBodyInfo =
                              collectRuntimeRequestBodyInfo(operationNode.path("requestBody"));
                          Map<String, String> successSchemaRefs =
                              collectRuntimeSuccessResponseSchemaRefs(operationNode.path("responses"));
                          operations.put(
                              path + "#" + method,
                              new OperationSignature(
                                  parameters,
                                  responses,
                                  collectSuccessCodes(responses),
                                  successSchemaRefs,
                                  requestBodyInfo.hasRequestBody,
                                  requestBodyInfo.required,
                                  requestBodyInfo.schemaRef));
                        }
                      });
            });
    return operations;
  }

  private Set<String> collectContractParameters(Object parametersNode) {
    if (!(parametersNode instanceof Iterable<?> iterable)) {
      return Collections.emptySet();
    }
    Set<String> keys = new HashSet<>();
    for (Object item : iterable) {
      if (!(item instanceof Map<?, ?> param)) {
        continue;
      }
      Object name = param.get("name");
      Object location = param.get("in");
      boolean required = Boolean.TRUE.equals(param.get("required"));
      keys.add(String.valueOf(name) + ":" + String.valueOf(location) + ":" + required);
    }
    return keys;
  }

  private Set<String> collectContractResponses(Object responsesNode) {
    if (!(responsesNode instanceof Map<?, ?> responseMap)) {
      return Collections.emptySet();
    }
    Set<String> codes = new HashSet<>();
    for (Object key : responseMap.keySet()) {
      codes.add(String.valueOf(key));
    }
    return codes;
  }

  private RequestBodyInfo collectContractRequestBodyInfo(Object requestBodyNode) {
    if (!(requestBodyNode instanceof Map<?, ?> map)) {
      return RequestBodyInfo.none();
    }
    boolean required = Boolean.TRUE.equals(map.get("required"));
    String schemaRef = extractContractJsonSchemaRef(map.get("content"));
    return new RequestBodyInfo(true, required, schemaRef);
  }

  private Set<String> collectRuntimeParameters(JsonNode parametersNode) {
    if (!parametersNode.isArray()) {
      return Collections.emptySet();
    }
    Set<String> keys = new HashSet<>();
    parametersNode.forEach(
        parameter -> {
          String name = parameter.path("name").asText();
          String location = parameter.path("in").asText();
          boolean required = parameter.path("required").asBoolean(false);
          keys.add(name + ":" + location + ":" + required);
        });
    return keys;
  }

  private Set<String> collectRuntimeResponses(JsonNode responsesNode) {
    if (!responsesNode.isObject()) {
      return Collections.emptySet();
    }
    Set<String> codes = new HashSet<>();
    responsesNode.fieldNames().forEachRemaining(codes::add);
    return codes;
  }

  private RequestBodyInfo collectRuntimeRequestBodyInfo(JsonNode requestBodyNode) {
    if (!requestBodyNode.isObject()) {
      return RequestBodyInfo.none();
    }
    boolean required = requestBodyNode.path("required").asBoolean(false);
    String schemaRef = extractRuntimeJsonSchemaRef(requestBodyNode.path("content"));
    return new RequestBodyInfo(true, required, schemaRef);
  }

  private Set<String> collectSuccessCodes(Set<String> responseCodes) {
    Set<String> successCodes = new HashSet<>();
    for (String code : responseCodes) {
      if (code.startsWith("2")) {
        successCodes.add(code);
      }
    }
    return successCodes;
  }

  private Map<String, String> collectContractSuccessResponseSchemaRefs(Object responsesNode) {
    if (!(responsesNode instanceof Map<?, ?> responseMap)) {
      return Collections.emptyMap();
    }
    Map<String, String> refs = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : responseMap.entrySet()) {
      String code = String.valueOf(entry.getKey());
      if (!code.startsWith("2")) {
        continue;
      }
      if (!(entry.getValue() instanceof Map<?, ?> responseDetail)) {
        continue;
      }
      String schemaRef = extractContractJsonSchemaRef(responseDetail.get("content"));
      if (schemaRef != null) {
        refs.put(code, schemaRef);
      }
    }
    return refs;
  }

  private Map<String, String> collectRuntimeSuccessResponseSchemaRefs(JsonNode responsesNode) {
    if (!responsesNode.isObject()) {
      return Collections.emptyMap();
    }
    Map<String, String> refs = new LinkedHashMap<>();
    responsesNode
        .fields()
        .forEachRemaining(
            responseEntry -> {
              String code = responseEntry.getKey();
              if (!code.startsWith("2")) {
                return;
              }
              String schemaRef =
                  extractRuntimeJsonSchemaRef(responseEntry.getValue().path("content"));
              if (schemaRef != null) {
                refs.put(code, schemaRef);
              }
            });
    return refs;
  }

  private String extractContractJsonSchemaRef(Object contentNode) {
    if (!(contentNode instanceof Map<?, ?> contentMap)) {
      return null;
    }
    Object appJson = contentMap.get("application/json");
    if (!(appJson instanceof Map<?, ?> appJsonMap)) {
      return null;
    }
    Object schema = appJsonMap.get("schema");
    if (!(schema instanceof Map<?, ?> schemaMap)) {
      return null;
    }
    Object ref = schemaMap.get("$ref");
    return ref == null ? null : String.valueOf(ref);
  }

  private String extractRuntimeJsonSchemaRef(JsonNode contentNode) {
    if (!contentNode.isObject()) {
      return null;
    }
    JsonNode schema = null;
    JsonNode appJson = contentNode.path("application/json");
    if (appJson.isObject()) {
      schema = appJson.path("schema");
    } else {
      var iterator = contentNode.elements();
      while (iterator.hasNext()) {
        JsonNode mediaTypeNode = iterator.next();
        if (mediaTypeNode.isObject() && mediaTypeNode.has("schema")) {
          schema = mediaTypeNode.path("schema");
          break;
        }
      }
    }
    if (schema == null || !schema.isObject()) {
      return null;
    }
    JsonNode ref = schema.path("$ref");
    return ref.isTextual() ? ref.asText() : null;
  }

  private record RequestBodyInfo(boolean hasRequestBody, boolean required, String schemaRef) {
    private static RequestBodyInfo none() {
      return new RequestBodyInfo(false, false, null);
    }
  }

  private record OperationSignature(
      Set<String> parameters,
      Set<String> responseCodes,
      Set<String> successResponseCodes,
      Map<String, String> successResponseSchemaRefs,
      boolean hasRequestBody,
      boolean requestBodyRequired,
      String requestBodySchemaRef) {
    private static OperationSignature empty() {
      return new OperationSignature(
          Collections.emptySet(),
          Collections.emptySet(),
          Collections.emptySet(),
          Collections.emptyMap(),
          false,
          false,
          null);
    }
  }
}
