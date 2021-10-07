import com.github.t3hnar.bcrypt._
import com.twitter.finagle.http
import com.twitter.finagle.http.Status
import org.apache.commons.codec.binary.Base64
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class RoutingServiceTest extends AnyWordSpec with Matchers with RoutingService with BeforeAndAfterAll {
  val db = Database.forConfig("h2mem1")
  val books = TableQuery[Books]
  val users = TableQuery[Users]
  override def beforeAll(): Unit = {
    configureDB()
  }
  private def configureDB() = {
    val setup = DBIO.seq(
      // Create the tables, including primary and foreign keys
      (books.schema ++ users.schema).create,

      // Insert some books
      books += Book("The Lord of the Rings", "JRR Tolkien", 1954, "Allen & Unwin"),
      books += Book("The Lord of the Rings 2", "JRR Tolkien", 1956, "Allen & Unwin"),
      books += Book("Rich Dad Poor Dad", "Robert T. Kiyosaki", 1997, "Warner Books"),


      users += User("Chukwuebuka", "Anazodo", "chuk", "123456".bcrypt),
      users += User("Mahya", "Mirtar", "mahya", "abcdef".bcrypt)
    )
    db.run(setup)
  }

  "Register endpoint" should {
    "register a new user" when{
      "correct user passed" in {
        val newUser = User("First", "Last", "username", "password")
        val res = register(newUser)
        res.status shouldEqual Status.Created
      }
    }

    "reject new user registration" when {
      "username is absent" in {
        val newUser = User("First", "Last", "", "password")
        val newUser2 = User("First", "Last", null, "password")

        an[IllegalArgumentException] should be thrownBy register(newUser)
        an[IllegalArgumentException] should be thrownBy register(newUser2)
      }

      "firstname absent" in {
        val newUser = User("", "Last", "username", "password")
        val newUser2 = User(null, "Last", "username", "password")

        an[IllegalArgumentException] should be thrownBy register(newUser)
        an[IllegalArgumentException] should be thrownBy register(newUser2)
      }

      "lastname absent" in {
        val newUser = User("First", "", "username", "password")
        val newUser2 = User("First", null, "username", "password")

        an[IllegalArgumentException] should be thrownBy register(newUser)
        an[IllegalArgumentException] should be thrownBy register(newUser2)
      }

      "password absent" in {
        val newUser = User("First", "Last", "username", "")
        val newUser2 = User("First", "Last", "username", null)

        an[IllegalArgumentException] should be thrownBy register(newUser)
        an[IllegalArgumentException] should be thrownBy register(newUser2)
      }
    }

  }

  "Login endpoint" should{
    "Log a user in correctly" in {
      val request = http.Request()
      request.authorization = "Basic " + new String(Base64.encodeBase64("chuk:123456".getBytes))
      val resFuture  = login(request)
      val res = Await.result(resFuture, 3 seconds)
      res.status shouldEqual Status.Ok
    }

    "deny an user login" when {
      "user is unregistered" in {
        val request = http.Request()
        request.authorization = "Basic " + new String(Base64.encodeBase64("unknownUser:123456".getBytes))
        an[UnAuthorizedException] should be thrownBy Await.result(login(request), 3 seconds)
      }
      "user supplied wrong password" in {
        val request = http.Request()
        request.authorization = "Basic " + new String(Base64.encodeBase64("chuk:1234567".getBytes))
        an[UnAuthorizedException] should be thrownBy Await.result(login(request), 3 seconds)
      }
    }
  }

  "GetBooks endpoint" should {
    "return correct books when" when {
      "only author is provided" in {
        val req = http.Request(http.Method.Get, "/?author=Robert T. Kiyosaki")
        val resFuture  = getBooksByAuthor(req)
        val res = Await.result(resFuture, 3 seconds)
        res.size shouldEqual 1
        res.head.title shouldEqual "Rich Dad Poor Dad"
      }

      "author and one year is provided" in {
        val req = http.Request(http.Method.Get, "/?author=JRR Tolkien&year=1954")
        val resFuture  = getBooksByAuthor(req)
        val res = Await.result(resFuture, 3 seconds)
        res.size shouldEqual 1
        res.head.title shouldEqual "The Lord of the Rings"
      }

      "author and more than one year is provided" in {
        val req = http.Request(http.Method.Get, "/?author=JRR Tolkien&year=1954&year=1956")
        val resFuture  = getBooksByAuthor(req)
        val res = Await.result(resFuture, 3 seconds)
        res.size shouldEqual 2
        res.head.title shouldEqual "The Lord of the Rings"
        res.last.title shouldEqual "The Lord of the Rings 2"
      }
    }

    "reject request when author is not provided" when {
      "deny a user with wrong password" in {
        val request = http.Request(http.Method.Get, "/?year=1954&year=1956")
        an[IllegalArgumentException] should be thrownBy Await.result(getBooksByAuthor(request), 3 seconds)
      }
    }
  }

  "UploadBooks endpoint" should {
    "upload book correctly" when {
      "correct book passed" in {
        val newBook = Book("Things Fall Apart", "Chinua Achebe", 1958, "William Heinemann Ltd")
        val resFuture  = uploadBook(newBook)
        val res = Await.result(resFuture, 3 seconds)
        res.author shouldEqual newBook.author
        res.title shouldEqual newBook.title
        val insertedBook = Await.result(
          db.run((for (book <- books if book.author === newBook.author) yield  book).result), 3 seconds)

        insertedBook.size shouldEqual 1
        insertedBook.head.title shouldEqual newBook.title
      }
    }

    "reject book" when {
      "title is absent" in {
        val newBook = Book("Things Fall Apart", "", 1958, "William Heinemann Ltd")
        val newBook2 = Book("Things Fall Apart", null, 1958, "William Heinemann Ltd")
        an[IllegalArgumentException] should be thrownBy uploadBook(newBook)
        an[IllegalArgumentException] should be thrownBy uploadBook(newBook2)
      }

      "author is absent" in {
        val newBook = Book("", "Chinua Achebe", 1958, "William Heinemann Ltd")
        val newBook2 = Book(null, "Chinua Achebe", 1958, "William Heinemann Ltd")
        an[IllegalArgumentException] should be thrownBy uploadBook(newBook)
        an[IllegalArgumentException] should be thrownBy uploadBook(newBook2)
      }

      "year is absent" in {
        val newBook = Book("Things Fall Apart", "Chinua Achebe", 0, "William Heinemann Ltd")
        an[IllegalArgumentException] should be thrownBy uploadBook(newBook)
      }
    }
  }
}
