package ru.itis.katya.memsholder.domain.mem

import cats.data.OptionT

trait TagRepositoryAlgebra[F[_]] {
  def create(tag: Tag): F[Tag]

  def get(tagId: Long): OptionT[F, Tag]

  def delete(tagId: Long): OptionT[F, Tag]

  def addTagToMem(tagID:Long, memId:Long):F[Unit]

  def list(pageSize: Int, offset: Int): F[List[Tag]]
}
