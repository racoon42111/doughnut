package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.models.WikidataLocationModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private HttpClientAdapter httpClientAdapter;

  public RestWikidataController(
      TestabilitySettings testabilitySettings, HttpClientAdapter httpClientAdapter) {
    this.testabilitySettings = testabilitySettings;
    this.httpClientAdapter = httpClientAdapter;
  }

  @GetMapping("/wikidata/{wikiDataId}")
  public WikidataEntity fetchWikiDataByID(@PathVariable("wikiDataId") String wikiDataId)
      throws InterruptedException, BindException {
    try {
      return getWikiDataService().fetchWikiData(wikiDataId);
    } catch (IOException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(wikiDataId, "wikidataId");
      bindingResult.rejectValue(null, "error.error", "The wikidata service is not available");
      throw new BindException(bindingResult);
    }
  }

  @GetMapping("/wikidata/search/{search}")
  public List<WikidataSearchEntity> searchWikidata(@PathVariable("search") String search)
      throws InterruptedException, BindException {
    try {
      return getWikiDataService().fetchWikidataByQuery(search);
    } catch (IOException e) {
      BindingResult bindingResult = new BeanPropertyBindingResult(search, "search");
      bindingResult.rejectValue(null, "error.error", "The wikidata service is not available");
      throw new BindException(bindingResult);
    }
  }

  private WikidataService getWikiDataService() {
    return new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @GetMapping("/wikidata/entity/{id}/location")
  public WikidataLocationModel getWikidataLocation(String id)
      throws IOException, InterruptedException {
    httpClientAdapter.getResponseString(
        URI.create(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                + id
                + "&props=claims&format=json"));

    return new WikidataLocationModel("1.3", "103.8");
  }
}
