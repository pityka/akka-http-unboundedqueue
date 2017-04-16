An unbounded queue in front of a Akka HTTP's `superPool` connection flow. In some cases this might be a solution to the overflowing connection pool (`akka.stream.BufferOverflowException`) from single requests. In some other cases this is very bad idea.

If you application makes a potentially unbounded stream of requests, then you should follow Akka's documentation and not use this.

Otherwise this is a drop in replacement of `Http().singleRequest`.

```
import httpqueue._
import akka.actor._
import akka.http.scaladsl.model._
val system = ActorSystem("test")
val request : HttpRequet = ???
val response : Future[HttpResponse] = HttpQueue(system).queue(request)
```

HttpQueue.queue is a `HttpRequest => Future[HttpResponse]`. The request is sent to an ActorPublisher which either queues it up, or forwards to the pipeline.

HttpQueue is an Akka Extension, thus is unique per ActorSystem and the `Http().superPool()` flow is materialized only once per ActorSystem.

There is a graph stage based variant as well.

# License

MIT (c) Istvan Bartha, except callbackwrapper.scala which is copied from Akka's source and is Apache 2.
