import com.twitter.finagle.http.{Request, Response, Status}
import org.apache.commons.codec.binary.Base64
import slick.jdbc.H2Profile.api._
import wvlet.airframe.http.{Endpoint, HttpMethod}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps


trait RoutingService{

  private val db = Database.forConfig("h2mem1")
  private val books = TableQuery[Books]
  private val users = TableQuery[Users]

  @Endpoint(method = HttpMethod.GET, path = "/me/books/list")
  def getBooksByAuthor(request: Request): Future[Seq[Book]] = {
    require(Option[String](request.getParam("author")).isDefined, "Author name is required")
    val author = Option[String](request.getParam("author")) match {
      case Some(x) => x.toLowerCase
      case None => throw new IllegalArgumentException("Author name is required")
    }

    val year  = Option(request.getParams("year"))
      .map(_.asScala.toSeq)
      .map(_.map(_.toInt)).getOrElse(Seq())

    try{
      val query = for{
        book <- books if book.author.toLowerCase === author
      } yield book
      db.run(query.result.map(_.filter(book => year.isEmpty || year.contains(book.year))))
    }
  }

  @Endpoint(method = HttpMethod.POST, path = "/me/books/upload")
  def uploadBook(book: Book): Future[Book] = {
    require(Option(book.title).exists(!_.isBlank), "Book title must not be empty")
    require(Option(book.author).exists(!_.isBlank), "Book author must not be empty")
    require(Option(book.year).exists(_ > 0), "Book year is required")

    Await.result(db.run(DBIO.seq(
      books += book
    )), 2 seconds)
    Future(book)
  }

  @Endpoint(method = HttpMethod.POST, path = "/login")
  def login(request: Request): Future[Response] = {
    request.authorization match {
      case Some(x) =>
        val authDecoded = new String(Base64.decodeBase64(x.substring(5).getBytes)).split(":")
        val username = authDecoded(0)
        val password = authDecoded(1)
        val query = for(u <- users if u.username.toLowerCase === username.toLowerCase && u.password === password ) yield u
        db.run(query.result).map(s => if (s.isEmpty) throw UnAuthorizedException("Login failed") else s).map(a => Response(Status.Ok))
      case None => throw UnAuthorizedException("Login Failed")
    }
  }


  @Endpoint(method = HttpMethod.POST, path = "/register")
  def register(newUser: User): Response = {
    require(Option(newUser.first).exists(!_.isBlank), "Firstname is required")
    require(Option(newUser.last).exists(!_.isBlank), "Lastname is required")
    require(Option(newUser.username).exists(!_.isBlank), "Username is required")
    require(Option(newUser.password).exists(!_.isBlank), "Password is required")

    val query = for(u <- users if u.username === newUser.username) yield u
    db.run(query.result).map(rs => if (rs.nonEmpty) throw new IllegalArgumentException("username is already taken"))

    db.run(DBIO.seq(
      users += newUser
    ))

    Response(Status.Created)
  }

  @Endpoint(method = HttpMethod.GET, path = "/")
  def test(): Future[Response] ={
    Future(Response(Status.Ok))
  }
}
