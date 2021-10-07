import slick.jdbc.H2Profile.api._


case class Books(tag: Tag) extends Table[Book](tag, "BOOK"){
  def id = column[Int]("BOOK_ID", O.PrimaryKey, O.AutoInc)
  def title = column[String]("TITLE")
  def author = column[String]("AUTHOR")
  def year = column[Int]("YEAR")
  def publisher = column[String]("PUBLISHER")

  override def * = (title, author, year, publisher, id.?) <> (Book.tupled, Book.unapply)

}
