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
package org.apache.shindig.social;

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Name.Field.html
 *
 */
public class Name extends AbstractSocialData {
  private String additionalName;
  private String familyName;
  private String givenName;
  private String honorificPrefix;
  private String honorificSuffix;
  private String unstructured;

  public Name(String unstructured) {
    this.unstructured = unstructured;
  }

  public String getUnstructured() {
    return unstructured;
  }

  public void setUnstructured(String unstructured) {
    this.unstructured = unstructured;
  }

  public String getAdditionalName() {
    return additionalName;
  }

  public void setAdditionalName(String additionalName) {
    this.additionalName = additionalName;
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getHonorificPrefix() {
    return honorificPrefix;
  }

  public void setHonorificPrefix(String honorificPrefix) {
    this.honorificPrefix = honorificPrefix;
  }

  public String getHonorificSuffix() {
    return honorificSuffix;
  }

  public void setHonorificSuffix(String honorificSuffix) {
    this.honorificSuffix = honorificSuffix;
  }
}
