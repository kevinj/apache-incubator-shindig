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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContentFilter;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSpec;
import org.apache.shindig.gadgets.GadgetView;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.ProcessingOptions;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets, typically in an IFRAME.
 */
public class GadgetRenderingServlet extends HttpServlet {
  private CrossServletState servletState;
  private static final String CAJA_PARAM = "caja";
  private static final String USERPREF_PARAM_PREFIX = "up_";
  private static final String LIBS_PARAM_NAME = "libs";
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");


  @Override
  public void init(ServletConfig config) throws ServletException {
    servletState = CrossServletState.get(config);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String urlParam = req.getParameter("url");
    if (urlParam == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     "Missing required parameter: url");
      return;
    }

    URI uri = null;
    try {
      uri = new URI(urlParam);
    } catch (URISyntaxException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     String.format("Malformed URL %s, reason: %s",
                                   urlParam,
                                   e.getMessage()));
      return;
    }

    int moduleId = 0;
    String mid = req.getParameter("mid");
    if (mid != null) {
      moduleId = Integer.parseInt(mid);
    }

    BasicHttpContext context = new BasicHttpContext(req);
    GadgetView.ID gadgetId = new Gadget.GadgetId(uri, moduleId);
    ProcessingOptions options = new HttpProcessingOptions(req);

    // Prepare a list of GadgetContentFilters applied to the output
    List<GadgetContentFilter> contentFilters =
        new LinkedList<GadgetContentFilter>();
    if (getUseCaja(req)) {
      contentFilters.add(new CajaContentFilter(uri));
    }

    Gadget gadget = null;
    String view = req.getParameter("view");
    view = (view == null || view.length() == 0) ? GadgetSpec.DEFAULT_VIEW : view;
    try {
      gadget = servletState.getGadgetServer().processGadget(gadgetId,
          getPrefsFromRequest(req), context.getLocale(),
          RenderingContext.GADGET, options);
      outputGadget(gadget, view, options, contentFilters, resp);
    } catch (GadgetServer.GadgetProcessException e) {
      outputErrors(e, resp);
    }
  }

  /**
   * Renders a successfully processed gadget.
   *
   * @param gadget
   * @param view
   * @param options
   * @param contentFilters
   * @param resp
   * @throws IOException
   * @throws GadgetServer.GadgetProcessException
   */
  private void outputGadget(Gadget gadget,
                            String view,
                            ProcessingOptions options,
                            List<GadgetContentFilter> contentFilters,
                            HttpServletResponse resp)
      throws IOException, GadgetServer.GadgetProcessException {
    switch(gadget.getContentType()) {
    case HTML:
      outputHtmlGadget(gadget, view, options, contentFilters, resp);
      break;
    case URL:
      outputUrlGadget(gadget, options, resp);
      break;
    }
  }

  /**
   * Handles type=html gadget output.
   *
   * @param gadget
   * @param view
   * @param options
   * @param contentFilters
   * @param resp
   * @throws IOException
   * @throws GadgetServer.GadgetProcessException
   */
  private void outputHtmlGadget(Gadget gadget,
                                String view,
                                ProcessingOptions options,
                                List<GadgetContentFilter> contentFilters,
                                HttpServletResponse resp)
      throws IOException, GadgetServer.GadgetProcessException {
    resp.setContentType("text/html");

    StringBuilder markup = new StringBuilder();
    markup.append("<html><head>");
    // TODO: This is so wrong.
    markup.append("<style type=\"text/css\">" +
                  "body,td,div,span,p{font-family:arial,sans-serif;}" +
                  "a {color:#0000cc;}a:visited {color:#551a8b;}" +
                  "a:active {color:#ff0000;}" +
                  "body{margin: 0px;padding: 0px;background-color:white;}" +
                  "</style>");
    markup.append("</head><body>");
    StringBuilder externJs = new StringBuilder();
    StringBuilder inlineJs = new StringBuilder();
    String externFmt = "<script src=\"%s\"></script>\n";
    String forcedLibs = options.getForcedJsLibs();

    for (JsLibrary library : gadget.getJsLibraries()) {
      JsLibrary.Type type = library.getType();
      if (type == JsLibrary.Type.URL) {
        // TODO: This case needs to be handled more gracefully by the js
        // servlet. We should probably inline external JS as well.
        externJs.append(String.format(externFmt, library.getContent()));
      } else if (type == JsLibrary.Type.INLINE) {
        inlineJs.append(library.getContent()).append('\n');
      } else {
        // FILE or RESOURCE
        if (forcedLibs == null) {
          inlineJs.append(library.getContent()).append('\n');
        } // otherwise it was already included by options.forceJsLibs.
      }
    }

    // Forced libs first.

    if (forcedLibs != null) {
      String[] libs = forcedLibs.split(":");
      markup.append(String.format(externFmt, servletState.getJsUrl(libs)));
    }

    if (inlineJs.length() > 0) {
      markup.append("<script><!--\n").append(inlineJs)
            .append("\n-->\n</script>");
    }

    if (externJs.length() > 0) {
      markup.append(externJs);
    }

    List<GadgetException> gadgetExceptions = new LinkedList<GadgetException>();
    String content = gadget.getContentData(view);
    if (content == null) {
      // unknown view
      gadgetExceptions.add(
          new GadgetException(
              GadgetException.Code.UNKNOWN_VIEW_SPECIFIED,
              "View: '" + view + "' invalid for gadget: " +
              gadget.getId().getKey()));
    } else {
      for (GadgetContentFilter filter : contentFilters) {
        try {
          content = filter.filter(content);
        } catch (GadgetException e) {
          gadgetExceptions.add(e);
        }
      }
    }

    if (gadgetExceptions.size() > 0) {
      throw new GadgetServer.GadgetProcessException(gadgetExceptions);
    }

    markup.append(content);
    markup.append("<script>gadgets.util.runOnLoadHandlers();</script>");
    markup.append("</body></html>");

    resp.getOutputStream().print(markup.toString());
  }

  private void outputUrlGadget(Gadget gadget,
      ProcessingOptions options, HttpServletResponse resp) throws IOException {
    // TODO: generalize this as injectedArgs on Gadget object

    // Preserve existing query string parameters.
    URI redirURI = gadget.getContentHref();
    StringBuilder query = new StringBuilder(redirURI.getQuery());

    // TODO: userprefs on the fragment rather than query string
    query.append(getPrefsQueryString(gadget.getUserPrefValues()));

    String[] libs;
    String forcedLibs = options.getForcedJsLibs();
    if (forcedLibs == null) {
      libs = (String[])gadget.getRequires().keySet().toArray();
    } else {
      libs = forcedLibs.split(":");
    }
    appendLibsToQuery(libs, query);

    try {
      redirURI = new URI(redirURI.getScheme(),
                         redirURI.getUserInfo(),
                         redirURI.getHost(),
                         redirURI.getPort(),
                         redirURI.getPath(),
                         query.toString(),
                         redirURI.getFragment());
    } catch (URISyntaxException e) {
      // Not really ever going to happen; input values are already OK.
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     e.getMessage());
    }
    resp.sendRedirect(redirURI.toString());
  }

  private void outputErrors(GadgetServer.GadgetProcessException errs,
                            HttpServletResponse resp)
      throws IOException {
    // TODO: make this way more robust
    StringBuilder markup = new StringBuilder();
    markup.append("<html><body>");
    markup.append("<pre>");
    for (GadgetException error : errs.getComponents()) {
      markup.append(error.getCode().toString());
      markup.append(' ');
      markup.append(error.getMessage());
      markup.append('\n');

      // Log the errors here for now. We might want different severity levels
      // for different error codes.
      logger.log(Level.INFO, "Failed to render gadget", error);
    }
    markup.append("</pre>");
    markup.append("</body></html>");
    resp.getOutputStream().print(markup.toString());
  }

  @SuppressWarnings("unchecked")
  private UserPrefs getPrefsFromRequest(HttpServletRequest req) {
    Map<String, String> prefs = new HashMap<String, String>();
    Enumeration<String> paramNames = req.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (paramName.startsWith(USERPREF_PARAM_PREFIX)) {
        String prefName = paramName.substring(USERPREF_PARAM_PREFIX.length());
        prefs.put(prefName, req.getParameter(paramName));
      }
    }
    return new UserPrefs(prefs);
  }

  private String getPrefsQueryString(UserPrefs prefVals) {
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> prefEntry : prefVals.getPrefs().entrySet()) {
      buf.append('&');
      try {
        buf.append(USERPREF_PARAM_PREFIX)
           .append(URLEncoder.encode(prefEntry.getKey(), "UTF8"))
           .append("=")
           .append(URLEncoder.encode(prefEntry.getValue(), "UTF8"));
      } catch (UnsupportedEncodingException e) {
        // If UTF8 is somehow not supported, we may as well bail.
        // Not a whole lot we can do without such support.
        throw new RuntimeException("Unexpected error: UTF8 not supported.");
      }
    }
    return buf.toString();
  }

  /**
   * Appends libs to the query string.
   * @param libs
   * @param query
   */
  private void appendLibsToQuery(String[] libs, StringBuilder query) {
    query.append("&")
         .append(LIBS_PARAM_NAME)
         .append("=")
         .append(servletState.getJsUrl(libs));
  }

  /**
   * @param req
   * @return Whether or not to use caja.
   */
  protected boolean getUseCaja(HttpServletRequest req) {
    String cajaParam = req.getParameter(CAJA_PARAM);
    return "1".equals(cajaParam);
  }
}
