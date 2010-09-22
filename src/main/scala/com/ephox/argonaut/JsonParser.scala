package com.ephox.argonaut

import util.parsing.combinator._
import Json._

class JsonParser extends Parsers {
  type Elem = Char

  def jnull = acceptSeq("null") ^^^ jsonNull
  def jboolean = (f | t) ^^ jsonBool
  def f = acceptSeq("false") ^^^ false
  def t = acceptSeq("true") ^^^ true


  // FIX Pull out lexer, so the underlying constructs are easier  to work with (spaces in particular)

//  def jobject: Parser[Json] = acceptSeq("{}") ||| ('{' ~ members ~ '}')
//  def members = pair ||| (pair ~ ',' ~ members)
//  def pair = string ~ ':' ~ value
//  def array: Parser[Json] = acceptSeq("[]") ||| ('[' ~ elements ~ ']')
//  def elements = value ||| (value ~ ',' ~ elements)
//  def value = jstring  ||| jboolean ||| jnull // ||| array ||| jobject

  def jstring = string ^^ jsonString

  def string = '"' ~> (char*) <~ '"' ^^ {_.toString}

  def char = basicchar ||| escapedchar

  def basicchar  = elem("char", (c: Char) => c != '"' && c != '\\')

  def escapedchar  = '\\' ~> escapecode ^^ {(c: Char) => c}

  def escapecode = '"' ||| '\\' ||| '/' ||| 'b' ||| 'f' ||| 'n' ||| 'r' ||| 't' ||| unicode

  def unicode: Parser[Char]  = ('u' ~ repN(4, hex)) ^^^ '?' // FIX evaluate....

  def hex = digit0to9 ||| 'a' ||| 'b' ||| 'c' ||| 'd' ||| 'e' ||| 'f'

  def number = int | intfrac | intexp | intfracexp ^^ {_.toString.toDouble}

  def intexp = int ~ exp ^^ {case a ~ b => a ++ b}

  def intfracexp = int ~ frac ~ exp ^^ {case a ~ b ~ c => a ++ b ++ c}

  def intfrac = int ~ frac ^^ {case a ~ b => a ++ b}

  def exp = e ~ digits ^^ {case a ~ b => a ++ b}

  def frac = '.' ~ digits ^^ {case a ~ b => a :: b}

  def int = sign ~ (digitSeries | digitSingle) ^^ {
    case Some(s) ~ a => s :: a
    case None ~ a => a
  }

  def sign = ('-'?)

  def digits = digit0to9+

  def digitSeries: Parser[List[Char]] = digit1to9 ~ (digit0to9*) ^^ {case a ~ b => a :: b}

  def digitSingle: Parser[List[Char]] = digit0to9 map (List(_))

  def digit0to9: Parser[Char] = '0' ||| digit1to9

  def digit1to9: Parser[Char] = ('1' to '9' toList) map accept reduceRight (_ | _)

  def e = oneOf(List("e", "e+", "e-", "E", "E+", "E-"))

  def oneOf[ES](seqs: Iterable[ES])(implicit e : ES => Iterable[Elem]) = seqs map (acceptSeq[ES] _) reduceRight(_ ||| _)
}
