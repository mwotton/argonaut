package com.ephox
package argonaut

import org.scalacheck.Prop._
import org.scalacheck.Properties
import Data._
import Json._
import JsonIdentity._
import JsonLike._

object JsonTest extends Properties("Json") {
  property("not compose not is id") =
      forAll((j: Json) =>
        j.not.not == j)

  property("no-effect not equals !isBool") =
      forAll((j: Json) =>
        (j.not == j) != j.isBool)

  property("effect not equals isBool") =
      forAll((j: Json) =>
        (j.not != j) == j.isBool)

  property("effect withNumber implies isNumber") =
      forAll((j: Json, k: JsonNumber => JsonNumber) =>
        ((j withNumber k) == j) || j.isNumber)

  property("effect withString implies isString") =
      forAll((j: Json, k: JsonString => JsonString) =>
        ((j withString k) == j) || j.isString)

  property("effect withArray implies isArray") =
      forAll((j: Json, k: JsonArray => JsonArray) =>
        ((j withArray k) == j) || j.isArray)

  property("effect withObject implies isObject") =
      forAll((j: Json, k: JsonObject => JsonObject) =>
        ((j withObject k) == j) || j.isObject)

  property("Object prepend puts element on head") =
      forAll((j: Json, e: JsonAssoc) =>
        !j.isObject || (e ->: j).obj.map(_.toList.head) == Some(e))

  property("Array prepend puts element on head") =
      forAll((j: Json, e: Json) =>
        !j.isArray || (e -->>: j).array.map(_.head) == Some(e))

  property("jBool isBool") =
      forAll((b: Boolean) =>
        jBool[Json](b).isBool)

  property("jNumber isNumber") =
      forAll((n: JsonNumber) =>
        jNumber[Json](n).isNumber)

  property("jString isString") =
      forAll((s: String) =>
        jString[Json](s).isString)

  property("jArray isArray") =
      forAll((a: JsonArray) =>
        jArray[Json](a).isArray)

  property("jSingleArray is single array") =
      forAll((j: Json) =>
        jSingleArray[Json](j).array == Some(List(j)))

  property("jObject isObject") =
      forAll((a: JsonObject) =>
        jObject[Json](a).isObject)

  property("jSingleObject is single object") =
      forAll((f: JsonField, j: Json) =>
        (jSingleObject[Json](f, j).obj map (_.toList)) == Some(List((f, j))))

}