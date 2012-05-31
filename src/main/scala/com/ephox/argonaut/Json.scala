package com.ephox
package argonaut

import scalaz._, Scalaz._

/**
 * A data type representing possible <a href="http://www.json.org/">JSON</a> values.
 *
 * @author Tony Morris
 * @author Dylan Just
 * @author Mark Hibberd
 */
sealed trait Json {
  import Json._
  import JsonIdentity._

  /**
   * The catamorphism for the JSON value data type.
   */
  def fold[X](
    jsonNull: => X,
    jsonBool: Boolean => X,
    jsonNumber: JsonNumber => X,
    jsonString: String => X,
    jsonArray: JsonArray => X,
    jsonObject: JsonObject => X
  ): X =
    this match {
      case JNull      => jsonNull
      case JBool(b)   => jsonBool(b)
      case JNumber(n) => jsonNumber(n)
      case JString(s) => jsonString(s)
      case JArray(a)  => jsonArray(a)
      case JObject(o) => jsonObject(o)
    }

  /**
   * Constructor a cursor from this JSON value.
   */
  def unary_+ : Option[Cursor] =
    Cursor.cursor(this)

  /**
   * Return `true` if this JSON value is `null`, otherwise, `false`.
   */
  def isNull: Boolean =
    this == JNull

  /**
   *  Returns the possible boolean of this JSON value.
   */
  def bool: Option[Boolean] =
    jBoolL.get(this)

  /**
   *  Returns the possible number of this JSON value.
   */
  def number: Option[JsonNumber] =
    jNumberL.get(this)

  /**
   * Returns the possible string of this JSON value.
   */
  def string: Option[JsonString] =
    jStringL.get(this)

  /**
   * Returns the possible array of this JSON value.
   */
  def array: Option[JsonArray] =
    jArrayL.get(this)

  /**
   * Returns the possible object of this JSON value.
   */
  def obj: Option[JsonObject] =
    jObjectL.get(this)

  /**
   * If this is a JSON boolean value, invert the `true` and `false` values, otherwise, leave unchanged.
   */
  def not: Json =
    jBoolL mod (!_, this)

  /**
   * If this is a JSON number value, run the given function on the value, otherwise, leave unchanged.
   */
  def withNumber(k: JsonNumber => JsonNumber): Json =
    jNumberL mod (k, this)

  /**
   * If this is a JSON string value, run the given function on the value, otherwise, leave unchanged.
   */
  def withString(k: JsonString => JsonString): Json =
    jStringL mod (k, this)

  /**
   * If this is a JSON array value, run the given function on the value, otherwise, leave unchanged.
   */
  def withArray(k: JsonArray => JsonArray): Json =
    jArrayL mod (k, this)

  /**
   * If this is a JSON object value, run the given function on the value, otherwise, leave unchanged.
   */
  def withObject(k: JsonObject => JsonObject): Json =
    jObjectL mod (k, this)

  /**
   * If this is a JSON object, then prepend the given value, otherwise, return a JSON object with only the given value.
   */
  def ->:(k: => JsonAssoc): Json =
    withObject(o => o + (k._1, k._2))

  /**
   * If this is a JSON object, and the association is set, then prepend the given value, otherwise, return a JSON object with only the given value.
   */
  def ->?:(o: => Option[JsonAssoc]): Json =
    o.map(->:(_)).getOrElse(this)

  /**
   * If this is a JSON array, then prepend the given value, otherwise, return a JSON array with only the given value.
   */
  def -->>:(k: => Json): Json =
    withArray(k :: _)

  /**
   * If this is a JSON array, and the element is set, then prepend the given value, otherwise, return a JSON array with only the given value.
   */
  def -->>?:(o: => Option[Json]): Json =
    o.map(j => withArray(j :: _)).getOrElse(this)

  /**
   * Alias for `field`.
   */
  def -|(f: => JsonField): Option[Json] =
    field(f)

