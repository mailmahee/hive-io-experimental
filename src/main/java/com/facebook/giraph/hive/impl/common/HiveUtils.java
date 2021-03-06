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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.giraph.hive.impl.common;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.Warehouse;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.serde2.ColumnProjectionUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Functions.forMap;
import static com.google.common.collect.Lists.transform;

/**
 * Utilities for dealing with Hive
 */
public class HiveUtils {
  /**
   * Function for getting the name from FieldSchema
   */
  public static final Function<FieldSchema, String> FIELD_SCHEMA_NAME_GETTER =
      new Function<FieldSchema, String>() {
        @Override
        public String apply(FieldSchema input) {
          return input == null ? null : input.getName();
        }
      };

  /** Logger */
  private static final Logger LOG = Logger.getLogger(HiveUtils.class);

  /** Don't construct, allow inheritance */
  protected HiveUtils() { }

  /**
   * Get Configuration value as list of URIs.
   *
   * @param conf Configuration to use
   * @param key String key to lookup
   * @return URI list from value
   */
  public static List<URI> getURIs(HiveConf conf, HiveConf.ConfVars key) {
    String[] parts = conf.getVar(key).split(",");
    List<URI> uris = Lists.newArrayList();
    for (int i = 0; i < parts.length; ++i) {
      URI uri;
      try {
        uri = new URI(parts[i]);
      } catch (URISyntaxException e) {
        LOG.error("URI syntax error", e);
        continue;
      }
      if (uri.getScheme() == null) {
        LOG.error("URI '" + parts[i] + "' from key " + key +
            " does not have a scheme");
      } else {
        uris.add(uri);
      }
    }
    return uris;
  }

  /**
   * Get list of partition values in same order as partition keys passed in.
   * @param partitionKeys list of keys to grab
   * @param partitionValuesMap map of partition values
   * @return list of partition values
   */
  public static List<String> orderedPartitionValues(
      List<FieldSchema> partitionKeys, Map<String, String> partitionValuesMap) {
    List<String> partitionNames = transform(partitionKeys, FIELD_SCHEMA_NAME_GETTER);
    return transform(partitionNames, forMap(partitionValuesMap));
  }

  /**
   * Compute path to Hive partition
   * @param partitionKeys list of partition fields
   * @param partitionValuesMap partition values
   * @return path to partition for Hive table
   * @throws MetaException Hive meta issues
   */
  public static String computePartitionPath(List<FieldSchema> partitionKeys,
      Map<String, String> partitionValuesMap) throws MetaException {
    List<String> values = orderedPartitionValues(partitionKeys,
        partitionValuesMap);
    return Warehouse.makePartName(partitionKeys, values);
  }

  /**
   * Set ids of columns we're reading. Used by things like RCFile for skipping
   * other columns which we don't need.
   * @param conf Configuration to use
   * @param columnIds list of column ids
   */
  public static void setReadColumnIds(Configuration conf,
                                      List<Integer> columnIds) {
    if (columnIds.isEmpty()) {
      ColumnProjectionUtils.setFullyReadColumns(conf);
    } else {
      ColumnProjectionUtils.setReadColumnIDs(conf, columnIds);
    }
  }

  /**
   * Set number of columns we're writing for RCFile's knowledge
   * @param conf Configuration to use
   * @param numColumns integer number of columns writing
   */
  public static void setRCileNumColumns(Configuration conf, int numColumns) {
    conf.set("hive.io.rcfile.column.number.conf",
        Integer.toOctalString(numColumns));
  }
}
