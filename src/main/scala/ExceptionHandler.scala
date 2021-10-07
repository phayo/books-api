import com.twitter.finagle.http.Status.{BadRequest, Forbidden, InternalServerError, NotFound, Unauthorized}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.{Duration, Future, Timer}

import java.time.Instant

case class UnAuthorizedException(message: String) extends Exception(message)
case class ForbiddenException(message: String) extends Exception(message)
case class NotFoundException(message: String) extends Exception(message)

case class ErrorResponse(message: String, uri: String, timeStamp: Instant){
  def toJson: String =
    s"""
       |{
       |  "message": "$message",
       |  "uri": "$uri",
       |  "timeStamp": "${timeStamp.toString}"
       |}
       |""".stripMargin
}

class ExceptionHandler extends SimpleFilter[Request, Response] {
  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    // `handle` asynchronously handles exceptions.
    service(request) handle {
      case error =>
        error.printStackTrace()

        val statusCode: Status = error match {
          case x: IllegalArgumentException => BadRequest
          case x: UnAuthorizedException => Unauthorized
          case x: ForbiddenException => Forbidden
          case x: NotFoundException => NotFound
          case _ => InternalServerError
        }
        val errorResponse: Response = Response(statusCode)
        errorResponse.setContentString(ErrorResponse(error.getMessage, request.uri, Instant.now()).toJson)
        errorResponse.contentType = "application/json"

        errorResponse
    }
  }
}

class TimeoutFilter[Req, Rep](timeout: Duration, timer: Timer)
  extends SimpleFilter[Req, Rep] {

  def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    val res = service(request)
    res.within(timer, timeout)
  }
}