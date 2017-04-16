/*
 * The MIT License
 *
 * Copyright (c) 2017 Istvan Bartha
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package httpqueue

import akka.actor._
import akka.stream._
import akka.stream.actor._
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import scala.concurrent.Promise

class HttpQueueImpl(implicit as: ActorSystem, mat: ActorMaterializer)
    extends Extension {

  type T = (HttpRequest, Promise[HttpResponse])

  private val ac: ActorRef = UnboundedSourceQueue
    .fromActorPublisher[T]
    .via(Http().superPool[Promise[HttpResponse]]())
    .toMat(Sink.foreach({
      case (t, p) =>
        p.complete(t)
    }))(Keep.left)
    .run()

  def queue(rq: HttpRequest) = {
    val p = Promise[HttpResponse]()
    ac ! rq -> p
    p.future
  }

}

/** Akka extension providing a simple to use HttpRequest => Future[HttpResponse]
  *
  * Http().singleRequest may overflow the connection pool,
  * while this keeps queueing up requests in an unbounded queue before sending
  * them to the connection pool.
  */
object HttpQueue extends ExtensionId[HttpQueueImpl] with ExtensionIdProvider {

  override def lookup = HttpQueue
  override def createExtension(system: ExtendedActorSystem) = {
    implicit val s = system
    implicit val mat = ActorMaterializer()
    new HttpQueueImpl
  }

  override def get(system: ActorSystem): HttpQueueImpl = super.get(system)
}
