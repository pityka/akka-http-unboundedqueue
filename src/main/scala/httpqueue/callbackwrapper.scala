/* Copied from Akka's source, no modifications except this line and the package declaration */
/**
  * Copyright (C) 2015-2017 Lightbend Inc. <http://www.lightbend.com>
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

import akka.stream.SourceShape
import akka.stream.Graph
import akka.stream.stage._
import akka.stream.stage.OutHandler
import java.util.concurrent.ConcurrentLinkedQueue

trait CallbackWrapper[T] extends AsyncCallback[T] {
  private trait CallbackState
  private case class NotInitialized(list: List[T]) extends CallbackState
  private case class Initialized(f: T ⇒ Unit) extends CallbackState
  private case class Stopped(f: T ⇒ Unit) extends CallbackState

  /*
   * To preserve message order when switching between not initialized / initialized states
   * lock is used. Case is similar to RepointableActorRef
   */
  private[this] final val lock = new ReentrantLock

  private[this] val callbackState =
    new AtomicReference[CallbackState](NotInitialized(Nil))

  def stopCallback(f: T ⇒ Unit): Unit = locked {
    callbackState.set(Stopped(f))
  }

  def initCallback(f: T ⇒ Unit): Unit = locked {
    val list = (callbackState.getAndSet(Initialized(f)): @unchecked) match {
      case NotInitialized(l) ⇒ l
    }
    list.reverse.foreach(f)
  }

  override def invoke(arg: T): Unit = locked {
    callbackState.get() match {
      case Initialized(cb) ⇒ cb(arg)
      case list @ NotInitialized(l) ⇒
        callbackState.compareAndSet(list, NotInitialized(arg :: l))
      case Stopped(cb) ⇒ cb(arg)
    }
  }

  private[this] def locked(body: ⇒ Unit): Unit = {
    lock.lock()
    try body
    finally lock.unlock()
  }
}
