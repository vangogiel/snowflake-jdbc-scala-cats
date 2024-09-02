package io.vangogiel.snowflake.jdbc.cats

import cats.effect.{ Async, Blocker, ContextShift, Resource }
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps }

import java.sql.{ Connection, PreparedStatement, ResultSet, SQLException }
import scala.concurrent.duration.FiniteDuration

object SnowflakeImplicits {
  private val maxRetries: Int = 3
  type QueryTimeout = FiniteDuration

  implicit class QueryOps(val query: String) extends AnyVal {
    def prepare[F[_]: Async, A](function: PreparedStatement => F[A])(implicit
        connectionManager: Resource[F, SnowflakeConnection[F]],
        queryTimeout: QueryTimeout
    ): F[A] = makePrepare(0, function)

    private def makePrepare[F[_]: Async, A](retryCount: Int, function: PreparedStatement => F[A])(
        implicit
        connectionManager: Resource[F, SnowflakeConnection[F]],
        queryTimeout: QueryTimeout
    ): F[A] = {
      withConnection.use { connection =>
        Resource
          .make {
            Async[F].delay {
              val statement = connection.prepareStatement(query)
              statement.setQueryTimeout(queryTimeout.toSeconds.toInt)
              statement.setFetchSize(5000)
              statement.executeQuery("ALTER SESSION SET JDBC_QUERY_RESULT_FORMAT='JSON'")
              statement
            }
          }(statement => Async[F].delay(statement.close()))
          .use(function)
          .handleErrorWith(e => handleTokenExpiredError(e, retryCount, function))
      }
    }

    private def withConnection[F[_]: Async](implicit
        connectionManager: Resource[F, SnowflakeConnection[F]]
    ): Resource[F, Connection] = {
      connectionManager.flatMap { cm =>
        Resource.fromAutoCloseable[F, Connection](cm.getConnection)
      }
    }

    private def handleTokenExpiredError[F[_]: Async, A](
        e: Throwable,
        retryCount: Int,
        function: PreparedStatement => F[A]
    )(implicit
        connectionManagerResource: Resource[F, SnowflakeConnection[F]],
        queryTimeout: QueryTimeout
    ): F[A] = {
      e match {
        case e: SQLException if isTokenExpiredException(e) && retryCount <= maxRetries =>
          for {
            _ <- connectionManagerResource.use(_.reloadConnection)
            result <- makePrepare(retryCount + 1, function)
          } yield result
        case e => Async[F].raiseError(e)
      }
    }

    private def isTokenExpiredException(e: SQLException) =
      e.getMessage.toLowerCase.contains("authentication token has expired")
  }

  implicit class PreparedStatementOps(private val statement: PreparedStatement) extends AnyVal {
    def performQuery[F[_]: Async, A](processResult: ResultSet => A, blocker: Blocker)(implicit
        cs: ContextShift[F]
    ): F[A] =
      makeExecuteQuery(blocker, statement, _.executeQuery(), processResult)
  }

  private def makeExecuteQuery[F[_]: Async, A](
      blocker: Blocker,
      statement: PreparedStatement,
      execute: PreparedStatement => ResultSet,
      processResult: ResultSet => A
  )(implicit cs: ContextShift[F]): F[A] = {
    Resource
      .make(Async[F].delay(execute(statement)))(_ => Async[F].delay(statement.close()))
      .use(rs => blocker.blockOn(Async[F].delay(processResult(rs))))
  }
}
