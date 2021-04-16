/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.solr.cluster.api.SimpleMap;
import org.apache.solr.common.util.WrappedSimpleMap;

import static org.apache.solr.common.ConfigNode.Helpers.*;

/**
 * A generic interface that represents a config file, mostly XML
 */
public interface ConfigNode {
  ThreadLocal<Function<String,String>> SUBSTITUTES = new ThreadLocal<>();

  /**
   * Name of the tag
   */
  String name();

  /**
   * Text value of the node
   */
  String textValue();

  /**
   * Attributes
   */
  SimpleMap<String> attributes();

  /**
   * Child by name
   */
  default ConfigNode child(String name) {
    return child(null, name);
  }

  /**
   * Child by name or return an empty node if null
   * if there are multiple values , it returns the first elem
   * This never returns a null
   */
  default ConfigNode __(String name) {
    ConfigNode child = child(null, name);
    return child == null? EMPTY: child;
  }
  default ConfigNode __(String name, Predicate<ConfigNode> test) {
    List<ConfigNode> children = children(test, name);
    if(children.isEmpty()) return EMPTY;
    return children.get(0);

  }

  default ConfigNode child(List<String> path) {
    ConfigNode node = this;
    for (String s : path) {
      node = node.child(s);
      if (node == null) break;
    }
    return node;
  }

  default ConfigNode child(String name, Supplier<RuntimeException> err) {
    ConfigNode n = child(name);
    if(n == null) throw err.get();
    return n;
  }

  default boolean _bool(boolean def) { return __bool(textValue(),def); }
  default int _int(int def) { return __int(textValue(), def); }
  default String attr(String name, String def) { return __txt(attributes().get(name), def);}
  default String attr(String name) { return attributes().get(name);}
  default String requiredStrAttr(String name, Supplier<RuntimeException> err) {
    if(attributes().get(name) == null && err != null) throw err.get();
    return attributes().get(name);
  }
  default int intAttr(String name, int def) { return __int(attributes().get(name), def); }
  default boolean boolAttr(String name, boolean def){ return __bool(attributes().get(name), def); }
  default String txt(String def) { return textValue() == null ? def : textValue();}
  default String txt() { return textValue();}
  default double doubleVal(double def){ return __double(textValue(), def); }
  /**Iterate through child nodes with the name and return the first child that matches
   */
  default ConfigNode child(Predicate<ConfigNode> test, String name) {
    ConfigNode[] result = new ConfigNode[1];
    forEachChild(it -> {
      if (name!=null && !name.equals(it.name())) return Boolean.TRUE;
      if (test == null || test.test(it)) {
        result[0] = it;
        return Boolean.FALSE;
      }
      return Boolean.TRUE;
    });
    return result[0];
  }

  /**Iterate through child nodes with the names and return all the matching children
   * @param nodeNames names of tags to be returned
   * @param  test check for the nodes to be returned
   */
  default List<ConfigNode> children(Predicate<ConfigNode> test, String... nodeNames) {
    return children(test, nodeNames == null ? Collections.emptySet() : Set.of(nodeNames));
  }

  /**Iterate through child nodes with the names and return all the matching children
   * @param matchNames names of tags to be returned
   * @param  test check for the nodes to be returned
   */
  default List<ConfigNode> children(Predicate<ConfigNode> test, Set<String> matchNames) {
    List<ConfigNode> result = new ArrayList<>();
    forEachChild(it -> {
      if (matchNames != null && !matchNames.isEmpty() && !matchNames.contains(it.name())) return Boolean.TRUE;
      if (test == null || test.test(it)) result.add(it);
      return Boolean.TRUE;
    });
    return result;
  }

  default List<ConfigNode> children(String name) {
    return children(null, Collections.singleton(name));
  }

  default boolean exists() {
    return true;
  }

  default <T> T compute(Function<ConfigNode, T> ifNotNull, Supplier<T> ifNull) {
    return ifNotNull.apply(this);
  }

  /** abortable iterate through children
   *
   * @param fun consume the node and return true to continue or false to abort
   */
  void forEachChild(Function<ConfigNode, Boolean> fun);

  ConfigNode EMPTY = new ConfigNode() {
    @Override
    public String name() {
      return null;
    }

    @Override
    public String textValue() { return null; }

    @Override
    public SimpleMap<String> attributes() {
      return empty_attrs;
    }

    @Override
    public String attr(String name) { return null; }

    @Override
    public String attr(String name, String def) { return def; }

    @Override
    public ConfigNode child(String name) { return null; }

    @Override
    public ConfigNode __(String name) {
      return EMPTY;
    }

    public boolean exists() { return false; }

    @Override
    public <T> T compute(Function<ConfigNode, T> ifNotNull, Supplier<T> ifNull) { return ifNull.get(); }

    @Override
    public void forEachChild(Function<ConfigNode, Boolean> fun) { }
  } ;
  SimpleMap<String> empty_attrs = new WrappedSimpleMap<>(Collections.emptyMap());

  class Helpers {
    static boolean __bool(Object v, boolean def) { return v == null ? def : Boolean.parseBoolean(v.toString()); }
    static String __txt(Object v, String def) { return v == null ? def : v.toString(); }
    static int __int(Object v, int def) { return v==null? def: Integer.parseInt(v.toString()); }
    static double __double(Object v, double def) { return v == null ? def: Double.parseDouble(v.toString()); }
  }

}
