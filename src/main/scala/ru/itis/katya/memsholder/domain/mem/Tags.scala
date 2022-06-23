package ru.itis.katya.memsholder.domain.mem

case class Tag (
                  text: String,
                  id: Option[Long] = None,
                )
