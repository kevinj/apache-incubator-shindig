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

package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet which concatenates the content of several proxied HTTP responses
 *
 * @see org.apache.shindig.gadgets.rewrite.HTMLContentRewriter
 */
public class ConcatProxyServlet extends InjectedServlet {

  public static final String JSON_PARAM = "json";

  private static final Logger logger
      = Logger.getLogger(ConcatProxyServlet.class.getName());

  private transient ProxyHandler proxyHandler;

  @Inject
  public void setProxyHandler(ProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
  }

  @SuppressWarnings("boxing")
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    // Avoid response splitting vulnerability
    String ct = request.getParameter(ProxyBase.REWRITE_MIME_TYPE_PARAM);
    if(ct != null && ct.indexOf('\r')<0 && ct.indexOf('\n')<0) {
      response.setHeader("Content-Type",
          request.getParameter(ProxyBase.REWRITE_MIME_TYPE_PARAM));
    }

    boolean ignoreCache = proxyHandler.getIgnoreCache(request);
    if (!ignoreCache && request.getParameter(ProxyBase.REFRESH_PARAM) != null) {
        HttpUtil.setCachingHeaders(response, Integer.valueOf(request
            .getParameter(ProxyBase.REFRESH_PARAM)));
    } else {
      HttpUtil.setNoCache(response);
    }
    
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");

