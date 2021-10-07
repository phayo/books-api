import slick.jdbc.H2Profile.api._


class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def first = column[String]("FIRST")
  def last = column[String]("LAST")
  def username = column[String]("USERNAME", O.Unique)
  def password = column[String]("PASSWORD")
  def * = (first, last, username, password, id.?) <> (User.tupled, User.unapply)
}
