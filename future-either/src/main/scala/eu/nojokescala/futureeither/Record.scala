package eu.nojokescala.futureeither

final case class Record(id: UniqueID, title: String, data: WeirdDataType) {
  require(title.length >= 2 && title.length <= 20, "title has to be 2-20 characters long")
  require(id != null)
  require(data != null)
}
