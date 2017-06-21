/*
 * This file is part of the eson-mining distribution (https://github.com/michaelmior/eson-mining).
 * Copyright (c) 2017 Michael Mior.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.uwaterloo.dsg.eson

import de.metanome.backend.input.database.{DefaultDatabaseConnectionGenerator, DefaultTableInputGenerator}
import de.metanome.algorithm_integration.configuration.{ConfigurationSettingDatabaseConnection, ConfigurationSettingTableInput, DbSystem}
import de.metanome.algorithms.tane.TaneAlgorithm
import java.sql.{Connection, DriverManager}
import java.util.Properties

object TaneRunner {
  def main(args: Array[String]): Unit = {
    val connString = "jdbc:calcite:model=src/main/resources/model.json"
    val connectionProps = new Properties()
    connectionProps.put("user", "admin")
    connectionProps.put("password", "admin")
    val conn = DriverManager.getConnection(connString, connectionProps)
    val tables = conn.getMetaData().getTables(null, null, "%", null)
    tables.next; tables.next // skip the header
    val tableNames = new scala.collection.mutable.MutableList[String]
    while (tables.next) {
      tableNames += tables.getString(3)
    }

    val db = new ConfigurationSettingDatabaseConnection(connString, "admin", "admin", DbSystem.Oracle)

    tableNames.foreach(tableName => {
      val config = new ConfigurationSettingTableInput("\"" + tableName + "\"", db)
      val table = new DefaultTableInputGenerator(config)
      val tane = new TaneAlgorithm()
      tane.setRelationalInputConfigurationValue(TaneAlgorithm.INPUT_TAG, table)
      tane.setResultReceiver(new PrintingFunctionalDependencyReceiver)
      tane.execute
    })
  }
}
