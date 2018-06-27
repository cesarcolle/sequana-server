
import java.net.{InetAddress, UnknownHostException}

import DeviceControl._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object WebServer extends  JsonSupport {
  implicit def system: ActorSystem = ActorSystem("Sophia-system")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val deviceActor: ActorRef = system.actorOf(DeviceControl.props)


  val sequanaRoute: Route = concat(
    path("restartFrequencies") {
        post {
          entity(as[Frequencies]) { freqs =>
            println()
            complete("change made")
          }
        }
    }
    ,path("data") {
      post {
        complete("heyho")
      }
    }
    ,path("getAgenda"){
      get {
        val agenda = (deviceActor ? GetAllAgenda()).mapTo[Frequencies]
        complete(agenda)
      }
    }
    , path("changeADay") {
      post {
        entity(as[ChangeADay]) { changeDay =>


          complete("change made")
        }
      }
    }
  )
  def main(args: Array[String]): Unit = {

    Http().bindAndHandle(sequanaRoute, "localhost", 8080)

    println("server started... at localhost:" + 8080)

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
// JSON SUPPORT
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import DeviceControl._
  implicit val frequenciesM: RootJsonFormat[Frequencies] = jsonFormat1(Frequencies)
  implicit val deviceFrequencyM: RootJsonFormat[DeviceFrequency] = jsonFormat2(DeviceFrequency)
  implicit val tickM: RootJsonFormat[Tick] = jsonFormat4(Tick)
  implicit val durationM: RootJsonFormat[DurationTick] = jsonFormat2(DurationTick)
  implicit val changeADayM: RootJsonFormat[ChangeADay] = jsonFormat3(ChangeADay)
}

// COMPANION
object DeviceControl {

  case class Frequencies(devices : List[DeviceFrequency])
  case class DeviceFrequency(freqs: List[Tick], device : String)
  case class Tick(day: String, hour: Int, min: Int, duration: DurationTick)
  case class DurationTick(hour: Int, min: Int)

  case class GetAllAgenda()

  // change
  case class Restart(frenquences : Frequencies)
  case class ChangeADay(sourceCaptor : String, source: Tick, to : List[Tick])


  val props: Props = Props[DeviceControl]
}

// ACTOR
class DeviceControl extends Actor {
  import akka.http.scaladsl.model._
  val device: Map[String, String] = NetworkFinder.addressDeviceByName()

  var agenda : Frequencies = _

  val http = Http(context.system)

  override def receive: Receive = {

    // Manage frequency changes
    case f : Restart =>
      f.frenquences.devices.foreach(d =>
        Marshal(d.freqs).to[RequestEntity] flatMap { entity =>
          val request = HttpRequest(method = HttpMethods.POST, uri = device(d.device), entity = entity)
          http.singleRequest(request = request)
        }
      )
      // TODO : Do the rest.

    case GetAllAgenda() =>
      sender() ! agenda

    case c : ChangeADay =>
      val deviceChanged = agenda.devices.find(d => d.device == c.sourceCaptor)
      val newDevice = DeviceFrequency(deviceChanged.get.freqs.filter(_.day == c.source.day) ++ c.to, deviceChanged.get.device)

      agenda.devices.updated(agenda.devices.indexWhere(_.device == c.sourceCaptor), newDevice)
  }

}

// UTIL

object NetworkFinder {

  val devices = 1


  def addressDeviceByName() : Map[String, String] = {
    var result = Map.empty[String, String]
    // Translate hostname by their Adress IP !
    for (inet <-  1 until devices){
      try{
        // The 192.... have to be changed.
        val inetAdress = InetAddress.getByName("192.168.0.0." + inet.toString)
        result = result + (inetAdress.getHostName -> inetAdress.getHostAddress)

      }
      catch {
        case UnknownHostException => // NothingToDo
      }
    }
    result
  }

}