    // Check for json concat
    String jsonVar = request.getParameter(JSON_PARAM);
    if (jsonVar != null && !jsonVar.matches("^\\w*$")) {
      response.getOutputStream().println(
          formatHttpError(HttpServletResponse.SC_BAD_REQUEST,
              "Bad json variable name " + jsonVar));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    ResponseWrapper wrapper = new ResponseWrapper(response, jsonVar);

    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      String url = request.getParameter(Integer.toString(i));
      if (url == null) {
        break;
      }
      try {

        wrapper.processUrl(url);
        proxyHandler.doFetch(new RequestWrapper(request, url), wrapper);

        if (wrapper.getStatus() != HttpServletResponse.SC_OK) {
          response.getOutputStream().println(
              formatHttpError(wrapper.getStatus(), wrapper.getErrorMessage()));
        }
        
      } catch (GadgetException ge) {
        if (ge.getCode() != GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT) {
          wrapper.done();
          outputError(ge, url, response);
          return;
        } else {
          response.getOutputStream().println("/* ---- End " + url + " 404 ---- */");
        }
      }
    }
    wrapper.done();
    response.setStatus(200);
  }

  private static String formatHttpError(int status, String errorMessage) {
    StringBuilder err = new StringBuilder();
    err.append("/* ---- Error ");
    err.append(status);
    if (errorMessage != null) {
      err.append(", ");
      err.append(errorMessage);
    }

    err.append(" ---- */");
    return err.toString();
  }

  private static void outputError(GadgetException excep, String url, HttpServletResponse resp)
      throws IOException {
    StringBuilder err = new StringBuilder();
    err.append(excep.getCode().toString());
    err.append(" concat(");
    err.append(url);
    err.append(") ");
    err.append(excep.getMessage());

    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Concat proxy request failed", err);
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, err.toString());
  }

  /**
   * Simple request wrapper to make repeated calls to ProxyHandler
   */
  private static class RequestWrapper extends HttpServletRequestWrapper {

    private final String url;

    protected RequestWrapper(HttpServletRequest httpServletRequest, String url) {
      super(httpServletRequest);
      this.url = url;
    }

    @Override
    public String getParameter(String paramName) {
      if (ProxyBase.URL_PARAM.equals(paramName)) {
        return url;
      }
      return super.getParameter(paramName);
    }
  }

  /**
   * Wrap the response to prevent writing through of the status code and to hold a reference to the
   * stream across multiple proxied parts
   * Handles json concatenation by using the EscapedServletOutputStream class
   * to escape the data
   */
  private static class ResponseWrapper extends HttpServletResponseWrapper {

    private ServletOutputStream outputStream;
    private EscapedServletOutputStream jsonStream;

    private int errorCode = SC_OK;
    private String errorMessage;
    /** Specify hash key for json concat **/ 
    private String jsonVar = null;
    private String url = null;

    protected ResponseWrapper(HttpServletResponse httpServletResponse,
        String jsonVar) throws IOException {
      super(httpServletResponse);
      if (jsonVar != null && jsonVar.length() > 0) {
        this.jsonVar = jsonVar;
        super.getOutputStream().println(jsonVar + "={");
      }
    }

    
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      // For errors, we don't want the content returned by the remote
      // server;  we'll just include an HTTP error code to avoid creating
      // syntactically invalid output overall.
      if (errorCode != SC_OK) {
        closeStream();
        outputStream = new NullServletOutputStream();
      }
      
      if (outputStream == null) {
        outputStream = super.getOutputStream();
      }
      
      return outputStream;
    }

    /**
     * Restart a new file to concat
     * Close previous file, and add start comment if not json concat
     */
    public void processUrl(String fileUrl) throws IOException {
      closeStream();
      errorCode = SC_OK;
      this.url = fileUrl;
      if (jsonVar == null) {
        super.getOutputStream().println("/* ---- Start " + url + " ---- */");
      } else {
        // Create escaping stream (make sure url variable is defined)
        jsonStream = new EscapedServletOutputStream();
        outputStream = jsonStream;
      }
    }

    /**
     * Add close of json hash
     */
    public void done() throws IOException {
      closeStream();
      if (jsonVar != null) {
        // Close json concat main variable
        super.getOutputStream().println("};");
      }
    }

    private void closeStream() throws IOException {
      if (jsonVar == null && outputStream != null) {
        outputStream.println("/* ---- End " + url + " ---- */");
      } else if (jsonStream != null) {
        byte[] data = jsonStream.getBytes();
        ServletOutputStream mainStream = super.getOutputStream();
        mainStream.print("\"" + url + "\":\"");
        mainStream.write(data);
        mainStream.println("\",");
      }      
      outputStream = null;
      jsonStream = null;
    }
    
    public int getStatus() {
      return errorCode;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    // Suppress headers
    @Override
    public void setDateHeader(String s, long l) {
    }

    @Override
    public void addDateHeader(String s, long l) {
    }

    @Override
    public void setHeader(String s, String s1) {
    }

    @Override
    public void addHeader(String s, String s1) {
    }

    @Override
    public void setIntHeader(String s, int i) {
    }

    @Override
    public void addIntHeader(String s, int i) {
    }

    @Override
    public void sendError(int i, String s) throws IOException {
      errorCode = i;
      errorMessage = s;
    }

    @Override
    public void sendError(int i) throws IOException {
      errorCode = i;
    }

    @Override
    public void sendRedirect(String s) throws IOException {
    }

    @Override
    public void setStatus(int i) {
    }

    @Override
    public void setStatus(int i, String s) {
    }

    @Override
    public void setContentLength(int i) {
    }

    @Override
    public void setContentType(String s) {
    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public void setLocale(Locale locale) {
    }

    @Override
    public void setCharacterEncoding(String s) {
    }
  }

  /**
   * Small ServletOutputStream class, overriding just enough to ensure
   * there's no output.
   */
  private static class NullServletOutputStream extends ServletOutputStream {

    protected NullServletOutputStream() {
    }

    @Override
    public void write(int b) throws IOException {
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
    }

    @Override
    public void write(byte b[]) throws IOException {
    }
  }
  
  /**
   * Override Servlet output stream to support json escaping of the stream data
   * Use getBytes to get the escaped data. 
   */
  private static class EscapedServletOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream tempStream;
    protected EscapedServletOutputStream() {
      tempStream = new ByteArrayOutputStream();
    }
    
    public byte[] getBytes() throws IOException {
      try {
        return StringEscapeUtils.escapeJavaScript(tempStream.toString("UTF8")).getBytes();
      } catch (UnsupportedEncodingException e) {
        // Need to return IOException since that what ServletOutputStream constructor do.
        throw new IOException("Unsuported encoding in data");
      }
    }

    @Override
    public void write(int b) throws IOException {
      tempStream.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
      tempStream.write(b, off, len);
    }

    @Override
    public void write(byte b[]) throws IOException {
      tempStream.write(b);
    }
  }

}

