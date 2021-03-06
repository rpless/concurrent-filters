package io.github.rpless.filters

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** A Bloom Filter implementation that evaluates its hash functions concurrently
 *  when calling '''add''' and '''check'''.
 *
 * @param n The size of the filter
 * @param k The number of hash functions to use
 * @tparam T The type of object that is inserted into the filter.
 */
abstract class ConcurrentBloomFilter[T](n: Int, k: Int) {
  private[this] val filter = new Array[Boolean](n)
  private[this] val hashes = generateKHashes(k)

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
   * Ensure that the overriden '''buildHashes''' method returns the correct
   * number of hashes.
   *
   * @param k The number of hashes that should be generated
   * @return Returns the generated set of hashes.
   * @throws IllegalArgumentException An exception will be raised if the
   *                                  incorrect number of hash functions are generated.
   */
  private def generateKHashes(k: Int): Seq[(T => Int)] = {
    val hashes = buildHashes(k)
    if (hashes.length == k) {
      hashes
    } else {
      val errorMessage = s"Incorrect number of hash functions. Expected $k, but got ${hashes.length}"
      throw new IllegalArgumentException(errorMessage)
    }
  }

  /**
   * Build a set of hash functions.
   * Concrete implementations of the Bloom filter must provide this method.
   *
   * @param k The number of filters to build
   * @return The Set of hash functions to be used by the filter.
   */
  def buildHashes(k: Int): Seq[(T => Int)]
}
