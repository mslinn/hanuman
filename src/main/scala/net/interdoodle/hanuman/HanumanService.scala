package net.interdoodle.hanuman

import akka.actor.{ActorRef, Actor}
import akka.stm.Ref

import blueeyes.BlueEyesServiceBuilder
import blueeyes.concurrent.Future
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString, ByteChunk}
import blueeyes.core.http.{HttpRequest, HttpResponse, HttpStatus, HttpStatusCodes}
import blueeyes.core.http.combinators.HttpRequestCombinators
import blueeyes.core.http.MimeTypes._
import blueeyes.core.service.{HttpService, HttpServiceContext}
import blueeyes.json.JsonAST._

import java.util.UUID
import net.interdoodle.hanuman.message.SimulationStatus
import net.interdoodle.hanuman.domain.Hanuman
import net.interdoodle.hanuman.domain.Hanuman.{Simulations, TextMatchMap}
import net.lag.logging.Logger


/**
 * @author Mike Slinn */
trait HanumanService extends BlueEyesServiceBuilder
  with HttpRequestCombinators
  with BijectionsChunkString
  with BijectionsChunkJson {

  private var hanuman:Option[Hanuman] = None
  private var hanumanRefOption:Option[ActorRef] = None
  private val simulations:Simulations = new Simulations()

  /** Contains simulationID->Option[WorkVisorRef] map */
  private var simulationStatus = new SimulationStatus(false, None, simulations)
  private val simulationStatusRef = new Ref(simulationStatus)

  private val contentUrl = System.getenv("CONTENT_URL")

  private val staticContent = <html xmlns="http://www.w3.org/1999/xhtml">
                                <head>
                                  <script type="text/javascript" src={contentUrl + "jquery-1.7.min.js"}></script>
                                  <script type="text/javascript" src={contentUrl + "index.js"}></script>
                                </head>
                                <body>
                                </body>
                              </html>

  val versionMajor = 0
  val versionMinor = 1

  val hanumanService:HttpService[ByteChunk] = service("hanumanService", versionMajor + "." + versionMinor) {
    requestLogging {
      logging {
        log =>
          context: HttpServiceContext =>
            request {
              path("/") {
                contentType(application/json) {
                  get { requestParam:HttpRequest[JValue] => reqHello(log) } ~
                  path('operation) {
                    get { request => reqOperation(log, request) }
                  } ~
                  path('operation/'id) {
                    get { request => reqDoCommand(log, request) }
                  } ~
                  path('operation/'id/'param) {
                    get { request => reqDoCommandParam(log, request) }
                  } ~
                  orFail(HttpStatusCodes.NotFound, "No handler found that could handle this request.") // return HTTP status 404
                } ~
                produce(text/html) {
                  request: HttpRequest[ByteChunk] =>
                    Future.sync(HttpResponse[String](content = Some(staticContent.buildString(true))))
                }
              }
            }
      }
    }
  }

  private def reqHello[T, S](log:Logger) = {
    val json = JString("Hanuman v" + versionMajor.toString + "." + versionMinor.toString)
    val response = HttpResponse[JValue](content = Some(json))
    log.info(response.toString())
    Future.sync(response)
  }

  private def reqOperation[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    if (operation=="newSimulation") {
      val document = Configuration().defaultDocument
      val simulationID = UUID.randomUUID().toString
      simulationStatus.putSimulation(simulationID, new TextMatchMap())
      simulationStatusRef.set(simulationStatus)
      // Future simulation parameters might include workCellsPerVisor and maxTicks so pass those values to Hanuman
      hanumanRefOption = Some(Actor.actorOf(
        new Hanuman(simulationID, Configuration().workCellsPerVisor, Configuration().maxTicks, document, simulationStatusRef)))

      Future.sync(HttpResponse(content = Some(JObject(List(JField("id", simulationID))))))
    } else {
      val msg = "The only operation that can be without a simulationID is newSimulation. You specified '" + operation + "'"
      Future.sync(HttpResponse(status=HttpStatus(400, msg), content = Some(msg)))
    }
  }

  private def reqDoCommand[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation).toString
    val simulationID = request.parameters('id).toString
    val simulation = simulationStatus.getSimulation(simulationID)
    Future.sync(HttpResponse(
      content = Some(if (simulation==None) {
        "Simulation with ID " + simulationID + " does not exist"
      } else
        doCommand(log, operation, simulationID)
    )))
  }

  private def reqDoCommandParam[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    val simulationID = request.parameters('id)
    val param = request.parameters('param)
    val simulation = simulationStatus.getSimulation(simulationID)
    Future.sync(HttpResponse(
      content = if (simulation==None)
        Some("Simulation with ID " + simulationID + " does not exist")
      else
        Some("Simulation ID=" + simulationID + "; operation: '" + operation + "'"))
    )
  }

  private def doCommand(log:Logger, command:String, simulationID:String):JValue = {
    command match {
      case "run" =>
        val hanumanRef = hanumanRefOption.get
        hanumanRef.start
        JObject(List(JField("result", "Updated simulationStatus with new Hanuman instance " + hanumanRef.id + " and started hanuman")))

      case "status" =>
        simulationStatusAsJson(simulationID)

      case "stop" =>
        val hanumanRef = hanumanRefOption.get
        val future = hanumanRef ? "stop"
        future.await // block until hanuman shuts down
        simulationStatusAsJson(simulationID)

      case _ =>
        command + " is an unknown command"
    }
  }

  /** Return status of simulation with given simulationID */
  private def simulationStatusAsJson(simulationID:String) = {
    val simulation = simulationStatusRef.get.simulations(simulationID)
    val result = JArray({
      for (kv <- simulation) // Iterable[TextMatch]
        yield kv._2.decompose
    }.toList)
    JObject(JField("result", result) :: Nil)
  }
}
