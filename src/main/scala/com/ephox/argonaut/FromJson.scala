package com.ephox
package argonaut

import scalaz._, Scalaz._
import FromJsonResult._
import JsonIdentity._

trait FromJson[A] {
  import FromJson._

  def apply(json: Json): FromJsonResult[A]

  def map[B](f: A => B): FromJson[B] =
    fromJson(json => apply(json) map f)

  def flatMap[B](f: A => FromJson[B]): FromJson[B] =
    fromJson(json => apply(json) flatMap (a => f(a)(json)))

  def mapJsonValue[B](f: FromJsonResult[A] => FromJsonResult[B]): FromJson[B] =
    fromJson[B](j => f(apply(j)))

  def mapError(f: String => String): FromJson[A] =
    mapJsonValue(_ mapError f)

  def swizzle[B](f: A => B, g: String => String): FromJson[B] =
    map(f) mapError g

  def swazzle[B](f: A => B, g: => String): FromJson[B] =
    swizzle(f, _ => g)
}

object FromJson extends FromJsons

trait FromJsons {
  def fromJson[A](f: Json => FromJsonResult[A]): FromJson[A] = new FromJson[A] {
    def apply(json: Json) = f(json)
  }

  implicit def ListFromJson[A](implicit from: FromJson[A]): FromJson[List[A]] =
    fromJson(j => j.array.fold(
      js => (js: List[Json]).traverse[FromJsonResult, A](z => from.apply(z)).mapError(e => "array contains an unexpected element [" + e + "]")
    , jsonError("not an array")
    ))

  implicit def StringFromJson: FromJson[String] =
    fromJson(j => j.string.fold(jsonValue, jsonError("not a string")))

  implicit def DoubleFromJson: FromJson[Double] =
    fromJson(j => j.number.fold(jsonValue, jsonError("not an double")))

  implicit def IntFromJson: FromJson[Int] =
    DoubleFromJson.swazzle(_.toInt, "not an int")

  implicit def LongFromJson: FromJson[Long] =
    DoubleFromJson.swazzle(_.toLong, "not a long")

  implicit def BooleanFromJson: FromJson[Boolean] =
    fromJson(j => j.bool.fold(jsonValue, jsonError("not a boolean")))

  implicit def JIntegerFrom: FromJson[java.lang.Integer] =
    IntFromJson.map(z => z)

  implicit def JLongFrom: FromJson[java.lang.Long] =
    LongFromJson.map(z => z)

  implicit def JBooleanFromJson: FromJson[java.lang.Boolean] =
    BooleanFromJson.map(z => z)

  implicit def FromJsonMonad: Monad[FromJson] = new Monad[FromJson] {
    def point[A](a: => A) = fromJson(_ => jsonValue(a))
    def bind[A, B](a: FromJson[A])(f: A => FromJson[B]) = a flatMap f
    override def map[A, B](a: FromJson[A])(f: A => B) = a map f
  }
}