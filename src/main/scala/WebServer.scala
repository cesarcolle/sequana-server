
import java.net.{InetAddress, UnknownHostException}

import DeviceControl.Frequencies
import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer


object WebServer extends  JsonSupport {
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit def system: ActorSystem = ActorSystem()

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
  )
  def main(args: Array[String]): Unit = {
    println("start")
  }
}
// JSON SUPPORT
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import DeviceControl._
  implicit val frequenciesM: RootJsonFormat[Frequencies] = jsonFormat1(Frequencies)
  implicit val deviceFrequencyM: RootJsonFormat[DeviceFrequency] = jsonFormat2(DeviceFrequency)
  implicit val tickM: RootJsonFormat[Tick] = jsonFormat4(Tick)
  implicit val durationM: RootJsonFormat[Duration] = jsonFormat2(Duration)
}

// ACTOR

object DeviceControl {
  case class Frequencies(devices : List[DeviceFrequency])
  case class DeviceFrequency(freqs: List[Tick], device : String)
  case class Tick(day: String, hour: Int, min: Int, duration: Duration)
  case class Duration(hour: Int, min: Int)
}

class DeviceControl extends Actor {
  import akka.http.scaladsl.model._
  val device: Map[String, String] = NetworkFinder.addressDeviceByName()

  implicit val system = ActorSystem()
  val http = Http(system)

  override def receive: Receive = {

    // Manage frequency changes
    case f : Frequencies =>
      f.devices.foreach(d =>
        Marshal(d.freqs).to[RequestEntity] flatMap { entity =>
          val request = HttpRequest(method = HttpMethods.POST, uri = device(d.device), entity = entity)
          http.singleRequest(request = request)
        }
      )
      // TODO : Do the rest.
  }
}

// UTIL

object NetworkFinder {

  val devices = List()// TO BE COMPLETED


  def addressDeviceByName() : Map[String, String] = {
    var result = Map.empty[String, String]
    // Translate hostname by their Adress IP !
    for (inet <- devices.indices){
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