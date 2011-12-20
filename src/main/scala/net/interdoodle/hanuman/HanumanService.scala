package net.interdoodle.hanuman

import akka.actor.{ActorRef, Actor}
import akka.event.EventHandler
import collection.mutable.HashSet
import java.util.UUID
import message._
import net.lag.logging.Logger

import blueeyes.BlueEyesServiceBuilder
import blueeyes.concurrent.Future
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString, ByteChunk}
import blueeyes.core.http.{HttpRequest, HttpResponse, HttpStatus, HttpStatusCodes}
import blueeyes.core.http.MimeTypes._
import blueeyes.core.http.combinators.HttpRequestCombinators
import blueeyes.core.service.{HttpService, HttpServiceContext}
import blueeyes.json.JsonAST._
import blueeyes.json.Printer

import domain.types._
import domain.Hanuman


/** BlueEyes service handler for Hanuman requests.
 * @author Mike Slinn */
trait HanumanService extends BlueEyesServiceBuilder
    with HttpRequestCombinators
    with BijectionsChunkString
    with BijectionsChunkJson {
  private val contentUrl = System.getenv("CONTENT_URL")
  private val hanumanRefOption:Option[ActorRef] = Some(Actor.actorOf(new Hanuman))
  private val simulationIds = new HashSet[String]
  private val staticContent = <html xmlns="http://www.w3.org/1999/xhtml">
                                <head>
                                  <title>Hanuman Simulation</title>
                                  <link rel="stylesheet" type="text/css" href={contentUrl + "jquery-ui-1.8.16.css"} />
                                  <link rel="stylesheet" type="text/css" href={contentUrl + "stylesheet.css"} />
                                  <script type="text/javascript" src={contentUrl + "jquery-1.7.min.js"}></script>
                                  <script type="text/javascript" src={contentUrl + "jquery-ui-1.8.16.min.js"}></script>
                                  <script type="text/javascript" src={contentUrl + "index.js"}></script>
                                </head>
                                <body></body>
                              </html>
  val versionMajor = 0
  val versionMinor = 2

  val hanumanService:HttpService[ByteChunk] = service("hanumanService", versionMajor + "." + versionMinor) {
    requestLogging {
      logging {
        log =>
          context: HttpServiceContext =>
            request {
              path("/") {
                contentType(application/json) {
                  get { requestParam:HttpRequest[JValue] => reqVersion(log) } ~
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

  /* Mechanism to detect completion of a simulation; Hanuman also sets a completion flag in
     simulationResults. This shown how a non-actor can retrieve results from an actor. */
  EventHandler.addListener(Actor.actorOf(new Actor {
    self.dispatcher = EventHandler.EventHandlerDispatcher

    def receive = {
      case EventHandler.Error(cause, instance, message) =>
        EventHandler.error(this, instance.toString + "\n" + message.toString + "\n" + cause.toString)

      case EventHandler.Warning(instance, message) =>
        EventHandler.error(this, instance.toString + "\n" + message.toString)

//      case EventHandler.Info(instance, message) =>
//        EventHandler.info(this, instance.toString + "\n" + message.toString)

      case EventHandler.Debug(instance, message) =>
        EventHandler.debug(this, instance.toString + "\n" + message.toString)

      case SimulationStopped(simulationID) =>
        EventHandler.info(this, "Notify client that simulation " + simulationID + " is done")

      case _ =>
        // ignore
    }
  }))

  hanumanRefOption.get.start()


  private def reqVersion[T, S](log:Logger) = {
    val json = JString("Hanuman v" + versionMajor.toString + "." + versionMinor.toString)
    val response = HttpResponse[JValue](content = Some(json))
    log.info(response.toString())
    Future.sync(response)
  }

  /** Handle requests that only specify an operation without parameters */
  private def reqOperation[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    operation match {
      case "list" =>
        Future.sync(HttpResponse(content = Some(listSimulationsAsJson)))

      case "newSimulation" =>
        val simulationId = UUID.randomUUID().toString
        simulationIds += simulationId
        Future.sync(HttpResponse(content = Some(JObject(List(
          JField("simulationId", JString(simulationId)) ::
          JField("version", JString(versionMajor + "." + versionMinor)) ::
          Nil)))))

      case _ =>
        val msg = "The only operation that can be without a simulationID is newSimulation. You specified '" + operation + "'"
        Future.sync(HttpResponse(status=HttpStatus(400, msg), content = Some(msg)))
    }
  }

  /** Handle requests that only specify an operation with 1 parameter */
  private def reqDoCommand[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation).toString
    val simulationID = request.parameters('id).toString
    Future.sync(HttpResponse(
      content = Some(if (simulationIds.contains(simulationID)) {
        doCommand(log, operation, simulationID)
      } else
        "Simulation with ID " + simulationID + " does not exist"
      )))
  }

  private def reqDoCommandParam[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    val simulationID = request.parameters('id)
    val param = request.parameters('param)
    Future.sync(HttpResponse(
      content = if (simulationIds.contains(simulationID))
        Some("Simulation ID=" + simulationID + "; operation: '" + operation + "'")
    else
        Some("Simulation with ID " + simulationID + " does not exist"))
    )
  }

  private def doCommand(log:Logger, command:String, simulationId:String):JValue = command match {
    case "run" =>
      // Future simulation parameters sent from web client might include workCellsPerVisor, maxTicks and document
      // so pass those values here
      hanumanRefOption.get ! NewSimulation(simulationId, Configuration().workCellsPerVisor,
                                           Configuration().maxTicks, Configuration().defaultDocument)
      val result = simulationStatusAsJson(simulationId)
      println("Result=" + Printer.compact(Printer.render(result)))
      result

    case "status" =>
      simulationStatusAsJson(simulationId)

    case "stop" =>
      val hanumanRef = hanumanRefOption.get
      val future = hanumanRef ? StopSimulation(simulationId)
      future.await // block until hanuman shuts down; times out even though Hanuman does shut down
      simulationStatusAsJson(simulationId)

    case _ =>
      command + " is an unknown command"
  }

  private def listSimulationsAsJson:JValue = {
    (hanumanRefOption.get ? ListSimulations).await.get match {
      case Some(simulationStatuses:SimulationStatuses) =>
        JField("result", JObject(
          (simulationStatuses.values map {(simulationStatus:SimulationStatus) =>
              simulationStatus.decompose
          }).asInstanceOf[List[JField]]
        ))

      case None => // time out
        JField("result", JString("Time out waiting for list of simulations from Hanuman"))
    }
  }

  /** @return status of simulation with given simulationID as JSON */
  private def simulationStatusAsJson(simulationId:String):JValue = {
    val resultOption = (hanumanRefOption.get ? GetSimulationStatus(simulationId)).await.get
    resultOption match {
      case Some(result) =>
        val simulationStatus = result.asInstanceOf[SimulationStatus]
        JObject(
          JField("result", simulationStatus.decompose) :: Nil
        )

      case None => // time out
        JString("time out waiting for list of simulations from Hanuman")
    }
  }
}
