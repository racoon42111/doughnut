package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.web.util.UriComponentsBuilder;

public record WikidataService(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  private UriComponentsBuilder wikidataUriBuilder() {
    return UriComponentsBuilder.fromHttpUrl(wikidataUrl);
  }

  public WikidataEntity fetchWikidata(String wikidataId) throws IOException, InterruptedException {
    String responseBody = httpClientAdapter.getResponseString(ConstructWikidataUrl(wikidataId));
    if (responseBody == null) return null;
    WikidataModel wikidataModel =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    WikidataInfo wikidataInfo = wikidataModel.entities.get(wikidataId);
    return new WikidataEntity(
        wikidataInfo.GetEnglishTitle(), wikidataInfo.GetEnglishWikipediaUrl());
  }

  private URI ConstructWikidataUrl(String wikidataId) {
    return wikidataUriBuilder()
        .path("/wiki/Special:EntityData/" + wikidataId + ".json")
        .build()
        .toUri();
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  public static class WikidataModel {
    public Map<String, WikidataInfo> entities;
  }

  public List<WikidataSearchEntity> fetchWikidataByQuery(String search)
      throws IOException, InterruptedException {
    URI uri =
        wikidataUriBuilder()
            .path("/w/api.php")
            .queryParam("action", "wbsearchentities")
            .queryParam("search", "{search}")
            .queryParam("format", "json")
            .queryParam("language", "en")
            .queryParam("uselang", "en")
            .queryParam("type", "item")
            .queryParam("limit", 10)
            .build(search);
    String responseBody = httpClientAdapter.getResponseString(uri);
    WikidataSearchModel entities =
        getObjectMapper().readValue(responseBody, new TypeReference<>() {});
    return entities.getWikidataSearchEntities();
  }

  public static class WikidataSearchModel {
    public List<Map<String, Object>> search;

    private List<WikidataSearchEntity> getWikidataSearchEntities() {
      return search.stream().map(WikidataSearchEntity::new).collect(Collectors.toList());
    }
  }

  @SneakyThrows
  public String getWikidataLocationDescription(String wikidataId) {
    WikidataEntityModel entity;
    try {
      entity = getEntityDataById(wikidataId);
    } catch (IOException e) {
      return null;
    }

    if (entity == null) return null;

    Map<String, Object> locationValue = entity.getStringObjectMap(wikidataId, "P625");
    if (locationValue == null) return null;

    return "Location: "
        + locationValue.get("latitude").toString()
        + "'N, "
        + locationValue.get("longitude").toString()
        + "'E";
  }

  private WikidataEntityModel getEntityDataById(String wikidataId)
      throws IOException, InterruptedException {
    URI uri =
        wikidataUriBuilder()
            .path("/w/api.php")
            .queryParam("action", "wbgetentities")
            .queryParam("ids", "{id}")
            .queryParam("format", "json")
            .queryParam("props", "claims")
            .build(wikidataId);
    String responseBody = httpClientAdapter.getResponseString(uri);
    if (responseBody == null) return null;
    return getObjectMapper().readValue(responseBody, new TypeReference<>() {});
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WikidataEntityModel {
    private Map<String, WikidataEntityItemModel> entities;
    private Number success;

    public List<WikidataEntityItemObjectModel> getLocationClaims(
        String wikidataId, String locationId) {
      WikidataEntityItemModel entityItem = getEntities().get(wikidataId);
      if (entityItem.getClaims() == null) {
        return null;
      }
      if (entityItem.getClaims().containsKey(locationId)) {
        return entityItem.getClaims().get(locationId);
      }

      return null;
    }

    public Map<String, Object> getStringObjectMap(String wikidataId, String locationId) {
      if (!getEntities().containsKey(wikidataId)) return null;
      List<WikidataEntityItemObjectModel> locationClaims =
          getLocationClaims(wikidataId, locationId);
      if (locationClaims == null) {
        return null;
      }
      Map<String, Object> locationValue = locationClaims.get(0).getData();
      return locationValue;
    }
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WikidataEntityItemModel {
    private String type;
    private String id;
    Map<String, List<WikidataEntityItemObjectModel>> claims;
  }

  @Data
  public static class WikidataEntityItemObjectModel {
    static String DATAVALUE_KEY = "datavalue";
    static String VALUE_KEY = "value";
    static String VALUE_TYPE_KEY = "type";

    static class VALUE_TYPE {
      public static String GLOBE_COORDINATE = "globecoordinate";
      public static String STRING = "string";
    }

    private String type;
    private String id;
    Map<String, Object> data;

    @JsonProperty("mainsnak")
    private void unpackNested(Map<String, JsonNode> mainsnak) {
      if (mainsnak.containsKey(DATAVALUE_KEY) && mainsnak.get(DATAVALUE_KEY).has(VALUE_KEY)) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode value = mainsnak.get(DATAVALUE_KEY);
        if (VALUE_TYPE.GLOBE_COORDINATE.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
            == 0) {
          data =
              mapper.convertValue(
                  mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY),
                  new TypeReference<Map<String, Object>>() {});
        } else if (VALUE_TYPE.STRING.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
            == 0) {
          String stringValue =
              mapper.convertValue(
                  mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY), new TypeReference<String>() {});
          data = new LinkedHashMap<>();
          data.put(VALUE_KEY, stringValue);
        }
      }
    }
  }
}
