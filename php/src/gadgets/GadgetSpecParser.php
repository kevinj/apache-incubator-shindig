<?php
/**
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

class GadgetSpecException extends Exception {
}

/**
 * Parses the XML content into a GadgetSpec object
 */
class GadgetSpecParser {
  /**
   * Parses the $xmlContent into a Gadget class
   *
   * @param string $xmlContent
   */
  public function parse($xmlContent) {
    libxml_use_internal_errors(true);
    $doc = new DOMDocument();
    if (! $doc->loadXML($xmlContent, LIBXML_NOCDATA)) {
      $errors = libxml_get_errors();
      $errorStr = '';
      foreach ($errors as $error) {
        $errorStr .= $error . " \n";
      }
      libxml_clear_errors();
      throw new GadgetSpecException("Error parsing gadget xml:\n$errorStr");
    }
    //TODO: we could do a XSD schema validation here, but both the schema and most of the gadgets seem to have bugs, so it's not really practical yet (and slow)
    // $doc->schemaValidate('gadget.xsd');
    $gadget = new GadgetSpec();
    $gadget->checksum = md5($xmlContent);
    $this->parseModulePrefs($doc, $gadget);
    $this->parseLinks($doc, $gadget);
    $this->parseUserPrefs($doc, $gadget);
    $this->parseViews($doc, $gadget);
    //TODO
    // OAuthService / OAuthSpec
    // PipelinedData
    return $gadget;
  }

  /**
   * Parse the gadget views
   *
   * @param DOMDocument $doc
   * @param GadgetSpec $gadget
   */
  private function parseViews(DOMDocument &$doc, GadgetSpec &$gadget) {
    $views = $doc->getElementsByTagName('Content');
    if (! $views || $views->length < 1) {
      throw new GadgetSpecException("A gadget needs to have at least one view");
    }
    $gadget->views = array();
    foreach ($views as $viewNode) {
      if ($viewNode->getAttribute('type' == 'url') && $viewNode->getAttribute('href') == null) {
        throw new GadgetSpecException("Malformed <Content> href value");
      }
      foreach (explode(',', $viewNode->getAttribute('view')) as $view) {
        $view = trim($view);
        if (isset($gadget->views[$view])) {
          $gadget->views[$view]['content'] .= $viewNode->nodeValue;
        } else {
          $gadget->views[$view] = array('view' => $view, 'type' => strtoupper($viewNode->getAttribute('type')), 'href' => $viewNode->getAttribute('href'), 'preferedHeight' => $viewNode->getAttribute('prefered_height'),
              'preferedWidth' => $viewNode->getAttribute('prefered_width'), 'quirks' => $viewNode->getAttribute('quirks'), 'content' => $viewNode->nodeValue);
        }
      }
    }
  }

  /**
   * Parses the UserPref entries
   *
   * @param DOMDocument $doc
   * @param GadgetSpec $gadget
   */
  private function parseUserPrefs(DOMDocument &$doc, GadgetSpec &$gadget) {
    $gadget->userPrefs = array();
    if (($userPrefs = $doc->getElementsByTagName('UserPref')) != null) {
      foreach ($userPrefs as $prefNode) {
        $pref = array('name' => $prefNode->getAttribute('name'), 'displayName' => $prefNode->getAttribute('display_name'), 'datatype' => strtoupper($prefNode->getAttribute('datatype')), 'defaultValue' => $prefNode->getAttribute('default_value'),
            'required' => $prefNode->getAttribute('required'));
        if ($pref['datatype'] == 'ENUM') {
          if (($enumValues = $prefNode->getElementsByTagName('EnumValue')) != null) {
            $enumVals = array();
            foreach ($enumValues as $enumNode) {
              $enumVals[] = array('value' => $enumNode->getAttribute('value'), 'displayValue' => $enumNode->getAttribute('display_value'));
            }
          }
          $pref['enumValues'] = $enumVals;
        }
        $gadget->userPrefs[] = $pref;
      }
    }
  }

  /**
   * Parses the link spec elements
   *
   * @param DOMDocument $doc
   * @param GadgetSpec $gadget
   */
  private function parseLinks(DOMDocument &$doc, GadgetSpec &$gadget) {
    $gadget->links = array();
    if (($links = $doc->getElementsByTagName('link')) != null) {
      foreach ($links as $linkNode) {
        $gadget->links[] = array('rel' => $linkNode->getAttribute('rel'), 'href' => $linkNode->getAttribute('href'), 'method' => strtoupper($linkNode->getAttribute('method')));
      }
    }
  }