  /**
   * Returns a possible JSON value after traversing through JSON object values using the given field names.
   */
  def -||(fs: List[JsonField]): Option[Json] =
    fs match {
      case Nil => None
      case h::t => t.foldLeft(field(h))((a, b) => a flatMap (_ -| b))
    }

  /**
   * Return `true` if this JSON value is a boolean with a value of `true`, otherwise, `false`.
   */
  def isTrue: Boolean =
    bool exists (z => z)

  /**
   * Return `true` if this JSON value is a boolean with a value of `false`, otherwise, `false`.
   */
  def isFalse: Boolean =
    bool exists (z => !z)

  /**
   * Return `true` if this JSON value is a boolean.
   */
  def isBool: Boolean =
    isTrue || isFalse

  /**
   * Return `true` if this JSON value is a number.
   */
  def isNumber: Boolean =
    number.isDefined

  /**
   * Return `true` if this JSON value is a string.
   */
  def isString: Boolean =
    string.isDefined

  /**
   * Return `true` if this JSON value is a array.
   */
  def isArray: Boolean =
    array.isDefined

  /**
   * Return `true` if this JSON value is a object.
   */
  def isObject: Boolean =
    obj.isDefined

  /**
   * Returns the number of this JSON value, or the given default if this JSON value is not a number.
   *
   * @param d The default number if this JSON value is not a number.
   */
  def numberOr(d: => JsonNumber): JsonNumber =
    number getOrElse d

  /**
   * Returns the string of this JSON value, or the given default if this JSON value is not a string.
   *
   * @param d The default string if this JSON value is not a string.
   */
  def stringOr(d: => JsonString): JsonString =
    string getOrElse d

  /**
   * Returns the array of this JSON value, or the given default if this JSON value is not an array.
   *
   * @param d The default array if this JSON value is not an array.
   */
  def arrayOr(d: => JsonArray): JsonArray =
    array getOrElse d

  /**
   * Returns the object of this JSON value, or the given default if this JSON value is not a object.
   *
   * @param d The default object if this JSON value is not an object.
   */
  def objectOr(d: => JsonObject): JsonObject =
    obj getOrElse d

  /**
   * Returns this JSON number object or the value `0` if it is not a number.
   */
  def numberOrZero: JsonNumber =
    numberOr(0D)

  /**
   * Returns the string of this JSON value, or an empty string if this JSON value is not a string.
   */
  def stringOrEmpty: JsonString =
    stringOr("")

  /**
   * Returns the array of this JSON value, or an empty array if this JSON value is not an array.
   */
  def arrayOrEmpty: JsonArray =
    arrayOr(Nil)

  /**
   * Returns the object of this JSON value, or the empty object if this JSON value is not an object.
   */
  def objectOrEmpty: JsonObject =
    objectOr(JsonObject.empty)

  /**
   * Returns the possible object map of this JSON value.
   */
  def objectMap: Option[JsonObjectMap] =
    obj map (_.toMap)

  /**
   * Returns the object map of this JSON value, or the given default if this JSON value is not an object.
   *
   * @param m The default object map if this JSON value is not an object.
   */
  def objectMapOr(m: => JsonObjectMap): JsonObjectMap =
    objectMap getOrElse m

  /**
   * Returns the object map of this JSON value, or the empty map if this JSON value is not an object.
   */
  def objectMapOrEmpty: JsonObjectMap =
    objectMapOr(InsertionMap.empty)

  /**
   * Return the object keys if this JSON value is an object, otherwise, return the empty list.
   */
  def objectFields: Option[List[JsonField]] =
    objectMap map (_.keys)

  /**
   * Returns the object map keys of this JSON value, or the given default if this JSON value is not an object.
   *
   * @param f The default object map keys if this JSON value is not an object.
   */
  def objectFieldsOr(f: => List[JsonField]): List[JsonField] =
    objectFields getOrElse f

