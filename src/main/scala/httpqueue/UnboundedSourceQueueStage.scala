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
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

import akka.stream.stage.StageLogging
import akka.stream.SourceShape
import akka.stream.Graph
import akka.stream.stage._
import akka.stream.stage.OutHandler
import java.util.concurrent.ConcurrentLinkedQueue

/** A handle to the queue backing up the Source
  * created by UnboundedSourceQueue.apply */
trait QueueControl[T] {
  def send(t: T): Unit
  def close: Unit
}

object UnboundedSourceQueue {

  /** Creates a source that is materialized as a [[QueueControl]]
    *
    * The created source is accepts any number of elements cached in an unbounded queue.
    * It is safe to send elements to the queue from multiple threads.
    * It is the user's responsibility to track the elements in the pipeline.
    * There is no guarantee that the elements sent to the queue will be processed.
    *
    * The alternative is to use GraphStage
    */
  def fromActorPublisher[T] =
    Source.actorPublisher(Props[UnboundedSourceQueueActor])

  /** Creates a source that is materialized as a [[QueueControl]]
    *
    * The created source is accepts any number of elements cached in an unbounded queue.
    * It is safe to send elements to the queue from multiple threads.
    * It is the user's responsibility to track the elements in the pipeline.
    * There is no guarantee that the elements sent to the queue will be processed.
    *
    * See potential threading issues: https://github.com/akka/akka/issues/20503
    *
    * The alternative is to use ActorPublisher
    * This is here because ActorPublisher is deprecated in Akka 2.5
    */
  def fromStage[T] = Source.fromGraph(new UnboundedSourceQueue[T])
}

class UnboundedSourceQueue[T]
    extends GraphStageWithMaterializedValue[SourceShape[T], QueueControl[T]] {
  val out: Outlet[T] = Outlet("UnboundedSourceQueue")
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes) = {

    val stageLogic = new GraphStageLogic(shape) with CallbackWrapper[Option[T]]
    with StageLogging {
      val queue = new ConcurrentLinkedQueue[T]()

      override def preStart(): Unit = {
        log.debug("Stage preStart.")
        initCallback(callback.invoke)
      }

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val h = queue.poll
          if (h != null) {
            log.debug("Stage pulled, pushing head of queue.")
            push(out, h)
          } else {
            log.debug("Stage pulled, queue empty, not pushing.")
          }

        }
      })

      private val callback: AsyncCallback[Option[T]] = getAsyncCallback {
        case None =>
          log.debug("Callback. Stage complete.")
          complete(out)
        case Some(t: T @unchecked) =>
          if (isAvailable(out)) {
            log.debug(
              "Element received via callback. Stage is pulled, pushing element.")
            push(out, t)
          } else {
            log.debug(
              "Element received via callback. Stage is not pulled, queuing element.")
            queue.add(t)
          }
      }

    }

    val qc = new QueueControl[T] {
      def send(t: T) = {
        stageLogic.invoke(Some(t))
      }
      def close = stageLogic.invoke(None)
    }

    (stageLogic, qc)
  }
}
