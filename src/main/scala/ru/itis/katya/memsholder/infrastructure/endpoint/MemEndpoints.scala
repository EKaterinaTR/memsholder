package ru.itis.katya.memsholder.infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import ru.itis.katya.memsholder.domain._
import ru.itis.katya.memsholder.domain.mem.{Mem, MemService}



class MemEndpoints[F[_]: Sync] extends Http4sDsl[F] {
  //import Pagination._


  implicit val memDecoder: EntityDecoder[F, Mem] = jsonOf
  implicit val create: EntityDecoder[F, MemCreateRequest] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf


  private def createEndpoint(
                              memService: MemService[F],
                            ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req@POST -> Root =>
      val action = for {
        memspluse <- req.as[MemCreateRequest]
        result <- memService.
          createMem(
            Mem(memspluse.text, None, memspluse.user),
            memspluse.tags)
      } yield result

      action.flatMap(x => Ok(x.asJson))
    }



  //  private def listEndpoint(userService: UserService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
  //    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
  //          offset,
  //        )  =>
  //      for {
  //        retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
  //        resp <- Ok(retrieved.asJson)
  //      } yield resp
  //  }


  def endpoints(
                 memService: MemService[F],
               ): HttpRoutes[F] = {
    val unauthEndpoints = createEndpoint(memService)
    unauthEndpoints
  }
}
  object MemEndpoints {
    def endpoints[F[_]: Sync](
      memService: MemService[F],
    ): HttpRoutes[F] =
      new MemEndpoints[F].endpoints(memService)
  }