  /**
   * Parses the ModulePrefs section of the xml structure. The ModulePrefs
   * section is required, so if it's missing or if there's 2 an GadgetSpecException will be thrown.
   *
   * This function also parses the ModulePref's child elements (Icon, Features, Preload and Locale)
   *
   * @param DOMDocument $doc
   */
  private function parseModulePrefs(DOMDocument &$doc, GadgetSpec &$gadget) {
    $modulePrefs = $doc->getElementsByTagName("ModulePrefs");
    if ($modulePrefs->length < 1) {
      throw new GadgetSpecException("Missing ModulePrefs block");
    } elseif ($modulePrefs->length > 1) {
      throw new GadgetSpecException("More then one ModulePrefs block found");
    }
    $modulePrefs = $modulePrefs->item(0);
    // parse the ModulePrefs attributes
    $knownAttributes = array('title', 'author', 'authorEmail', 'description', 'directoryTitle', 'screenshot', 'thumbnail', 'titleUrl', 'authorAffiliation', 'authorLocation', 'authorPhoto', 'authorAboutme', 'authorQuote', 'authorLink', 'showStats',
        'showInDirectory', 'string', 'width', 'height', 'category', 'category2', 'singleton', 'renderInline', 'scaling', 'scrolling');
    foreach ($modulePrefs->attributes as $key => $attribute) {
      $attrValue = trim($attribute->value);
      // var format conversion from directory_title => directoryTitle
      $attrKey = str_replace(' ', '', ucwords(str_replace('_', ' ', $key)));
      $attrKey[0] = strtolower($attrKey[0]);
      if (in_array($attrKey, $knownAttributes)) {
        $gadget->$attrKey = $attrValue;
      }
    }
    // And parse the child nodes
    $this->parseIcon($modulePrefs, $gadget);
    $this->parseFeatures($modulePrefs, $gadget);
    $this->parsePreloads($modulePrefs, $gadget);
    $this->parseLocales($modulePrefs, $gadget);
  }

  /**
   * Parses the (optional) Icon element, returns a Icon class or null
   *
   * @param DOMElement $modulePrefs
   * @param Gadget $gadget
   */
  private function parseIcon(DOMElement &$modulePrefs, GadgetSpec &$gadget) {
    if (($iconNodes = $modulePrefs->getElementsByTagName('Icon')) != null) {
      if ($iconNodes->length > 1) {
        throw new GadgetSpecException("A gadget can only have one Icon element");
      } elseif ($iconNodes->length == 1) {
        $icon = $iconNodes->item(0);
        $gadget->icon = $icon->nodeValue;
      }
    }
  }

  /**
   * Parses the Required and Optional feature entries in the ModulePrefs
   *
   * @param DOMElement $modulePrefs
   * @param Gadget $gadget
   */
  private function parseFeatures(DOMElement &$modulePrefs, GadgetSpec &$gadget) {
    $gadget->requiredFeatures = $gadget->optionalFeatures = array();
    if (($requiredNodes = $modulePrefs->getElementsByTagName('Require')) != null) {
      foreach ($requiredNodes as $requiredFeature) {
        $gadget->requiredFeatures[] = $requiredFeature->getAttribute('feature');
      }
    }
    if (($optionalNodes = $modulePrefs->getElementsByTagName('Optional')) != null) {
      foreach ($optionalNodes as $optionalFeature) {
        $gadget->optionalFeatures[] = $optionalFeature->getAttribute('feature');
      }
    }
  }

  /**
   * Parses the preload elements
   *
   * @param DOMElement $modulePrefs
   * @param Gadget $gadget
   */
  private function parsePreloads(DOMElement &$modulePrefs, GadgetSpec &$gadget) {
    $gadget->preloads = array();
    if (($preloadNodes = $modulePrefs->getElementsByTagName('Preload')) != null) {
      foreach ($preloadNodes as $node) {
        $gadget->preloads[] = array('href' => $node->getAttribute('href'), 'authz' => strtoupper($node->getAttribute('authz')), 'signViewer' => $node->getAttribute('sign_viewer'), 'signOwner' => $node->getAttribute('sign_owner'));
      }
    }
  }

  /**
   * Parses the Locale (message bundle) entries
   *
   * @param DOMElement $modulePrefs
   * @param Gadget $gadget
   */
  private function parseLocales(DOMElement &$modulePrefs, GadgetSpec &$gadget) {
    $gadget->locales = array();
    if (($localeNodes = $modulePrefs->getElementsByTagName('Locale')) != null) {
      foreach ($localeNodes as $node) {
        $messageBundle = array();
        if (($messageBundleNode = $node->getElementsByTagName('messagebundle')) != null && $messageBundleNode->length > 0) {
          // parse inlined messages
          $messageBundleNode = $messageBundleNode->item(0);
          $messages = $messageBundleNode->getElementsByTagName('msg');
          foreach ($messages as $msg) {
            $messageBundle[$msg->getAttribute('name')] = trim($msg->nodeValue);
          }
        }
        $lang = $node->getAttribute('lang') == '' ? 'all' : strtolower($node->getAttribute('lang'));
        $country = $node->getAttribute('country') == '' ? 'all' : strtoupper($node->getAttribute('country'));
        $gadget->locales[] = array('lang' => $lang, 'country' => $country, 'messages' => $node->getAttribute('messages'), 'languageDirection' => $node->getAttribute('language_direction'), 'messageBundle' => $messageBundle);
      }
    }
  }
}
