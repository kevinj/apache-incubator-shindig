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
package org.apache.shindig.gadgets.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSerializer;
import org.apache.shindig.gadgets.parse.nekohtml.SocialMarkupHtmlParser;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateProcessor;

import java.io.IOException;
import java.util.Map;

import javax.el.ELResolver;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Maps;

/**
 * Unit tests for TemplateProcessor.
 * TODO: Refactor to remove boilerplate.
 * TODO: Add tests for special vars.
 * TODO: Add test for @var in @repeat loops. 
 */
public class TemplateProcessorTest {

  private Expressions expressions;

  private TemplateContext context;
  private TemplateProcessor processor;
  private Map<String, JSONObject> variables;
  private ELResolver resolver;

  private SocialMarkupHtmlParser parser;
  
  @Before
  public void setUp() throws Exception {
    expressions = new Expressions();
    variables = Maps.newHashMap();
    processor = new TemplateProcessor(expressions);
    resolver = new RootELResolver();
    parser = new SocialMarkupHtmlParser(new ParseModule.DOMImplementationProvider().get());    
    context = new TemplateContext(variables);
    
    addVariable("foo", new JSONObject("{ title: 'bar' }"));
    addVariable("user", new JSONObject("{ id: '101', name: { first: 'John', last: 'Doe' }}"));
    addVariable("toys", new JSONObject("{ list: [{name: 'Ball'}, {name: 'Car'}]}"));
    addVariable("xss", new JSONObject("{ script: '<script>alert();</script>'," +
    		"quote:'\"><script>alert();</script>'}"));
  }

  @Test
  public void testTextNode() throws Exception {
    String output = executeTemplate("${foo.title}");
    assertEquals("bar", output);
  }
  
  @Test
  public void testPlainText() throws Exception {
    // Verify that plain text is not interfered with, or incorrectly escaped
    String output = executeTemplate("<span>foo&amp;&bar</span>");
    assertEquals("<span>foo&amp;&bar</span>", output);
  }

  @Test
  public void testTextNodeEscaping() throws Exception {
    String output = executeTemplate("${xss.script}");
    assertFalse("Escaping not performed: \"" + output + "\"", output.contains("<script>alert("));
  }
  
  @Test
  public void testAppending() throws Exception {
    String output = executeTemplate("${user.id}${user.name.first}");
    assertEquals("101John", output);
    
    output = executeTemplate("foo${user.id}bar${user.name.first}baz");
    assertEquals("foo101barJohnbaz", output);

    output = executeTemplate("foo${user.nope}bar${user.nor}baz");
    assertEquals("foobarbaz", output);
  }
  
  @Test
  public void testEscapedExpressions() throws Exception {
    String output = executeTemplate("\\${escaped}");
    assertEquals("${escaped}", output);

    output = executeTemplate("foo\\${escaped}bar");
    assertEquals("foo${escaped}bar", output);
  }

  @Test
  public void testElement() throws Exception {
    String output = executeTemplate("<span title=\"${user.id}\">${user.name.first} baz</span>");
    assertEquals("<span title=\"101\">John baz</span>", output);
  }

  @Test
  public void testAttributeEscaping() throws Exception {
    String output = executeTemplate("<span title=\"${xss.quote}\">${user.name.first} baz</span>");
    assertFalse(output.contains("\"><script>alert("));
  }

  @Test
  public void testRepeat() throws Exception {
    String output = executeTemplate("<span repeat=\"${toys}\">${name}</span>");
    assertEquals("<span>Ball</span><span>Car</span>", output);
  }
  
  @Test
  public void testConditional() throws Exception {
    String output = executeTemplate(
        "<span repeat=\"${toys}\">" +
          "<span if=\"${name == 'Car'}\">Car</span>" +
          "<span if=\"${name != 'Car'}\">Not Car</span>" +
        "</span>");
    assertEquals("<span><span>Not Car</span></span><span><span>Car</span></span>", output);
  }

  private String executeTemplate(String markup) throws Exception {
    Element template = prepareTemplate(markup);
    DocumentFragment result = processor.processTemplate(template, context, resolver);
    return serialize(result);
  }
  
  private Element prepareTemplate(String markup) throws GadgetException {
    String content = "<script type=\"text/os-template\">" + markup + "</script>";
    Document document = parser.parseDom(content);
    return (Element) document.getElementsByTagName("script").item(0);
  }
  
  private String serialize(Node node) throws IOException {
    StringBuilder sb = new StringBuilder();
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      NekoSerializer.serialize(child, sb);
    }
    return sb.toString();
  }
  
  private void addVariable(String key, JSONObject value) {
    variables.put(key, value);
  }
}
