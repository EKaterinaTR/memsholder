package ru.itis.katya.memsholder.domain.user

import cats._
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(name: String)

object Role extends SimpleAuthEnum[Role, String] {
  val Admin: Role = Role("Admin")
  val SimpleUser: Role = Role("SimpleUser")

  override val values: AuthGroup[Role] = AuthGroup(Admin, SimpleUser)

  override def getRepr(t: Role): String = t.name

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
