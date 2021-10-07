import BooksApp.port
import com.twitter.finagle.http.Status
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Await
import slick.dbio.DBIO
import slick.lifted.TableQuery
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.Finagle
import wvlet.airspec.AirSpec
import slick.jdbc.H2Profile.api._


class BookAppSpec extends AirSpec{
  override def beforeAll: Unit = {
    val errorFilter = new ExceptionHandler

    val securityFilter = new SecurityFilter
    val router = Router.add[RoutingService]

    val service = Finagle.server
      .withName("book-api-server")
      .withRouter(router)
      .withBeforeRoutingFilter(securityFilter)
      .withErrorFilter(errorFilter)
      .withPort(8085)

    val db = Database.forConfig("h2mem1")

    try {
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

        users += User("Chukwuebuka", "Anazodo", "chuk", "123456"),
        users += User("Mahya", "Mirtar", "mahya", "abcdef")
      )
      val setupFuture = db.run(setup)

      service.start { server =>
        // The customized server will start here
        server.waitServerTermination
      }
    }
  }

  test("Service reachable"){
    val client: Service[http.Request, http.Response] = Http.newService("localhost:8085")

    val req = http.Request("/")

    val res = Await.result(client(req))
    info("response: " + res)
    assert(res.status == Status.Ok)
  }
}
