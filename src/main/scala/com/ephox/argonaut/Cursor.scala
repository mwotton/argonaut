package com.ephox
package argonaut

import scalaz._, Scalaz._
import Json._
import ContextElement._

sealed trait Cursor {
  /** Return the current context of the focus. */
  def context: Context =
    this match {
      case CJson(_) => Context.empty
      case CArray(p, _, l, j, _) => arrayC(l.length, j) +: p.context
      case CObject(p, _, _, (f, j)) => objectC(f, j) +: p.context
    }

  /** Set the focus to the given value. */
  def focus: Json =
    this match {
      case CJson(j) => j
      case CArray(_, _, _, j, _) => j
      case CObject(_, _, _, (_, j)) => j
    }

  /** Update the focus with the given function (alias for `withFocus`). */
  def >->(k: Json => Json): Cursor =
    withFocus(k)

  /** Update the focus with the given function (alias for `>->`). */
  def withFocus(k: Json => Json): Cursor =
    this match {
      case CJson(j) =>
        CJson(k(j))
      case CArray(p, _, l, j, r) =>
        CArray(p, true, l, k(j), r)
      case CObject(p, _, x, (f, j)) =>
        CObject(p, true, x, (f, k(j)))
    }

  /** Set the focus to the given value (alias for `:=`). */
  def set(j: Json): Cursor =
    withFocus(_ => j)

  /** Set the focus to the given value (alias for `set`). */
  def :=(j: Json): Cursor =
    set(j)

  /** Move the cursor left in a JSON array. */
  def left: Option[Cursor] =
    this match {
      case CArray(p, u, l, j, r) => l match {
        case Nil => None
        case h::t => Some(CArray(p, u, t, h, j::r))
      }
      case _ => None
    }

  /** Move the cursor right in a JSON array. */
  def right: Option[Cursor] =
    this match {
      case CArray(p, u, l, j, r) => r match {
        case Nil => None
        case h::t => Some(CArray(p, u, j::l, h, t))
      }
      case _ => None
    }
  
  /** Move the cursor to the first in a JSON array. */
  def first: Option[Cursor] =
    this match {
      case CArray(p, u, l, j, r) => {
        val h::t = l.reverse ::: j :: r
        Some(CArray(p, u, Nil, h, t))
      }
      case _ => None
    }

  /** Move the cursor to the last in a JSON array. */
  def last: Option[Cursor] =
    this match {
      case CArray(p, u, l, x, r) => {
        val h::t = r.reverse ::: x :: l
        Some(CArray(p, u, t, h, Nil))
      }
      case _ => None
    }

  /** Move the cursor left in a JSON array the given number of times. A negative value will move the cursor right. */
  def -<-:(n: Int): Option[Cursor] =
    if(n < 0)
      :->-(-n)
    else {
      @annotation.tailrec
      def r(x: Int, c: Option[Cursor]): Option[Cursor] =
        if (x == 0)
          c
        else
          r(x - 1, c flatMap (_.left))
      r(n, Some(this))
    }

  /** Move the cursor right in a JSON array the given number of times. A negative value will move the cursor left. */
  def :->-(n: Int): Option[Cursor] =
    if(n > 0)
      -<-:(-n)
    else {
      @annotation.tailrec
      def r(x: Int, c: Option[Cursor]): Option[Cursor] =
        if (x == 0)
          c
        else
          r(x + 1, c flatMap (_.right))
      r(n, Some(this))
    }

  /** Move the cursor left in a JSON array until the given predicate matches the focus. */
  def ?<-:(p: Json => Boolean): Option[Cursor] = {
    @annotation.tailrec
    def r(c: Option[Cursor]): Option[Cursor] =
      c match {
        case None => None
        case Some(w) => r(if(p(w.focus)) Some(w) else w.left)
      }

    r(left)
  }

  /** Move the cursor right in a JSON array until the given predicate matches the focus. */
  def :->?(p: Json => Boolean): Option[Cursor] = {
    @annotation.tailrec
    def r(c: Option[Cursor]): Option[Cursor] =
      c match {
        case None => None
        case Some(w) => r(if(p(w.focus)) Some(w) else w.right)
      }

    r(right)
  }
  
