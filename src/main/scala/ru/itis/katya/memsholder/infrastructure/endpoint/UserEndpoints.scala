package ru.itis.katya.memsholder.infrastructure.endpoint



import ru.itis.katya.memsholder.domain._
import ru.itis.katya.memsholder.domain.user._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.jwt.algorithms.JWTMacAlgo




class UserEndpoints[F[_]: Sync,  Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import Pagination._

  /* Jsonization of our User type */

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

//  private def loginEndpoint(
//      userService: UserService[F],
//      auth: Authenticator[F, Long, User, AugmentedJWT[Auth, Long]],
//  ): HttpRoutes[F] =
//    HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
//      val action = for {
//        login <- req.as[LoginRequest]
//        name = login.userName
//        user <- userService.getUserByName(name).value
//        result <- user.traverse(x => if(x.hash == login.password) x.pure[F] else throw new Exception())
//      } yield (result)
//
//      action.flatMap {
//        case Right(user) => user.id match {
//          case Some(value) => Ok(auth.create(value))
//          case None => BadRequest(s"Authentication failed for user ${user.userName}")
//        }
//        case Left(_) =>
//          BadRequest(s"Authentication failed for user ")
//      }
//    }

  private def signupEndpoint(
      userService: UserService[F],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root =>
      val action = for {
        signup <- req.as[SignupRequest]
        user <- signup.asUser().pure[F]
        result <- userService.createUser(user).value
      } yield result


      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserAlreadyExistsError(existing)) =>
          Conflict(s"The user with user name ${existing.userName} already exists")
      }
    }



  private def listEndpoint(userService: UserService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
          offset,
        )  =>
      for {
        retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }



  def endpoints(
      userService: UserService[F],
  ): HttpRoutes[F] = {
    val unauthEndpoints =
        signupEndpoint(userService) <+> listEndpoint(userService)

    unauthEndpoints
  }
}

object UserEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      userService: UserService[F],
  ): HttpRoutes[F] =
    new UserEndpoints[F, Auth].endpoints(userService)
}
