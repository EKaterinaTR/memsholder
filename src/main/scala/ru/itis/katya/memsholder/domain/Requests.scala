package ru.itis.katya.memsholder.domain





import ru.itis.katya.memsholder.domain.mem.Tag
import user.{Role, User}

final case class LoginRequest(
                               userName: String,
                               password: String,
                             )

final case class MemCreateRequest(
                                   text: String,
                                   user: Long,
                                   tags: List[Tag]
                                 )


final case class SignupRequest(
                                userName: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                password: String,
                                phone: String,
                                role: Role,
                              ) {
  def asUser[A](): User = User(
    userName,
    firstName,
    lastName,
    email,
    password,
    phone,
    role = role,
  )
}