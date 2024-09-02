package io.vangogiel.snowflake.jdbc.cats

import pureconfig.ConfigReader

final case class Secret(value: String) extends AnyVal {
  override def toString: String = "[SECRET]"
}

object Secret {
  implicit val secretConfigReader: ConfigReader[Secret] =
    ConfigReader[String].map(n => new Secret(n))
}

case class SnowflakeConfig(
    url: String,
    username: String,
    privateKey: Secret,
    db: String,
    schema: String,
    warehouse: String,
    role: String
)
