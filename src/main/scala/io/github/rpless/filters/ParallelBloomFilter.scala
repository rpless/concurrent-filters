package io.github.rpless.filters

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

abstract class ParallelBloomFilter[T](n: Int, k: Int) {
  private[this] val filter = new Array[Boolean](n)
  private[this] val hashes = buildHashes(k)

  final def add(v: T): Unit = {
    val computations = hashes.map { hashFunc =>
      Future { filter.update(hashFunc(v), true) }
    }
    FutureUtil.awaitAll(computations)
  }

  final def check(v: T): Boolean = {
    val computations = hashes.map { hashFunc =>
      Future { filter(hashFunc(v)) }
    }
    FutureUtil.forAllResults(computations)
  }

  def buildHashes(k: Int): Seq[(T => Int)]
}
