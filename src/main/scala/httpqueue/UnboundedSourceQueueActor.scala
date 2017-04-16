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
import akka.util._
import scala.concurrent.{ExecutionContext, Future, Promise}
import java.util.concurrent.ConcurrentLinkedQueue

class UnboundedSourceQueueActor
    extends Actor
    with ActorPublisher[(HttpRequest, Promise[HttpResponse])]
    with Stash {

  val queue = new ConcurrentLinkedQueue[(HttpRequest, Promise[HttpResponse])]()

  def receive = {
    case m: (HttpRequest, Promise[HttpResponse]) @unchecked =>
      if (isActive && totalDemand > 0) {
        onNext(m)
      } else {
        queue.add(m)
      }
    case ActorPublisherMessage.Request(n) =>
      var i = 0L
      while (i < n) {
        val h = queue.poll
        if (h != null) {
          onNext(h)
        }
        i += 1
      }

  }
}
