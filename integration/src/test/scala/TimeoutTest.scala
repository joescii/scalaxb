import scalaxb.compiler.wsdl11.Driver
import java.io.File
import scalaxb.compiler.Config
import scalaxb.stockquote.server._
import scala.concurrent._, duration.Duration

object TimeoutTest extends TestBase with JaxwsTestBase {
  override val module = new Driver // with Verbose

  def serviceImpl:DocumentWrappedService = new DocumentWrappedService(serverSleepTime)
  def serviceAddress: String = "document-wrapped"

  step {
    startServer
  }

  val packageName = "stockquote"
  val wsdlFile = new File(s"integration/target/$serviceAddress.wsdl")
  lazy val generated = {
    writeStringToFile(retrieveWsdl, wsdlFile)
    module.process(wsdlFile,
      Config(packageNames = Map(None -> Some(packageName)),
      packageDir = true, outdir = tmp, async = true))
  }
  val httpclientsFile = new File(tmp, "httpclients_dispatch0111_timeout.scala")
  copyFileFromResource("httpclients_dispatch0111_timeout.scala", httpclientsFile)

  def requestTimeout: Int = 20
  def connectionTimeout: Int = 20
  def serverSleepTime = Duration(30, "seconds") // specs2 has its own Duration
  def totalTestAwait: Int = 60

  "request timeout times out" in {
    (List(s"""import stockquote._
      import scala.concurrent._, duration._
      import java.util.concurrent.ExecutionException
      import java.util.NoSuchElementException""",
      s"""val service = (new DocumentWrappedServiceSoapBindings with scalaxb.Soap11ClientsAsync with scalaxb.DispatchHttpClientsTimeout {
        override def requestTimeout = $requestTimeout seconds
        override def connectionTimeout = $connectionTimeout seconds
      }).service""",
      s"""val fresponse: Future[Throwable] = service.price(Some("GOOG")).failed""",
      s"""val response: Throwable = Await.result(fresponse, $totalTestAwait seconds)""",
      s"""response match {
        case e: ExecutionException => ()
        case e: NoSuchElementException => sys.error("Request was supposed to timeout but it returned successfully")
        case e: Throwable => throw e
      }""",
      s"""true"""), httpclientsFile :: generated) must evaluateTo(true,
      outdir = "./tmp", usecurrentcp = true)
  }

  step {
    stopServer
  }
}