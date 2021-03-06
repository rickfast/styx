/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.support.configuration

import java.util.concurrent.TimeUnit.MILLISECONDS

import com.hotels.styx.api.client.ConnectionPool
import com.hotels.styx.client.connectionpool.ConnectionPoolSettings._


case class ConnectionPoolSettings(maxConnectionsPerHost: Int = DEFAULT_MAX_CONNECTIONS_PER_HOST,
                                  maxPendingConnectionsPerHost: Int = DEFAULT_MAX_PENDING_CONNECTIONS_PER_HOST,
                                  connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
                                  socketTimeoutMillis: Int = DEFAULT_SOCKET_TIMEOUT_MILLIS,
                                  pendingConnectionTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS
                               ) {
  def asJava: com.hotels.styx.client.connectionpool.ConnectionPoolSettings = new com.hotels.styx.client.connectionpool.ConnectionPoolSettings.Builder()
      .maxConnectionsPerHost(maxConnectionsPerHost)
      .maxConnectionsPerHost(maxPendingConnectionsPerHost)
      .connectTimeout(connectTimeoutMillis, MILLISECONDS)
      .socketTimeout(socketTimeoutMillis, MILLISECONDS)
      .pendingConnectionTimeout(pendingConnectionTimeoutMillis, MILLISECONDS)
    .build()
}

object ConnectionPoolSettings {
  def fromJava(from: ConnectionPool.Settings): ConnectionPoolSettings =
    ConnectionPoolSettings(
      maxConnectionsPerHost = from.maxConnectionsPerHost,
      maxPendingConnectionsPerHost = from.maxPendingConnectionsPerHost,
      connectTimeoutMillis = from.connectTimeoutMillis(),
      socketTimeoutMillis = from.socketTimeoutMillis(),
      pendingConnectionTimeoutMillis = from.pendingConnectionTimeoutMillis
    )
}
