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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.ContainerConfig;

import com.google.inject.Inject;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles processing a single http request
 */
public class GadgetRenderer {
  private final GadgetServer server;
  private final GadgetFeatureRegistry registry;
  private final ContainerConfig syndicatorConfig;
  private final UrlGenerator urlGenerator;

  /**
   * Renders the current request
   *
   * @param request
   * @param response
   * @throws IOException
   */
  public void render(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    GadgetRenderingTask task = new GadgetRenderingTask(
          request, response, server, registry, syndicatorConfig, urlGenerator);
    task.process();
  }

  @Inject
  public GadgetRenderer(GadgetServer server,
                        GadgetFeatureRegistry registry,
                        ContainerConfig syndicatorConfig,
                        UrlGenerator urlGenerator) {
    this.server = server;
    this.registry = registry;
    this.syndicatorConfig = syndicatorConfig;
    this.urlGenerator = urlGenerator;
  }
}
