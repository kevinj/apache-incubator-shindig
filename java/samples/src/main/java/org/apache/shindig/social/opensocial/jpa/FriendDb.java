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
package org.apache.shindig.social.opensocial.jpa;

import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.jpa.api.DbObject;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.Person;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import java.util.Map;

/**
 * Fiends relates users to one another with attributes.
 */
public class FriendDb implements DbObject {
  /**
   * The internal object ID used for references to this object. Should be generated by the
   * underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  protected long objectId;

  /**
   * An optimistic locking field.
   */
  @Version
  @Column(name = "version")
  protected long version;

  /**
   * Someone asserts the friendship.
   */
  @ManyToOne(targetEntity = PersonDb.class)
  @JoinColumn(name = "person_id", referencedColumnName = "oid")
  protected Person person;

  /**
   * Someone else is the recipient of the friendship.
   */
  @ManyToOne(targetEntity = PersonDb.class)
  @JoinColumn(name = "friend_id", referencedColumnName = "oid")
  protected Person friend;

  /**
   * The friendship has properties.
   */
  @OneToMany(targetEntity = FriendPropertyDb.class, mappedBy = "friend")
  @MapKey(name = "type")
  protected Map<String, ListField> properties;

  /**
   * @return the person
   */
  public Person getPerson() {
    return person;
  }

  /**
   * @param person the person to set
   */
  public void setPerson(Person person) {
    this.person = person;
  }

  /**
   * @return the friend
   */
  public Person getFriend() {
    return friend;
  }

  /**
   * @param friend the friend to set
   */
  public void setFriend(Person friend) {
    this.friend = friend;
  }

  /**
   * @return the properties
   */
  public Map<String, ListField> getProperties() {
    return properties;
  }

  /**
   * @param properties the properties to set
   */
  public void setProperties(Map<String, ListField> properties) {
    this.properties = properties;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * @return the version
   */
  public long getVersion() {
    return version;
  }

}
