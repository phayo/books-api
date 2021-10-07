import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64

import scala.util.{Failure, Success}

class SecurityFilter extends SimpleFilter[Request, Response]{
  private val keyConfig = ConfigFactory.load("application.conf").getConfig("encryption")
  private val authValid = ConfigFactory.load("application.conf").getInt("authValidInMinutes")
  private val encryptKeyBase64 = keyConfig.getString("key")
  private val encryptIVBase64 = keyConfig.getString("iv")
  private val encryptUtil = EncryptionUtil(encryptKeyBase64, encryptIVBase64)
  private val separator = "--"
  private val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()


  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.uri match {
      case path if path == "/" => service(request)
      case path if path.startsWith("/login") => loginService(request, service)
      case path if path.startsWith("/register") => registerService(request, service)
      case path if path.startsWith("/me/books/") => validateService(request, service)
      case _ => throw NotFoundException("Cannot find URI")
    }
  }

  private def generateAuth(username: String): String = {
    val now = System.currentTimeMillis() + (authValid * 60 * 1000)
    encryptUtil.encrypt(username + separator + now)
  }

  private def loginValid(auth: String): Boolean = encryptUtil.decrypt(auth) match {
      case Success(value) =>
        value.split(separator)(1).toLong > System.currentTimeMillis()
      case Failure(_) => false
    }

  private def registerService(request: Request, service: Service[Request, Response]): Future[Response] = {
    val response  = service(request)
    val user = mapper.readValue(request.getContentString(), new TypeReference[User] {})
    response.map(res => {
      if (res.status == Status.Created) {
        res.authorization = generateAuth(user.username)
        res
      } else throw new Exception
    })
  }

  private def loginService(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.authorization match {
      case Some(value) => service(request).map(res => {
        val username = new String(Base64.decodeBase64(value.substring(5).getBytes)).split(":")(0)
        if (res.status == Status.Ok) {
          res.authorization = generateAuth(username)
          res
        } else throw UnAuthorizedException("Login failed")
      })
      case None => throw UnAuthorizedException("Authorization not found")
    }
  }

  def validateService(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.authorization match {
      case Some(x) => if  (loginValid(x.substring(5))) service(request) else throw UnAuthorizedException("Please login to continue")
      case None => throw UnAuthorizedException("Please login to continue")
    }
  }
}
