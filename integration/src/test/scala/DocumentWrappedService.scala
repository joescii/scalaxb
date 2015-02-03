package scalaxb.stockquote.server

import javax.jws.{ WebService, WebMethod, WebParam, WebResult }
import javax.jws.soap.SOAPBinding
import javax.jws.soap.SOAPBinding.{Style, Use, ParameterStyle}
import collection.mutable
import scala.concurrent._, duration.Duration

@WebService(name = "DocumentWrappedService", serviceName = "DocumentWrappedService")
@SOAPBinding(style = Style.DOCUMENT, use = Use.LITERAL, parameterStyle = ParameterStyle.WRAPPED)
class DocumentWrappedService(sleepTime: Duration) {
  private val buffer = mutable.Map[String, Double]()

  def price(symbol: String): Double = {
    Thread.sleep(sleepTime.toMillis)
    buffer.getOrElse(symbol, 42.0)
  }

  def update(symbol: String, price: Double): Unit =
    buffer(symbol) = price

  def infos: Array[String] = Array("x")
}
