package io.vangogiel.snowflake.jdbc.cats

import cats.effect.concurrent.Ref
import cats.effect.{Async, Resource}
import cats.implicits.{toFlatMapOps, toFunctorOps}

import java.sql.Connection

trait SnowflakeConnection[F[_]] {
  def getConnection: F[Connection]
  def reloadConnection: F[Unit]
}

class SnowflakeLogicalConnection[F[_]: Async](
    connRef: Ref[F, Connection],
    snowflakeJdbcConfig: SnowflakeConfig
) extends SnowflakeConnection[F] {
  def getConnection: F[Connection] = connRef.get

  def reloadConnection: F[Unit] = {
    for {
      _ <- connRef.get.flatMap(c => Async[F].delay(c.close()))
      _ <- createConnection.map(newConnection => Async[F].delay(connRef.set(newConnection)))
    } yield ()
  }

  private def createConnection: F[Connection] =
    Async[F].delay(SnowflakePhysicalConnection.createConnection(snowflakeJdbcConfig))
}

object SnowflakeLogicalConnection {
  def make[F[_]: Async](
      snowflakeJdbcConfig: SnowflakeConfig
  ): Resource[F, SnowflakeLogicalConnection[F]] = {
    Resource
      .make {
        for {
          connection <- Async[F].delay(
            SnowflakePhysicalConnection.createConnection(snowflakeJdbcConfig)
          )
          connRef <- Ref.of[F, Connection](connection)
        } yield new SnowflakeLogicalConnection[F](connRef, snowflakeJdbcConfig)
      } { manager =>
        manager.getConnection.flatMap(c => Async[F].delay(c.close()))
      }
  }
}
