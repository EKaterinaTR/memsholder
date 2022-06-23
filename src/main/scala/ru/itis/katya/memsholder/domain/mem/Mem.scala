package ru.itis.katya.memsholder.domain.mem


case class Mem (
                 text: String,
                 id: Option[Long] = None,
                 creator: Long
               );

