/*
 * $Id$
 *
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contains useful static objects for testing classes that deal with GadgetSpecs.
 */
public class GadgetSpecTestFixture {
  static final Locale EN_US_LOCALE = new Locale("en", "US");

  private static final String DATETIME_TITLE = "Hello, World!";
  private static final String DATETIME_CONTENT = "Goodbye, World!";

  static final String DATETIME_URI_STRING = "http://www.google.com/ig/modules/datetime.xml";
  static final URI DATETIME_URI;
  static final int DATETIME_MODULE_ID = 1;
  static final GadgetView.ID DATETIME_ID;
  static final String DATETIME_XML = "<?xml version=\"1.0\"?>" +
                                     "<Module>" +
                                     "  <ModulePrefs title=\"" + DATETIME_TITLE + "\"/>" +
                                     "  <Content type=\"html\">" + DATETIME_CONTENT + "</Content>" +
                                     "</Module>";

  static final GadgetSpec DATETIME_SPEC =
      new GadgetSpec() {
        public GadgetSpec copy() {
          throw new UnsupportedOperationException();
        }
        public String getAuthor() {
          return null;
        }
        public String getAuthorEmail() {
          return null;
        }
        public String getContentData() {
          return DATETIME_CONTENT;
        }
        public URI getContentHref() {
          return null;
        }
        public ContentType getContentType() {
          return ContentType.HTML;
        }
        public String getDescription() {
          return null;
        }
        public String getDirectoryTitle() {
          return null;
        }
        public List<Icon> getIcons() {
          return null;
        }
        public List<LocaleSpec> getLocaleSpecs() {
          return new LinkedList<LocaleSpec>();
        }
        public List<String> getPreloads() {
          return null;
        }
        public Map<String, FeatureSpec> getRequires() {
          return new HashMap<String, FeatureSpec>();
        }
        public String getScreenshot() {
          return null;
        }
        public String getThumbnail() {
          return null;
        }
        public String getTitle() {
          return DATETIME_TITLE;
        }
        public URI getTitleURI() {
          return null;
        }
        public List<UserPref> getUserPrefs() {
          return new LinkedList<UserPref>();
        }
      };

  static {
    try {
      DATETIME_URI = new URI(DATETIME_URI_STRING);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Cannot initialize fixture", e);
    }
    DATETIME_ID = new Gadget.GadgetId(DATETIME_URI, DATETIME_MODULE_ID);
  }
}