  /** Move the cursor to the given sibling key in a JSON object */
  def --(q: JsonField): Option[Cursor] =
    this match {
      case CObject(p, u, o, (f, j)) =>
        o(q) map (jj => CObject(p, u, o, (q, jj)))
      case _ => None
    }

  /** Move the cursor down to a JSON object at the given field. */
  def --\(q: JsonField): Option[Cursor] =
    focus.obj flatMap (o => o(q) map (jj => CObject(this, false, o, (q, jj))))

  /** Move the cursor down to a JSON array at the first element. */
  def downArray: Option[Cursor] =
    focus.array flatMap (_ match {
      case Nil => None
      case h::t => Some(CArray(this, false, Nil, h, t))
    })

  /** Move the cursor down to a JSON array at the first element satisfying the given predicate. */
  def -\(p: Json => Boolean): Option[Cursor] =
    downArray flatMap (_ :->? p)

  /** Move the cursor down to a JSON array at the given index. */
  def =\(n: Int): Option[Cursor] =
    downArray flatMap (_ :->- n)

  /** Deletes the JSON value at focus and moves up to parent (alias for `deleteGoParent`). */
  def unary_! : Option[Cursor] =
    deleteGoParent

  /** Deletes the JSON value at focus and moves up to parent (alias for `unary_!`). */
  def deleteGoParent: Option[Cursor] =
    this match {
      case CJson(_) =>
        None
      case CArray(p, _, l, _, r) => {
        val q = jArray(l.reverse ::: r)
        Some(p match {
          case CJson(_) =>
            CJson(q)
          case CArray(pp, _, l1, _, r1) =>
            CArray(pp, true, l1, q, r1)
          case CObject(pp, _, oo, (ff, _)) =>
            CObject(pp, true, oo, (ff, q))
        })
      }
      case CObject(p, _, o, (f, _)) => {
        val q = jObject(o - f)
        Some(p match {
          case CJson(_) =>
            CJson(q)
          case CArray(pp, _, l1, _, r1) =>
            CArray(pp, true, l1, q, r1)
          case CObject(pp, _, oo, (ff, _)) =>
            CObject(pp, true, oo, (ff, q))
        })
      }
    }

  /** Deletes the JSON value at focus and moves to the left in a JSON array. */
  def deleteGoLeft: Option[Cursor] =
    this match {
      case CArray(p, _, l, _, r) => l match {
        case Nil => None
        case h::t => Some(CArray(p, true, t, h, r))
      }
      case _ => None
    }

  /** Deletes the JSON value at focus and moves to the right in a JSON array. */
  def deleteGoRight: Option[Cursor] =
    this match {
      case CArray(p, _, l, _, r) => r match {
        case Nil => None
        case h::t => Some(CArray(p, true, l, h, t))
      }
      case _ => None
    }

  /** Deletes the JSON value at focus and moves to the first in a JSON array. */
  def deleteGoFirst: Option[Cursor] =
    this match {
      case CArray(p, _, l, _, r) => {
        val h::t = l.reverse ::: r
        Some(CArray(p, true, Nil, h, t))
      }
      case _ => None
    }

  /** Deletes the JSON value at focus and moves to the last in a JSON array. */
  def deleteGoLast: Option[Cursor] =
    this match {
      case CArray(p, _, l, _, r) => {
        val h::t = r.reverse ::: l
        Some(CArray(p, true, t, h, Nil))
      }
      case _ => None
    }

  /** Deletes all JSON values to left of focus in a JSON array. */
  def deleteLefts: Option[Cursor] =
    this match {
      case CArray(p, _, _, j, r) =>
        Some(CArray(p, true, Nil, j, r))
      case _ =>
        None
    }

  /** Deletes all JSON values to left of focus in a JSON array. */
  def deleteRights: Option[Cursor] =
    this match {
      case CArray(p, _, l, j, _) =>
        Some(CArray(p, true, l, j, Nil))
      case _ =>
        None
    }

  /** Move the cursor to the given sibling key in a JSON object */
  def deleteGoField(q: JsonField): Option[Cursor] =
    this match {
      case CObject(p, _, o, (f, _)) =>
        o(q) map (jj => CObject(p, true, o - f, (q, jj)))
      case _ => None
    }