  /**
   * Returns the object map keys of this JSON value, or the empty list if this JSON value is not an object.
   */
  def objectFieldsOrEmpty: List[JsonField] =
    objectFieldsOr(Nil)

  /**
   * Return the object values if this JSON value is an object, otherwise, return the empty list.
   */
  def objectValues: Option[List[Json]] =
    objectMap map (_.toList map (_._2))

  /**
   * Returns the object map values of this JSON value, or the given default if this JSON value is not an object.
   *
   * @param k The default object map values if this JSON value is not an object.
   */
  def objectValuesOr(k: => List[Json]): List[Json] =
    objectValues getOrElse k

  /**
   * Returns the object map values of this JSON value, or the empty list if this JSON value is not an object.
   */
  def objectValuesOrEmpty: List[Json] =
    objectValuesOr(Nil)

  /**
   * Returns `true` if this is a JSON object which has the given field, `false` otherwise.
   */
  def hasField(f: => JsonField): Boolean =
    objectMap exists (_ contains f)

  /**
   * Returns the possible value for the given JSON object field.
   */
  def field(f: => JsonField): Option[Json] =
    objectMap flatMap (_ get f)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns the default..
   */
  def fieldOr(f: => JsonField, j: => Json): Json =
    field(f) getOrElse j

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns a JSON `null`..
   */
  def fieldOrNull(f: => JsonField): Json =
    fieldOr(f, jNull)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns a JSON boolean with the value `true`.
   */
  def fieldOrTrue(f: => JsonField): Json =
    fieldOr(f, jTrue)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns a JSON boolean with the value `false`.
   */
  def fieldOrFalse(f: => JsonField): Json =
    fieldOr(f, jFalse)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns a JSON number with the value `0`.
   */
  def fieldOrZero(f: => JsonField): Json =
    fieldOr(f, jZero)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns an empty JSON.
   */
  def fieldOrEmptyString(f: => JsonField): Json =
    fieldOr(f, jEmptyString)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns an empty JSON array.
   */
  def fieldOrEmptyArray(f: => JsonField): Json =
    fieldOr(f, jEmptyArray)

  /**
   * Returns the value for the given JSON object field if this is an object with the given field, otherwise, returns an empty JSON object.
   */
  def fieldOrEmptyObject(f: => JsonField): Json =
    fieldOr(f, jEmptyObject)

  /**
   * The name of the type of the JSON value.
   */
  def name: String =
    this match {
      case JNull      => "Null"
      case JBool(_)   => "Boolean"
      case JNumber(_) => "Number"
      case JString(_) => "String"
      case JArray(_)  => "Array"
      case JObject(_) => "Object"
    }

  /**
   * Attempts to encode this JSON value to another data type.
   */
  def encode[A](implicit e: EncodeJson[A]): EncodeResult[A] =
    e(this)

  /**
   * Compute a `String` representation for this JSON value.
   */
  override def toString =
    "Json<" +
        fold(
          "null"
        , "bool(" + _ + ")"
        , "number(" + _ + ")"
        , "string(" + _ + ")"
        , "array" + _.shows
        , "object" + _.toList.shows
        ) + ">"

}
import Json._

private[argonaut] case object JNull extends Json
private[argonaut] case class JBool(b: Boolean) extends Json
private[argonaut] case class JNumber(n: JsonNumber) extends Json
private[argonaut] case class JString(s: String) extends Json
private[argonaut] case class JArray(a: JsonArray) extends Json
private[argonaut] case class JObject(o: JsonObject) extends Json

object Json extends Jsons

/**
 * Constructors and other utilities for JSON values.
 *
 * @author Tony Morris
 * @author Dylan Just
 * @author Mark Hibberd
 */
trait Jsons {
  type JsonNumber = Double
  type JsonArray = List[Json]
  type JsonString = String
  type JsonField = String
  type JsonAssoc = (JsonField, Json)
  type JsonObjectMap = scalaz.InsertionMap[JsonField, Json]

