/*
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

import java.util.Locale;

/**
 * Bundles together per-server data and helper mechanisms providing
 * generic functionality such as retrieval of remote data and caching.
 */
public class GadgetContext {
  private final RemoteContentFetcher httpFetcher;
  public RemoteContentFetcher getHttpFetcher() {
    return httpFetcher;
  }

  private final GadgetDataCache<MessageBundle> messageBundleCache;
  public GadgetDataCache<MessageBundle> getMessageBundleCache() {
    return messageBundleCache;
  }

  private final Locale locale;
  public Locale getLocale() {
    return locale;
  }

  private final RenderingContext renderingContext;
  public RenderingContext getRenderingContext() {
    return renderingContext;
  }
  
  private final ProcessingOptions options;
  public ProcessingOptions getOptions() {
    return options;
  }

  /**
   * Creates a context for the current gadget.
   * @param httpFetcher
   * @param messageBundleCache
   * @param locale
   * @param renderingContext
   * @param options
   */
  public GadgetContext(RemoteContentFetcher httpFetcher,
                       GadgetDataCache<MessageBundle> messageBundleCache,
                       Locale locale,
                       RenderingContext renderingContext,
                       ProcessingOptions options) {
    this.httpFetcher = httpFetcher;
    this.messageBundleCache = messageBundleCache;
    this.locale = locale;
    this.renderingContext = renderingContext;
    this.options = options != null ? options : new ProcessingOptions();
  }
}
