package ru.itis.katya.memsholder

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.parser
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}
import ru.itis.katya.memsholder.config._
import ru.itis.katya.memsholder.domain.mem.MemService
import ru.itis.katya.memsholder.domain.user._
import ru.itis.katya.memsholder.infrastructure.endpoint._
import ru.itis.katya.memsholder.infrastructure.repository.doobie.{ DoobieMemRepositoryInterpreter, DoobieTagRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import tsec.mac.jca.HMACSHA256


object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, MemStoreConfig]("memsstore"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      memRepo = DoobieMemRepositoryInterpreter[F](xa)
      tagRepo = DoobieTagRepositoryInterpreter[F](xa)
      userValidation = UserValidationInterpreter[F](userRepo)
      userService = UserService[F](userRepo, userValidation)
      memService = MemService(memRepo,tagRepo)
      httpApp = Router(
        "/user" -> UserEndpoints
          .endpoints[F, HMACSHA256](userService),
        "/mem" -> MemEndpoints.endpoints[F](memService),
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer[IO].use(_ => IO.never).as(ExitCode.Success)
}
