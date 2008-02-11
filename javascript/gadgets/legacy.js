/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 // All functions in this file should be treated as deprecated legacy routines.
 // Gadget authors are explicitly discouraged from using any of them.

var _IG_Prefs = gadgets.Prefs;

// Yes, these technically modifiy gadget.Prefs as well. Unfortunately,
// simply setting IG_Prefs.prototype to a new gadgets.Prefs object means
// that we'd have to duplicate the gadgets.Prefs constructor.
_IG_Prefs._parseURL = gadgets.Prefs.parseUrl;

function _IG_Fetch_wrapper(callback, obj) {
  callback(obj.data);
}

function _IG_FetchContent(url, callback, opt_params) {
  var params = opt_params || {};
  params.type = gadgets.IO.ContentType.TEXT;
  var cb = gadgets.Util.makeClosure(null, _IG_Fetch_wrapper, callback);
  gadgets.IO.makeRequest(url, cb, params);
}

function _IG_FetchXmlContent(url, callback, opt_params) {
  var params = opt_params || {};
  params.type = gadgets.IO.ContentType.XML;
  var cb = gadgets.Util.makeClosure(null, _IG_Fetch_wrapper, callback);
  gadgets.IO.makeRequest(url, cb, params);
}

function _IG_FetchFeedAsJSON_cb(callback, obj) {
  if (obj.data.fr_1) {
    callback(obj.data.fr_1);
  } else {
    callback(null);
  }
}

// NOTE: this implementation does not batch calls as is the case on igoogle.
function _IG_FetchFeedAsJSON(url, callback, numItems, getDescriptions,
                             opt_params) {
  var params = opt_params || {};
  // TODO: this no longer works. The proxy needs to support POST requests
  // to make it work.
  var finalUrl = "http://www.gmodules.com/ig/feedjson?fr_1=";
  finalUrl += gadgets.IO.encodeValues({
    url: encodeURIComponent(url),
    val: numItems,
    sum: getDescriptions ? 1 : 0
  });
  params.type = gadgets.IO.ContentType.JSON;
  var cb = gadgets.Util.makeClosure(null, _IG_FetchFeedAsJSON_cb, callback);
  gadgets.IO.makeRequest(finalUrl, cb, params);
}

function _IG_GetCachedUrl(url) {
  return gadgets.IO.getProxyUrl(url);
}
function _IG_GetImageUrl(url) {
  return gadgets.IO.getProxyUrl(url);
}

function _IG_RegisterOnloadHandler(callback) {
  window.onload = callback;
}

var _args = gadgets.Util.getParameters;

/**
 * Fetches an object by document id.
 *
 * @param {String | Object} el The element you wish to fetch. You may pass
 *     an object in which allows this to be called regardless of whether or
 *     not the type of the input is known.
 * @return {HTMLElement} The element, if it exists in the document, or null.
 */
function _gel(el) {
  return document.getElementById ? document.getElementById(el) : null;
}

/**
 * Fetches elements by tag name.
 * This is functionally identical to document.getElementsByTagName()
 *
 * @param {String} tag The tag to match elements against.
 * @return {Array.<HTMLElement>} All elements of this tag type.
 */
function _gelstn(tag) {
  if (n === "*" && document.all) {
    return document.all;
  }
  return document.getElementsByTagName ?
         document.getElementsByTagName(n) : [];
}

/**
 * Fetches elements with ids matching a given regular expression.
 *
 * @param {tagName} tag The tag to match elements against.
 * @param {RegEx} regex The expression to match.
 * @return {Array.<HTMLElement>} All elements of this tag type that match
 *     regex.
 */
function _gelsbyregex(tagName, regex) {
  var matchingTags = _gelstn(tagName);
  var matchingRegex = [];
  for (var i = 0, j = matchingTags.length; i < j; ++i) {
    if (regex.test(matchingTags[i].id)) {
      matchingRegex.push(matchingTags[i]);
    }
  }
  return matchingRegex;
}

/**
 * URI escapes the given string.
 * @param {String} str The string to escape.
 * @return {String} The escaped string.
 */
function _esc(str) {
  return window.encodeURIComponent ? encodeURIComponent(str) : escape(str);
}

/**
 * URI unescapes the given string.
 * @param {String} str The string to unescape.
 * @return {String} The unescaped string.
 */
function _unesc(str) {
  return window.decodeURIComponent ? decodeURIComponent(str) : unescape(str);
}

/**
 * Encodes HTML entities such as <, " and >.
 *
 * @param {String} str The string to escape.
 * @return The escaped string.
 */
function _hesc(str) {
  // '<' and '>'
  str = str.replace(/</g, "&lt;").replace(/>/g, "&gt;");
  // '"' and '
  str = str.replace(/"/g, "&quot;").replace(/'/g, "&#39;");

  return str;
}

/**
 * Removes HTML tags from the given input string.
 *
 * @param {String} str The string to strip.
 * @return The stripped string.
 */
function _striptags(str) {
  return s.replace(/<\/?[^>]+>/g, "");
}

/**
 * Trims leading & trailing whitespace from the given string.
 *
 * @param {String} str The string to trim.
 * @return {String} The trimmed string.
 */
function _trim(str) {
  return str.replace(/^\s+|\s+$/g, "");
}

/**
 * Toggles the given element between being shown and block-style display.
 *
 * @param {String | HTMLElement} el The element to toggle.
 */
function _toggle(el) {
  el = _gel(el);
  if (el !== null) {
    if (el.style.display.length === 0 || el.style.display === "block") {
      el.style.display = "none";
    } else if (el.style.display === "none") {
      el.style.display = "block";
    }
  }
}

/**
 * {Number} A counter used by uniqueId().
 */
var _global_legacy_uidCounter = 0;

/**
 * @return a unique number.
 */
function _uid() {
  return _global_legacy_uidCounter++;
}

/**
 * @param {Number} a
 * @param {Number} b
 * @return The lesser of a or b.
 */
function _min(a, b) {
  return (a < b ? a : b);
}

/**
 * @param {Number} a
 * @param {Number} b
 * @return The greater of a or b.
 */
function _max(a, b) {
  return (a > b ? a : b);
}
