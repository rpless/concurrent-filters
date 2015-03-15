package io.github.rpless.filters

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** A Bloom Filter implementation that parallelizes all of the hash
 * function evaluations when calling '''add''' and '''check'''.
 *
 * @param n The size of the filter
 * @param k The number of hash functions to use
 * @tparam T The type of object that is inserted into the filter.
 */
abstract class ParallelBloomFilter[T](n: Int, k: Int) {
  private[this] val filter = new Array[Boolean](n)
  private[this] val hashes = buildHashes(k)

  /**
   * Add a value into the Bloom filter.
   *
   * @param v The value to add
   */
  final def add(v: T): Unit = {
    val computations = hashes.map { hashFunc =>
      Future { filter.update(hashFunc(v), true) }
    }
    FutureUtil.awaitAll(computations)
  }

  /**
   * Check is a value is in the Bloom filter.
   *
   * @param v The value to check
   * @return Returns true if the value is believed to be in the filter
   */
  final def check(v: T): Boolean = {
    val computations = hashes.map { hashFunc =>
      Future { filter(hashFunc(v)) }
    }
    FutureUtil.forAllResults(computations)
  }

  /**
   * Build a set of hash functions.
   *
   * @param k The number of filters to build
   * @return The Set of hash functions to be used by the filter.
   */
  def buildHashes(k: Int): Seq[(T => Int)]
}
