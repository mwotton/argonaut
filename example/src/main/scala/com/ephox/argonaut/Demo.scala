package com.ephox.argonaut

import com.ephox.argonaut._, Argonaut._

object Demo {
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

    val c =
      j.pparse flatMap (k =>
        +k --\ "values" flatMap (_.downArray) map (_ := jBool(false)) map (-_)
      )

    println(c map (JsonPrinter.pretty(_)))
  }
}
