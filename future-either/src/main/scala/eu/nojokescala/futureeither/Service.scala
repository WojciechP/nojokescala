package eu.nojokescala.futureeither

import java.time.{ZonedDateTime => DateTime}

import scala.concurrent.Future

import eu.nojokescala.futureeither.Service.StorageError

trait Service {
  def store(record: Record): Future[Either[StorageError, DateTime]]
}

object Service {
  sealed abstract class StorageError(message: String) extends Exception(message)

  final case class DuplicateId(record: Record) extends StorageError("Duplicate ID in record " + record)
  final case class DuplicateTitle(record: Record) extends StorageError("Duplicate title in record " + record)
}
