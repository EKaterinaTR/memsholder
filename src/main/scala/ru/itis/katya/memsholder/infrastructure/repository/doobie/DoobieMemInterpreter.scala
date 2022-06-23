package ru.itis.katya.memsholder.infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import ru.itis.katya.memsholder.domain.mem.{Mem, MemRepositoryAlgebra}
import ru.itis.katya.memsholder.domain.user.{Role}
import ru.itis.katya.memsholder.infrastructure.repository.doobie.SQLPagination.paginate
import tsec.authentication.IdentityStore

private object MemSQL {
  implicit val roleMeta: Meta[Role] =
    Meta[String].imap(decode[Role](_).leftMap(throw _).merge)(_.asJson.toString)

  def insert(mem: Mem): Update0 = sql"""
    INSERT INTO MEMS (TEXT, CREATOR)
    VALUES (${mem.text}, ${mem.creator})
  """.update

  def update(mem: Mem, id: Long): Update0 = sql"""
    UPDATE MEMS
    SET TEXT = ${mem.text}
    WHERE ID = $id
  """.update

  def select(memId: Long): Query0[Mem] = sql"""
    SELECT (TEXT, CREATOR, ID)
    FROM MEMS
    WHERE ID = $memId
  """.query



  def delete(memId: Long): Update0 = sql"""
    DELETE FROM MEMS WHERE ID = $memId
  """.update

  val selectAll: Query0[Mem] = sql"""
    SELECT (TEXT, CREATOR, ID)
    FROM MEMS
  """.query

  def selectAllByUser(userId:Long): Query0[Mem] = sql"""
    SELECT (TEXT, CREATOR, ID)
    FROM MEMS
    WHERE CREATOR = $userId
  """.query
}

class DoobieMemRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends MemRepositoryAlgebra[F]
    with IdentityStore[F, Long, Mem] { self =>
  import MemSQL._

  override def create(mem: Mem): F[Mem] =
    insert(mem).withUniqueGeneratedKeys[Long]("id").map(x => mem.copy(id = x.some)).transact(xa)


  override def update(mem: Mem): OptionT[F, Mem] =  OptionT.fromOption[F](mem.id).semiflatMap { id =>
    MemSQL.update(mem, id).run.transact(xa).as(mem)
  }

  override def delete(memId: Long): OptionT[F, Mem] =
    get(memId).semiflatMap(mem => MemSQL.delete(memId).run.transact(xa).as(mem))

//  override def findByTag(tagId: Long, pageSize: Int, offset: Int): F[List[Mem]] = ???

  override def findByUser(userId: Long, pageSize: Int, offset: Int): F[List[Mem]] =
    paginate(pageSize, offset)(selectAllByUser(userId)).to[List].transact(xa)

  override def list(pageSize: Int, offset: Int): F[List[Mem]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  override def get(id: Long): OptionT[F, Mem] = OptionT(select(id).option.transact(xa))
}

object DoobieMemRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieMemRepositoryInterpreter[F] =
    new DoobieMemRepositoryInterpreter(xa)
}
