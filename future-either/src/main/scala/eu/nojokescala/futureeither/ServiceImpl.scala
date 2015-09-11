package eu.nojokescala.futureeither

import java.time.ZonedDateTime

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Future._

import eu.nojokescala.futureeither.Service.{DuplicateId, DuplicateTitle, StorageError}

/** This is a sample implementation for the purpose of experimenting
  * with API signatures - it's neither well designed nor implemented */
object ServiceImpl extends Service {
  private[this] val collection = mutable.Set.empty[Record]

  private[this] def storeAsFuture(record: Record): Future[Either[Nothing, ZonedDateTime]] = {
    // "Random" fail:
    val time = ZonedDateTime.now()
    if (time.getSecond % 3 == 0) {
      failed(new Exception("DB storage error"))
    } else {
      collection.add(record)
      successful(Right(time))
    }
  }
    successful(Right(ZonedDateTime.now()))

  override def store(record: Record): Future[Either[StorageError, ZonedDateTime]] = {
    collection.find(_.id == record.id)
      .map(_ => successful(Left(DuplicateId(record))))
      .getOrElse(collection.find(_.title == record.title)
        .map(_ => successful(Left(DuplicateTitle(record))))
        .getOrElse(storeAsFuture(record)))
  }

}