  /** Delete the given field in a JSON object */
  def deleteField(q: JsonField): Cursor =
    this match {
      case CObject(p, _, o, (f, j)) =>
        CObject(p, true, o - q, (f, j))
      case _ => this
    }

  /** Move the cursor up one step to the parent context. */
  def up: Option[Cursor] =
    this match {
      case CJson(_) =>
        None
      case CArray(p, u, l, j, r) => {
        val q = jArray(l.reverse ::: j :: r)
        Some(p match {
          case CJson(_) =>
            CJson(q)
          case CArray(pp, v, l1, _, r1) =>
            CArray(pp, u || v, l1, q, r1)
          case CObject(pp, v, oo, (ff, _)) =>
            CObject(pp, u || v, if(u) oo + (ff, q) else oo, (ff, q))
        })
      }
      case CObject(p, u, o, (f, j)) => {
        val q = jObject(if(u) o + (f, j) else o)
        Some(p match {
          case CJson(_) =>
            CJson(q)
          case CArray(pp, v, l1, _, r1) =>
            CArray(pp, u || v, l1, q, r1)
          case CObject(pp, v, oo, (ff, _)) =>
            CObject(pp, u || v, oo, (ff, q))
        })
      }
    }

  /** Unapplies the cursor to the top-level parent. */
  def unary_- : Json = {
    @annotation.tailrec
    def goup(c: Cursor): Json =
      c match {
        case CJson(j) =>
          j
        case CArray(p, u, l, j, r) => {
          val q = jArray(l.reverse ::: j :: r)
          goup(p match {
              case CJson(_) =>
                CJson(q)
              case CArray(pp, v, l1, _, r1) =>
                CArray(pp, u || v, l1, q, r1)
              case CObject(pp, v, oo, (ff, _)) =>
                CObject(pp, u || v, if(u) oo + (ff, q) else oo, (ff, q))
            })
        }
        case CObject(p, u, o, (f, j)) => {
          val q = jObject(if(u) o + (f, j) else o)
          goup(p match {
            case CJson(_) =>
              CJson(q)
            case CArray(pp, v, l1, _, r1) =>
              CArray(pp, u || v, l1, q, r1)
            case CObject(pp, v, oo, (ff, _)) =>
              CObject(pp, u || v, oo, (ff, q))
          })
        }
      }
    goup(this)
  }
}
private case class CJson(j: Json) extends Cursor
private case class CArray(p: Cursor, u: Boolean, ls: List[Json], x: Json, rs: List[Json]) extends Cursor
private case class CObject(p: Cursor, u: Boolean, o: JsonObject, x: (JsonField, Json)) extends Cursor

object Cursor extends Cursors {
  def apply(j: Json): Cursor =
    CJson(j)
}

trait Cursors {
  val focusL: Cursor @> Json =
    Lens {
      case CJson(j) =>
        Costate(CJson(_), j)
      case CArray(p, _, l, j, r) =>
        Costate(CArray(p, true, l, _, r), j)
      case CObject(p, _, x, (f, j)) =>
        Costate(jj => CObject(p, true, x, (f, jj)), j)
    }

  implicit val CursorInstances: Equal[Cursor] with Show[Cursor] = new Equal[Cursor] with Show[Cursor] {
    def equal(c1: Cursor, c2: Cursor) =
      c1 match {
       case CJson(j1) =>
         c2 match {
           case CJson(j2) => j1 === j2
           case _ => false
         }
       case CArray(p1, u1, l1, j1, r1) =>
         c2 match {
           case CArray(p2, u2, l2, j2, r2) => p1 === p2 && u1 === u2 && l1 === l2 && j1 === j2 && r1 === r2
           case _ => false
         }
       case CObject(p1, u1, x1, (f1, j1)) =>
         c2 match {
           case CObject(p2, u2, x2, (f2, j2)) => p1 === p2 && u1 === u2 && x1 === x2 && f1 === f2 && j1 === j2
           case _ => false
         }
     }

    def show(c: Cursor) =
      List(c.context.show, " ==> ".toList, c.focus.show).join
  }
}
