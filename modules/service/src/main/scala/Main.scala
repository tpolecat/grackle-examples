// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.Port
import lucuma.graphql.routes.GrackleGraphQLService
import lucuma.graphql.routes.Routes
import natchez.Trace
import natchez.Trace.Implicits.noop
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp:

  def serverResource[F[_]: Async](
    port: Port,
    app:  WebSocketBuilder2[F] => HttpApp[F]
  ): Resource[F, Server] =
    BlazeServerBuilder
      .apply[F]
      .bindHttp(port.value, "0.0.0.0")
      .withHttpWebSocketApp(app)
      .resource

  def graphQLRoutes[F[_]: Async: Logger: Trace]: Resource[F, WebSocketBuilder2[F] => HttpRoutes[F]] =
    Resource.pure { wsb =>    
      Routes.forService(
        _ => GrackleGraphQLService(TimeMapping[F]).some.pure[F],
        wsb
      )
    }

  def server[F[_]: Async: Logger: Trace]: Resource[F, ExitCode] =
    for
      c  <- Resource.eval(Config.load[F])
      ap <- graphQLRoutes.map(_.map(_.orNotFound))
      _  <- serverResource(c.port, ap)
    yield ExitCode.Success

  def runF[F[_]: Async: Logger: Trace]: F[ExitCode] =
    server.use(_ => Concurrent[F].never[ExitCode])

  def run(args: List[String]): IO[ExitCode] =
    given Logger[IO] = Slf4jLogger.getLoggerFromName[IO]("example")
    runF[IO]



