package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.LinkType;
import jakarta.validation.constraints.NotNull;

public class LinkCreation {
  @NotNull public LinkType linkType;
  public Boolean fromTargetPerspective = false;
  public Boolean moveUnder;
  public Boolean asFirstChild = false;
}
