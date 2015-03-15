package io.github.rpless.filters

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

private[filters] object FutureUtil {

  def forAllResults(fs: Seq[Future[Boolean]]): Boolean = {
    val remaining = new AtomicInteger(fs.length)
    val promise: Promise[Boolean] = Promise[Boolean]()
    fs.foreach {
      _.onComplete {
        case Success(v) if v =>
          if (remaining.decrementAndGet() == 0) {
            promise.tryComplete(Try(v))
          }
        case Success(v) if !v => promise.tryComplete(Try(false))
        case f: Failure[Boolean] => promise.tryComplete(f)
      }
    }
    Await.result(promise.future, Duration.Inf)
  }

  def awaitAll[T](fs: Seq[Future[T]]): Future[T] = {
    val remaining = new AtomicInteger(fs.length)
    val promise: Promise[T] = Promise[T]()

    fs.foreach {
      _.onComplete {
        case s @ Success(_) =>
          if (remaining.decrementAndGet() == 0) {
            promise.tryComplete(s)
          }
        case f @ Failure(_) => promise.tryComplete(f)
      }
    }
    promise.future
  }
}
