package ru.itis.katya.memsholder.config

final case class ServerConfig(host: String, port: Int)
final case class MemStoreConfig(db: DatabaseConfig, server: ServerConfig)
