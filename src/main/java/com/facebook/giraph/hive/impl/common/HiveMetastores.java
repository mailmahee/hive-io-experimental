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

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

/**
 * Wrapper around Thrift MetasStore client with helper methods.
 */
public class HiveMetastores {
  /** Connect timeout in milliseconds */
  public static final int DEFAULT_TIMEOUT_MS = 20 * 1000;

  /** Logger */
  private static final Logger LOG = Logger.getLogger(HiveMetastores.class);

  /** Don't construct */
  private HiveMetastores() { }

  /**
   * Create client from host and port with timeout
   * @param host Host to connect to
   * @param port Port to connect on
   * @param timeoutMillis Socket timeout
   * @return Hive Thrift Metastore client
   * @throws TTransportException Connection errors
   */
  public static ThriftHiveMetastore.Iface create(String host, int port,
    int timeoutMillis) throws TTransportException {
    TTransport transport = new TSocket(host, port, timeoutMillis);
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    return new ThriftHiveMetastore.Client(protocol);
  }

  /**
   * Create client from host and port with default timeout
   * @param host Host to connect to
   * @param port Port to connect to
   * @return Thrift Hive interface connected to host:port
   * @throws TTransportException network problems
   */
  public static ThriftHiveMetastore.Iface create(String host, int port)
    throws TTransportException {
    return create(host, port, DEFAULT_TIMEOUT_MS);
  }

  /**
   * Create client from a HiveConf.
   *
   * First we try to read the URIs out of the HiveConf and connect to those. If
   * that fails we create Hive's client and grab the thrift connection out of
   * that using reflection.
   *
   * @param hiveConf HiveConf to use
   * @return Thrift Hive client
   * @throws TException network problems
   */
  public static ThriftHiveMetastore.Iface create(HiveConf hiveConf)
    throws TException {
    ThriftHiveMetastore.Iface client = createFromURIs(hiveConf);
    if (client == null) {
      client = createfromReflection(hiveConf);
    }
    return client;
  }

  /**
   * Create client by instantiating Hive's own client and using relfection to
   * grab the thrift client out of that.
   *
   * @param hiveConf HiveConf to use
   * @return Thrift Hive client
   * @throws TException network problems
   */
  private static ThriftHiveMetastore.Iface createfromReflection(
    HiveConf hiveConf) throws TException  {
    HiveMetaStoreClient hiveClient;
    try {
      hiveClient = new HiveMetaStoreClient(hiveConf);
    } catch (MetaException e) {
      throw new TException(e);
    }

    Field clientField;
    try {
      clientField = hiveClient.getClass().getDeclaredField("client");
    } catch (NoSuchFieldException e) {
      throw new TException(e);
    }
    clientField.setAccessible(true);

    ThriftHiveMetastore.Iface thriftIface;
    try {
      thriftIface = (ThriftHiveMetastore.Iface) clientField.get(hiveClient);
    } catch (IllegalAccessException e) {
      throw new TException(e);
    }

    return thriftIface;
  }

  /**
   * Create Hive client from URIs in HiveConf
   *
   * @param hiveConf HiveConf to use
   * @return Thrift Hive client, or null if could not make one out of URIs
   */
  private static ThriftHiveMetastore.Iface createFromURIs(HiveConf hiveConf) {
    List<URI> uris = HiveUtils.getURIs(hiveConf, HiveConf.ConfVars.METASTOREURIS);
    if (uris.isEmpty()) {
      LOG.warn("No Hive Metastore URIs to connect to");
      return null;
    }
    for (URI uri : uris) {
      try {
        return create(uri.getHost(), uri.getPort());
      } catch (TTransportException e) {
        LOG.error("Failed to connect to " + uri.getHost() +
            ":" + uri.getPort(), e);
      }
    }
    return null;
  }
}
