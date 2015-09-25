package eu.nojokescala.futureeither

import java.time.{ZonedDateTime => DateTime}

import scala.concurrent.Future

import eu.nojokescala.futureeither.Service.StorageError

trait Service {
  def store(record: Record): Future[Either[StorageError, DateTime]]
}

object Service {
  sealed trait StorageError

  final case class DuplicateId(record: Record) extends StorageError
  final case class DuplicateTitle(record: Record) extends StorageError
}
