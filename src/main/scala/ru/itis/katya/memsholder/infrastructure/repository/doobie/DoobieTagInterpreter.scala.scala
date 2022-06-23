package ru.itis.katya.memsholder.infrastructure.repository.doobie

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import ru.itis.katya.memsholder.domain.mem.{Tag, TagRepositoryAlgebra}

import ru.itis.katya.memsholder.infrastructure.repository.doobie.SQLPagination.paginate
import tsec.authentication.IdentityStore

private object TagSQL {

  def insert(tag: Tag): Update0 = sql"""
    INSERT INTO TAGS (TEXT)
    VALUES (${tag.text})
  """.update

  def tagToMem(memId:Long, tagId: Long): Update0 = sql"""
    INSERT INTO MEMS_TAGS (MEM_ID,TAG_ID)
    VALUES ($memId,$tagId)
  """.update

  def select(tagId: Long): Query0[Tag] = sql"""
    SELECT TEXT,ID
    FROM TAGS
    WHERE ID = $tagId
  """.query



  def delete(tagId: Long): Update0 = sql"""
    DELETE FROM TAGS WHERE ID = $tagId
  """.update

  val selectAll: Query0[Tag] = sql"""
    SELECT TEXT,ID
    FROM TAGS
  """.query
}

class DoobieTagRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends TagRepositoryAlgebra[F]
    with IdentityStore[F, Long, Tag] { self =>
  import TagSQL._

  override def create(tag: Tag): F[Tag] =
    insert(tag).withUniqueGeneratedKeys[Long]("id").map(x => tag.copy(id = x.some)).transact(xa)

  override def delete(tagId: Long): OptionT[F, Tag] =
    get(tagId).semiflatMap(tag => TagSQL.delete(tagId).run.transact(xa).as(tag))

  override def addTagToMem(tagId: Long, memId: Long): F[Unit] = tagToMem(memId, tagId).run.transact(xa).void

  override def list(pageSize: Int, offset: Int): F[List[Tag]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  override def get(id: Long): OptionT[F, Tag] = OptionT(select(id).option.transact(xa))
}

object DoobieTagRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieTagRepositoryInterpreter[F] =
    new DoobieTagRepositoryInterpreter(xa)
}
