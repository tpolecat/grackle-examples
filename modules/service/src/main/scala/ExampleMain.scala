// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.Port
import edu.gemini.grackle.Mapping
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

import java.time.ZoneId
import scala.jdk.CollectionConverters.*

trait ExampleMain extends IOApp:

  def serverResource(
    port: Port,
    app:  WebSocketBuilder2[IO] => HttpApp[IO]
  ): Resource[IO, Server] =
    BlazeServerBuilder
      .apply[IO]
      .bindHttp(port.value, "0.0.0.0")
      .withHttpWebSocketApp(app)
      .resource

  def mapping: Resource[IO, Mapping[IO]]

  def graphQLRoutes(m: Mapping[IO])(using Logger[IO]): Resource[IO, WebSocketBuilder2[IO] => HttpRoutes[IO]] =
    Resource.pure { wsb =>    
      Routes.forService(
        _ => GrackleGraphQLService(m).some.pure[IO],
        wsb
      )
    }

  def server(using Logger[IO]): Resource[IO, ExitCode] =
    for
      c  <- Resource.eval(Config.load[IO])
      m  <- mapping
      ap <- graphQLRoutes(m).map(_.map(_.orNotFound))
      _  <- serverResource(c.port, ap)
    yield ExitCode.Success

  def run(args: List[String]): IO[ExitCode] =
    given Logger[IO] = Slf4jLogger.getLoggerFromName[IO]("example")
    server.use(_ => IO.never[ExitCode])



