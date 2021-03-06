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

package com.facebook.giraph.hive.impl.input;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import com.facebook.giraph.hive.HiveRecord;
import com.facebook.giraph.hive.input.HiveApiInputObserver;

/**
 * An input observer that does nothing
 */
public class NoOpInputObserver implements HiveApiInputObserver {
  /** Singleton */
  private static final NoOpInputObserver INSTANCE = new NoOpInputObserver();

  /** Constructor */
  protected NoOpInputObserver() { }

  /**
   * Get singleton instance
   *
   * @return singleton instance
   */
  public static NoOpInputObserver get() {
    return INSTANCE;
  }

  @Override
  public void beginReadRow() { }

  @Override
  public void endReadRow(WritableComparable key, Writable value) { }

  @Override
  public void hiveReadRowFailed() { }

  @Override
  public void beginParse() { }

  @Override
  public void endParse(HiveRecord record) { }
}
