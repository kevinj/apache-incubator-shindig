/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for proxy-based handlers.
 */
public abstract class ProxyBase {
  public static final String URL_PARAM = "url";
  public static final String REFRESH_PARAM = "refresh";
  public static final String GADGET_PARAM = "gadget";

  // Public because of rewriter. Rewriter should be cleaned up.
  public static final String REWRITE_MIME_TYPE_PARAM = "rewriteMime";

  /**
   * Validates the given url.
   *
   * @return A URI representing a validated form of the url.
   * @throws GadgetException If the url is not valid.
   */
  protected URI validateUrl(String urlToValidate) throws GadgetException {
    if (urlToValidate == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is missing.");
    }
    try {
      URI url = new URI(urlToValidate);
      if (!"http".equals(url.getScheme()) && !"https".equals(url.getScheme())) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "Invalid request url scheme; only " +
            "\"http\" and \"https\" supported.");
      }
      if (url.getPath() == null || url.getPath().length() == 0) {
        // Forcibly set the path to "/" if it is empty
        url = new URI(url.getScheme(),
                      url.getUserInfo(),
                      url.getHost(),
                      url.getPort(),
                      "/", url.getQuery(),
                      url.getFragment());
      }
      return url;
    } catch (URISyntaxException use) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is not a valid url.");
    }
  }

  /**
   * Extracts the first parameter from the parameter map with the given name.
   *
   * @param request The request to extract parameters from.
   * @param name The name of the parameter to retrieve.
   * @param defaultValue The default value to use if the parameter is not set.
   * @return The parameter, if found, or defaultValue
   */
  protected String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String ret = request.getParameter(name);
    return ret == null ? defaultValue : ret;
  }

  /**
   * Sets cache control headers for the response.
   */
  protected void setResponseHeaders(HttpServletRequest request,
      HttpServletResponse response, HttpResponse results) {
    int refreshInterval = 0;
    if (results.isStrictNoCache()) {
      refreshInterval = 0;
    } else  if (request.getParameter(REFRESH_PARAM) != null) {
      refreshInterval =  Integer.valueOf(request.getParameter(REFRESH_PARAM));
    } else {
      refreshInterval = Math.max(60 * 60, (int)(results.getExpiration() / 1000));
    }
    HttpUtil.setCachingHeaders(response, refreshInterval);
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");
  }

  /**
   * Processes the given request.
   */
  abstract public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws GadgetException, IOException;
}
