package net.interdoodle.hanuman

import akka.actor.{ActorRef, Actor}
import akka.stm.Ref
import blueeyes._
import blueeyes.concurrent.Future
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString, ByteChunk}
import blueeyes.core.http.{HttpRequest, HttpResponse, HttpStatus, HttpStatusCodes}
import blueeyes.core.http.combinators.HttpRequestCombinators
import blueeyes.json.JsonAST._
import core.service._
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

  private val statusContent = """<!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
              <head>
                <script type="text/javascript" src="http://code.jquery.com/jquery-1.7.min.js"></script>
                <script type="text/javascript">
                  $(function() {
                    $.ajax("/json", {
                      contentType: "application/json",
                      success: function(data) {
                        $("#result").append(data.result);
                      }
                    });
                  });
                </script>
              </head>
              <body>
                <h1>Status</h1>
                <div id="result">
                </div>
              </body>
            </html>""";

  val versionMajor = 0
  val versionMinor = 1

  val hanumanService:HttpService[ByteChunk] = service("hanumanService", versionMajor + "." + versionMinor) {
    requestLogging {
      logging {
        log =>
          context: HttpServiceContext =>
            request {
              path("/json/") {
                jvalue {
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

      Future.sync(HttpResponse(
        /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
        content = Some(simulationID)))
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
      /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
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
      /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
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
        "Updated simulationStatus with new Hanuman instance " + hanumanRef.id + " and started hanuman"

      case "status" =>
        /** Return status of simulation with given simulationID */
        val simulation = simulationStatusRef.get.simulations(simulationID)
        val result:JArray = JArray({
          for (kv <- simulation) // Iterable[TextMatch]
            yield JString(kv._2.toString())
        }.toList)

        val jField  = JField("result", result)
        val jObject = JObject(jField :: Nil)
        jObject

      case "stop" =>
        val hanumanRef = hanumanRefOption.get
        val future = hanumanRef ? "stop"
        future.await // block until hanuman shuts down
        simulationStatus = future.result.get.asInstanceOf[SimulationStatus] // TODO return proper result
        // TODO return simulationStatus object in JSON format to client
        //simulationStatus = simulationStatusRef.get
        "Simulation " + simulationID + " stopped"

      case _ =>
        command + "is an unknown command"
    }
  }

   /** BlueEyes cookie support is not fully baked */
  /*private def sessionCookie(simulationID:String) = {
    val cookie = new HttpCookie {
      def name = "SessionID"
      def cookieValue = simulationID
      def expires = Some(HttpDateTime.parseHttpDateTimes("MON, 01-JAN-2001 00:00:00 UTC"))
      def domain = Option("")
      def path = Option("")
      // TODO add "; HttpOnly"
    }
    `Set-Cookie`(cookie :: Nil)
  }*/
}
