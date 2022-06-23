package ru.itis.katya.memsholder.domain.mem

import cats.data.OptionT

trait MemRepositoryAlgebra[F[_]] {
  def create(mem: Mem): F[Mem]

  def update(mem: Mem): OptionT[F, Mem]

  def get(memId: Long): OptionT[F, Mem]

  def delete(memId: Long): OptionT[F, Mem]

//  def findByTag(tagId: Long, pageSize: Int, offset: Int): F[List[Mem]]

  def findByUser(userId: Long, pageSize: Int, offset: Int): F[List[Mem]]

  def list(pageSize: Int, offset: Int): F[List[Mem]]
}
