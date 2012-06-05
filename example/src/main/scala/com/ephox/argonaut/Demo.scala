package com.ephox.argonaut

import com.ephox.argonaut._, Argonaut._

object Demo {
/*
  def d(j: PossibleJson) {
    import Json._

    // searches down through objects with the keys and obtains the number value at that level
    val a: Option[JsonNumber] = j -| "abc" -| "def" number

    // searches down through objects with the list of keys and returns the JSON string value or the empty string
    val b: JsonString = j -|| List("ghi", "jkl", "mno") stringOrEmpty

    // If it is a JSON object a "pqr" field
    val c: Boolean = j hasField "pqr"

    // If it is a number, add ten to it
    val d: PossibleJson = j withNumber (10+)

    // If it is a JSON array, return it, or default to List(jNull, jTrue)
    val e: JsonArray = j arrayOr (List(jNull[Json], jTrue[Json]))

    // If it is a JSON object, with a field "xyz" that is a JSON number, return it, otherwise, default to 42
    val f: JsonNumber = j -| "xyz" numberOr 42D

    // If it is a JSON object, prepend the given key/value pairs to it
    val g: PossibleJson = ("k1", jString[Json]("v1")) ->: ("k2", jTrue[Json]) ->: j

    // If it is a JSON array, prepend the given JSON values to it
    val h: PossibleJson = jFalse[Json] -->>: jString[Json]("boo") -->>: j

    List(("a", a), ("b", b), ("c", c), ("d", d), ("e", e), ("f", f), ("g", g), ("h", h)).
        foreach { case (x, y) => println(x + " : " + y) }
    
    println
  }
*/
  def demo(j: List[String]) =
    j map (_.pparse)

  def main(args: Array[String]) {
    val j =
      """
        {
          "abc" :
            {
              "def" : 7
            },
          "ghi" :
            {
              "ata" : null,
              "jkl" :
                {
                  "mno" : "argo"
                }
            },
          "pqr" : false,
          "operator": "is",
          "values": [
                      ["cat", "lol"]
                    , "dog"
                    , "rabbit"
                    ],
          "xyz" : 24
        }
      """

    /*

     1. parse
     2. create cursor                        CJson(None, jObject(...))
     3. down to "ghi"                        CObject((None, jObject(...)), ("ghi", jObject(...))
     4. set focus to false                   CObject((None, jObject(...)), ("ghi", jBoolean(false))
     5. release cursor                       CJson(None, jObject(...))



     */
    val r =
      j.pparse
    val c =
      r flatMap (k => {
        val k2 = +k // create cursor
        val k3 = k2 --\ "values"
        val k33 = k3 flatMap (_.downArray)
        val k4 = k33 map (_ := jBool(false))
        val k5 = k4 map (-_)
        k5
        /*
        // +k --\ "ghi" map (_ := jBool(false)) map (-_)
        val rr = +k --\ "ghi" map (_ := jBool(false)) map (-_)
        println("rr: " + rr)
        rr
        */
      })

    println(c map (JsonPrinter.pretty(_)))

    /*
    val jsons = List(
      "true"
    , "[ true ]"
    , "8"
    , """[ "chook1", "chook2" ]"""
    , """{ "chook1" : "chook2" }"""
    , """
      {
        "abc" :
          {
            "def" : 7
          },
        "ghi" :
          {
            "ata" : null,
            "jkl" :
              {
                "mno" : "argo"
              }
          },
        "pqr" : false,
        "operator": "is",
        "values": ["cat", "dog", "rabbit"],
        "xyz" : 24
      }
      """
    )

    println(demo(jsons))
    */
  }
}