  import scalaz._, Scalaz._, PLens._, CostateT._

  /**
   * A partial lens for JSON boolean values.
   */
  def jBoolL: Json @?> Boolean =
    PLens(_.fold(None, z => Some(Costate(JBool, z)), _ => None, _ => None, _ => None, _ => None))

  /**
   * A partial lens for JSON number values.
   */
  def jNumberL: Json @?> JsonNumber =
    PLens(_.fold(None, _ => None, z => Some(Costate(JNumber, z)), _ => None, _ => None, _ => None))

  /**
   * A partial lens for JSON string values.
   */
  def jStringL: Json @?> JsonString =
    PLens(_.fold(None, _ => None, _ => None, z => Some(Costate(JString, z)), _ => None, _ => None))

  /**
   * A partial lens for JSON string values.
   */
  def jArrayL: Json @?> JsonArray =
    PLens(_.fold(None, _ => None, _ => None, _ => None, z => Some(Costate(JArray, z)), _ => None))

  /**
   * A partial lens for JSON string values.
   */
  def jObjectL: Json @?> JsonObject =
    PLens(_.fold(None, _ => None, _ => None, _ => None, _ => None, z => Some(Costate(JObject, z))))

  /**
   * Construct a JSON value that is `null`.
   */
  val jNull: Json =
    JNull

  /**
   * Construct a JSON value that is a boolean.
   */
  val jBool: Boolean => Json =
    JBool(_)

  /**
   * Construct a JSON value that is a number.
   */
  val jNumber: JsonNumber => Json =
    JNumber(_)

  /**
   * Construct a JSON value that is a string.
   */
  val jString: JsonString => Json =
    JString(_)

  /**
   * Construct a JSON value that is an array.
   */
  val jArray: JsonArray => Json =
    JArray(_)

  /**
   * Construct a JSON value that is an object.
   */
  val jObject: JsonObject => Json =
    JObject(_)

  /**
   * Construct a JSON boolean value of `true`.
   */
  val jTrue: Json =
    JBool(true)

  /**
   * Construct a JSON boolean value of `false`.
   */
  val jFalse: Json =
    JBool(false)

  /**
   * A JSON value that is a zero number.
   */
  val jZero: Json =
    JNumber(0D)

  /**
   * A JSON value that is an empty string.
   */
  val jEmptyString: Json =
    JString("")

  /**
   * A JSON value that is an empty array.
   */
  val jEmptyArray: Json =
    JArray(Nil)

  /**
   * Returns a function that takes a single value and produces a JSON array that contains only that value.
   */
  def jSingleArray(j: Json): Json =
    JArray(List(j))

  /**
   * A JSON value that is an empty object.
   */
  val jEmptyObject: Json =
    JObject(JsonObject.empty)

  /**
   * Returns a function that takes an association value and produces a JSON object that contains only that value.
   */
  def jSingleObject(k: JsonField, v: Json): Json =
    JObject(JsonObject.single(k, v))

  /**
   * Construct a JSON value that is an object from an index.
   */
  def jObjectMap(x: JsonObjectMap): Json =
    JObject(JsonObject(x))

  /**
   * Construct a JSON value that is an object from an association list.
   */
  def jObjectAssocList(x: List[(JsonField, Json)]): Json =
    JObject(JsonObject(InsertionMap(x: _*)))

  import JsonIdentity._

  implicit def JsonInstances: Equal[Json] with Show[Json] =
    new Equal[Json] with Show[Json] {
      def equal(a1: Json, a2: Json) =
        a1 match {
              case JNull      => a2.isNull
              case JBool(b)   => a2.bool exists (_ == b)
              case JNumber(n) => a2.number exists (_ == n)
              case JString(s) => a2.string exists (_ == s)
              case JArray(a)  => a2.array exists (_ === a)
              case JObject(o) => a2.obj exists (_ === o)
            }

      def show(a: Json) = Show.showFromToString show a
    }
}
