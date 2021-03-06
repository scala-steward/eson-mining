/*
 * Copyright 2017-2019 Michael Mior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.uwaterloo.dsg.eson

import org.scalatest.Matchers._

import collection.mutable.Stack
import org.apache.cassandra.config.DatabaseDescriptor
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest._

class DiscoveryRunnerSpec extends FlatSpec with Matchers {
  "DiscoveryRunner" should "find simple FDs and INDs" in {
    // This hack is necessary otherwise tests fail with CassandraUnit in IntelliJ (jdk10)
    // should be called right after constructor
    // NullPointerException for DatabaseDescriptor.getDiskFailurePolicy
    // for more info see
    // https://github.com/jsevellec/cassandra-unit/issues/249
    // https://github.com/jsevellec/cassandra-unit/issues/221
    try {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE)
    } catch {
      case e: NullPointerException =>
        DatabaseDescriptor.daemonInitialization()
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE)
    }

    val cluster = EmbeddedCassandraServerHelper.getCluster()
    val session = EmbeddedCassandraServerHelper.getSession()
    val dataLoader = new CQLDataLoader(session)
    val dataSet = new ClassPathCQLDataSet("twissandra.cql")
    dataLoader.load(dataSet)

    val receiver = new PrintingDependencyReceiver
    DiscoveryRunner.run("127.0.0.1", 9142, "twissandra", receiver)

    receiver.fds.map(_.toString) should contain ("[tweets.tweet_id]->tweets.body")
    receiver.inds.map(_.toString) should contain ("[\"users\".username][=[\"tweets\".username]")
  }
}
