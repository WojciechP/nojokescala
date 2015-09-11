package eu.nojokescala.futureeither

import scala.concurrent.ExecutionContext.Implicits.global

import eu.nojokescala.futureeither.Service.{DuplicateId, DuplicateTitle}

object SimpleClient extends App {
  val service: Service = ServiceImpl
  val record = Record("id-123", "A title", "This is extremely valuable piece of data")
  val result = service.store(record) map (_ fold (
    {
      case DuplicateId(_) => "Ehh, duplicate record id"
      case DuplicateTitle(_) => "This title is already taken"
    },
    time => "Yey, stored on " + time))

  result foreach println
}
