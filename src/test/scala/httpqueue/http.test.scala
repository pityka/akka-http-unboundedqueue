package httpqueue

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.duration._
import scala.concurrent._
import akka.actor.{Actor, PoisonPill, ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import akka.stream._
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import scala.util._

class HttpTest extends FunSpec with ShouldMatchers {

  def makeTest(numberOfRequests: Int,
               target: String = "http://127.0.0.1:8080/IMG_1802.MOV")(
      exec: (HttpRequest, ActorSystem,
             ActorMaterializer) => Future[HttpResponse]) = {
    implicit val system =
      ActorSystem("test",
                  ConfigFactory.parseString(
                    """
         akka.http.client.parsing.max-content-length = infinite
         akka.http.client.host-connection-pool.max-connections = 2
         akka.http.host-connection-pool.max-connections = 2
         akka.http.host-connection-pool.max-open-requests = 2
         akka.loglevel=DEBUG
         """))
    implicit val mat = ActorMaterializer()
    val requests = 1 to numberOfRequests map (i =>
                                                HttpRequest(uri = target) -> i)

    import system.dispatcher

    val responses = Await.result(
      Future.sequence(
        requests.map(rs => exec(rs._1, system, mat) -> rs._2).map {
          case (fut, i) =>
            fut.map { resp =>
              resp.discardEntityBytes()
              (true, i)
            }.recover {
              case e =>
                println(e)
                (false, i)
            }

        }),
      atMost = 600 seconds)
    system.shutdown
    responses
  }

  describe(
    " 10 concurrent request for bigger files on a connection pool with 2 slots ") {
    it(" singleRequest should fail ") {
      val responses = makeTest(10) {
        case (rs, as, mat) =>
          implicit val as2 = as
          implicit val mat2 = mat
          Http().singleRequest(rs)
      }
      responses.count(_._1) should equal(2)
      responses.count(x => !x._1) should equal(8)
    }
    it(" actor ") {
      val responses = makeTest(10) {
        case (rs, as, mat) =>
          HttpQueue(as).queue(rs)
      }
      responses.count(_._1) should equal(10)
      responses.count(x => !x._1) should equal(0)
    }
    it(" graphstage ") {
      val responses = makeTest(10) {
        case (rs, as, mat) =>
          HttpQueueWithGraphStage(as).queue(rs)
      }
      responses.count(_._1) should equal(10)
      responses.count(x => !x._1) should equal(0)
    }
  }

  describe(
    " 100 concurrent request for bigger files on a connection pool with 2 slots ") {
    it(" singleRequest should fail ") {
      val responses = makeTest(100) {
        case (rs, as, mat) =>
          implicit val as2 = as
          implicit val mat2 = mat
          Http().singleRequest(rs)
      }
      responses.count(_._1) should equal(2)
      responses.count(x => !x._1) should equal(98)
    }
    it(" actor ") {
      val responses = makeTest(100) {
        case (rs, as, mat) =>
          HttpQueue(as).queue(rs)
      }
      responses.count(_._1) should equal(100)
      responses.count(x => !x._1) should equal(0)
    }
    it(" graphstage ") {
      val responses = makeTest(100) {
        case (rs, as, mat) =>
          HttpQueueWithGraphStage(as).queue(rs)
      }
      responses.count(_._1) should equal(100)
      responses.count(x => !x._1) should equal(0)
    }
  }
}
