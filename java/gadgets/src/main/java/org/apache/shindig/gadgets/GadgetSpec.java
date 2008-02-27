/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a Gadget specification.
 */
public interface GadgetSpec {
  public static final String DEFAULT_VIEW = "default";

  public String getTitle();
  public URI getTitleURI();
  public String getDirectoryTitle();
  public String getDescription();
  public String getAuthor();
  public String getAuthorEmail();
  public URI getScreenshot();
  public URI getThumbnail();
  public List<String> getCategories();

  public static interface LocaleSpec {
    public Locale getLocale();
    public URI getURI();
    public boolean isRightToLeft();
  }

  public List<LocaleSpec> getLocaleSpecs();

  public static interface FeatureSpec {
    public String getName();
    public Map<String, String> getParams();
    public boolean isOptional();
  }

  public Map<String, FeatureSpec> getRequires();
  public List<String> getPreloads();

  public static interface Icon {
    public URI getURI();
    public String getMode();
    public String getType();
  }

  public List<Icon> getIcons();

  public static interface UserPref {

    public String getName();
    public String getDisplayName();
    public String getDefaultValue();
    public boolean isRequired();

    public enum DataType {
      STRING, HIDDEN, BOOL, ENUM, LIST, NUMBER
    }

    public DataType getDataType();

    public Map<String, String> getEnumValues();
  }

  public List<UserPref> getUserPrefs();

  public enum ContentType {
    HTML, URL;

    public static ContentType parse(String type) {
      if ("url".equals(type)) {
        return URL;
      }
      return HTML;
    }
  }

  public static interface View {
    /**
     * @return Content type for the view, either HTML or URL
     */
    public ContentType getType();
    /**
     * Must be a URI type gadget.
     *
     * @return The URI for this gadget spec.
     * @throws IllegalStateException if contentType is not URI.
     */
    public URI getHref();
    /**
     * @return The HTML content for the default view of this gadget spec.
     * @throws IllegalStateException if contentType is not HTML.
     */
    public String getData();
    /**
     * @return Whether to use quirks or standards mode.
     *         If standards mode, then add appropriate DOCTYPE.
     */
    public boolean getQuirks();
  }

  /**
   * @param view Identifier of the desired view to retrieve.
   * @return The View for the specified view of this gadget spec,
   *         or null if no such view was defined.
   */
  public View getView(String view);

  /**
   * @return A copy of the spec. This is NOT the same as clone().
   */
  public GadgetSpec copy();

  /**
   * @return Info string for all views on attributes/contents
   */
  public String toString();
}