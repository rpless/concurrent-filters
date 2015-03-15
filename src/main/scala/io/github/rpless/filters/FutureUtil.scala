package io.github.rpless.filters

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A set of utilities for dealing with sequences of Futures.
 */
private[filters] object FutureUtil {

  /**
   * Take a sequence of Futures that return booleans and validate that they all return true.
   *
   * @param fs The sequence of futures to execute
   * @return Returns true if all of the Future values are true.
   */
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

  /**
   * Wait for all of the futures to complete.
   *
   * @param fs The sequence of futures to execute
   * @tparam T Return type of the Future
   * @return Returns the result of the last future to be executed
   */
  def awaitAll[T](fs: Seq[Future[T]]): T = {
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
    Await.result(promise.future, Duration.Inf)
  }
}