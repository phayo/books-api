import com.github.t3hnar.bcrypt._
import com.twitter.app.{App, Flag, Flaggable}
import slick.jdbc.H2Profile.api._
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.Finagle


object BooksApp extends App{

  // parses an integer from the "-port" flag.
  // Finagle already provides an implicit Flaggable typeclass for Int
  // usage: -port 9000
  val port: Flag[Int] = flag[Int]("port", 8080, "port this server should use")

  val env: Flag[Env] = flag[Env]("env", Dev, "environment this server runs")

  sealed trait Env
  case object Prod extends Env
  case object Dev extends Env
  // parses an Env trait. See typeclass below

  implicit val flaggableEnv: Flaggable[Env] = {
    case "prod" => Prod
    case "dev" => Dev
  }

  def main(): Unit = {
    val errorFilter = new ExceptionHandler

    val securityFilter = new SecurityFilter
    val router = Router.add[RoutingService]

    val service = Finagle.server
      .withName("book-api-server")
      .withRouter(router)
      .withBeforeRoutingFilter(securityFilter)
      .withErrorFilter(errorFilter)
      .withPort(port.apply())

    val db = Database.forConfig("h2mem1")

    val books = TableQuery[Books]
    val users = TableQuery[Users]
    val setup = DBIO.seq(
      // Create the tables, including primary and foreign keys
      (books.schema ++ users.schema).create,

      // Insert some books
      books += Book("The Lord of the Rings ", "JRR Tolkien", 1954, "Allen & Unwin"),
      books += Book("Rich Dad Poor Dad", "Robert T. Kiyosaki", 1997, "Warner Books"),
      books += Book("The Notebook", "Nicholas Sparks", 1996, "Warner Books"),
      books += Book("My Life in Red and White", "Arsene Wenger", 2020, "Chronicle Prism"),
      books += Book("A Promised Land", "Barrack Obama", 2020, "Crown"),
      books += Book("Things Fall Apart", "Chinua Achebe", 1958, "William Heinemann Ltd"),
      books += Book("Purple Hibiscus", "Chimamanda Ngozi Adichie", 2003, "Algonquin Books Kachifo Limited"),

      users += User("Chukwuebuka", "Anazodo", "chuk", "123456".bcrypt),
      users += User("Mahya", "Mirtar", "mahya", "abcdef".bcrypt)
    )
    val setupFuture = db.run(setup)

    service.start { server =>
      // The customized server will start here
      server.waitServerTermination
    }

    //finally db.close


  }
}
