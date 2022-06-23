package ru.itis.katya.memsholder.domain.mem

import cats.{Functor, Monad}
import cats.data._
import cats.implicits.toFunctorOps
import ru.itis.katya.memsholder.domain.MemNotFoundError

case class MemService[F[_]](memRepo: MemRepositoryAlgebra[F], tagRepo: TagRepositoryAlgebra[F]) {
  def createMem(mem: Mem, tags: List[Tag])(implicit M: Monad[F]): F[Mem] = {
    tags.collect {
        case tag@Tag(_, None)   => tagRepo.create(tag)
      }
    val memWithID = memRepo.create(mem)

    memWithID.map(m => m.id match {
      case Some(mId) => tags.collect {
        case Tag(_, Some(value))  =>  tagRepo.addTagToMem(value, mId)
      }
      case None => () // Mem must have id after create
    }
    )
    memWithID
  }

  def getMem(memId: Long)(implicit F: Functor[F]): EitherT[F, MemNotFoundError.type, Mem] =
    memRepo.get(memId).toRight(MemNotFoundError)

  def deleteMem(memId: Long)(implicit F: Functor[F]): F[Unit] =
    memRepo.delete(memId).value.void

  def update(mem: Mem)(implicit M: Monad[F]): EitherT[F, MemNotFoundError.type, Mem] =
    for {
      saved <- memRepo.update(mem).toRight(MemNotFoundError)
    } yield saved

  def list(pageSize: Int, offset: Int): F[List[Mem]] =
    memRepo.list(pageSize, offset)

//  def findMemByTag(tagId: Long, pageSize: Int, offset: Int): F[List[Mem]] =
//    memRepo.findByTag(tagId, pageSize, offset)

  def findMemByUser(userId: Long, pageSize: Int, offset: Int): F[List[Mem]] =
    memRepo.findByUser(userId, pageSize, offset)
}




