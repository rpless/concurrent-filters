package io.github.rpless.filters

import org.scalatest.{FlatSpec, Matchers}

object BloomFilterSpec {
  class ExampleFilter(val n: Int, val k: Int) extends ConcurrentBloomFilter[String](n, k) {
    override def buildHashes(k: Int): Seq[(String) => Int] = {
      (0 until k).map { i: Int => { str: String => ((str + i).hashCode % n + n) % n } }
    }
  }

  class BadFilter(val n: Int, val k: Int) extends ConcurrentBloomFilter[String](n, k) {
    override def buildHashes(k: Int): Seq[(String) => Int] = {
      (0 to k).map { i: Int => { str: String => str.hashCode } }
    }
  }
}

class BloomFilterSpec extends FlatSpec with Matchers {
  import io.github.rpless.filters.BloomFilterSpec._

  val testString1 = "Hello"
  val testString2 = "World"

  "A ConcurrentBloomFilter" should "check as true if the element has been added" in {
    val filter = new ExampleFilter(1000, 10)
    filter.add(testString1)
    filter.check(testString1) should be (true)
  }

  it should "check as false if the element has not been added and there are no other elements in the filter" in {
    val filter = new ExampleFilter(1000, 10)
    val result: Boolean = filter.check(testString1)
    println(result)
    result should be (false)
  }

  it should "check as true for multiple added elements" in {
    val filter = new ExampleFilter(1000, 10)
    filter.add(testString1)
    filter.add(testString2)
    filter.check(testString1) should be (true)
    filter.check(testString2) should be (true)
  }

  it should "raise an IllegalArgumentException if the incorrect number of hash functions is generated" in {
    a [IllegalArgumentException] should be thrownBy (new BadFilter(1000, 10))
  }
}
