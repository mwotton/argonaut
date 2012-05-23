package com.ephox
package argonaut

trait JsonData[A] {
  def toJson(a: A): Json
  def fromJson(json: Json): FromJsonResult[A]
}

object JsonData extends JsonDatas

trait JsonDatas {
  implicit def ToJsonData[A](implicit to: ToJson[A], from: FromJson[A]): JsonData[A] = new JsonData[A] {
    def fromJson(json: Json) = from(json)
    def toJson(a: A) = to(a)
  }
}
