package io.vangogiel.snowflake.jdbc.cats

import java.sql.{ Connection, DriverManager }
import java.util.Properties

object SnowflakePhysicalConnection {
  def createConnection(snowflakeJdbcConfig: SnowflakeConfig): Connection = {
    val prop = new Properties()
    prop.put("user", snowflakeJdbcConfig.username)
    prop.put("privateKey", snowflakeJdbcConfig.privateKey.value)
    prop.put("db", snowflakeJdbcConfig.db)
    prop.put("schema", snowflakeJdbcConfig.schema)
    prop.put("warehouse", snowflakeJdbcConfig.warehouse)
    prop.put("role", snowflakeJdbcConfig.role)
    DriverManager.getConnection(snowflakeJdbcConfig.url, prop)
  }
}
