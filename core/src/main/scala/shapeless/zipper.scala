/*
 * Copyright (c) 2012-16 Miles Sabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shapeless

/**
 * Generic Zipper for any type with a representation via `Generic`.
 *
 * @author Miles Sabin
 */
case class Zipper[C, L <: HList, R <: HList, P](prefix : L, suffix : R, parent : P) {
  import ops.zipper._

  type Self = Zipper[C, L, R, P]

  /** Move the cursor one place to the right. Available only if not already at the rightmost element. */
  def right(implicit right : Right[Zipper[C, L, R, P]]) : right.Out = right(this)

  /** Move the cursor one place to the left. Available only if not already at the leftmost element. */
  def left(implicit left : Left[Zipper[C, L, R, P]]) : left.Out = left(this)

  /** Moves the cursor to the leftmost position. */
  def first(implicit first : First[Zipper[C, L, R, P]]) : first.Out = first(this)

  /** Moves the cursor to the rightmost position. */
  def last(implicit last : Last[Zipper[C, L, R, P]]) : last.Out = last(this)

  /** Move the cursor ''n'' places to the right. Requires an explicit type argument. Available only if there are
   * ''n'' places to the right of the cursor. */
  def rightBy[N <: Nat](implicit rightBy : RightBy[Zipper[C, L, R, P], N]) = rightBy(this)

  /** Move the cursor ''n'' places to the right. Available only if there are ''n'' places to the right of the cursor. */
  def rightBy(n: Nat)(implicit rightBy : RightBy[Zipper[C, L, R, P], n.N]) = rightBy(this)

  /** Move the cursor ''n'' places to the left. Requires an explicit type argument. Available only if there are
   * ''n'' places to the left of the cursor. */
  def leftBy[N <: Nat](implicit leftBy : LeftBy[Zipper[C, L, R, P], N]) = leftBy(this)

  /** Move the cursor ''n'' places to the left. Available only if there are ''n'' places to the right of the cursor. */
  def leftBy(n: Nat)(implicit leftBy : LeftBy[Zipper[C, L, R, P], n.N]) = leftBy(this)

  /** Move the cursor to the first element of type `T` to the right. Available only if there is an element of type `T`
   * to the right of the cursor.
   */
  def rightTo[T](implicit rightTo : RightTo[Zipper[C, L, R, P], T]) = rightTo(this)

  /** Move the cursor to the first element of type `T` to the left. Available only if there is an element of type `T`
   * to the left of the cursor.
   */
  def leftTo[T](implicit leftTo : LeftTo[Zipper[C, L, R, P], T]) = leftTo(this)

  /** Moves the cursor up to the next level. The element at the new cursor position will be updated with the
   * reification of the current level.
   */
  def up(implicit up : Up[Zipper[C, L, R, P]]) : up.Out = up(this)

  /** Moves the cursor down to the next level, placing it at the first element on the left. Available only if the
   * element current at the cursor has a representation via `Generic`.
   */
  def down(implicit down : Down[Zipper[C, L, R, P]]) : down.Out = down(this)

  /** Moves the cursor to root of this Zipper. */
  def root(implicit root : Root[Zipper[C, L, R, P]]) : root.Out = root(this)

  /** Returns the element at the cursor. Available only if the underlying `HList` is non-empty. */
  def get(implicit get : Get[Zipper[C, L, R, P]]) : get.Out = get(this)

  /** Replaces the element at the cursor. Available only if the underlying `HList` is non-empty. */
  def put[E](e : E)(implicit put : Put[Zipper[C, L, R, P], E]) : put.Out = put(this, e)

  /** Inserts a new element to the left of the cursor. */
  def insert[E](e : E)(implicit insert : Insert[Zipper[C, L, R, P], E]) : insert.Out = insert(this, e)

  /** Removes the element at the cursor. Available only if the underlying `HList` is non-empty. */
  def delete(implicit delete : Delete[Zipper[C, L, R, P]]) : delete.Out = delete(this)

  /** Reifies the current level of this `Zipper`. */
  def reify(implicit reify : Reify[Zipper[C, L, R, P]]) : reify.Out = reify(this)
}

object Zipper {
  def apply[C, CL <: HList](c : C)(implicit gen : Generic.Aux[C, CL]) : Zipper[C, HNil, CL, None.type] =
    Zipper[C, HNil, CL, None.type](HNil, gen.to(c), None)

  def apply[L <: HList](l : L) : Zipper[L, HNil, L, None.type] = Zipper[L, HNil, L, None.type](HNil, l, None)

  implicit class Modifier[C, L <: HList, RH, RT <: HList, P](val zipper: Zipper[C, L, RH :: RT, P]) extends AnyVal {
    import ops.zipper._
    /** Modifies the element at the cursor by the use of function f. Available only if the underlying `HList` is non-empty. */
    def modify[E](f: RH => E)(implicit modify: Modify[Zipper[C, L, RH :: RT, P], RH, E]): modify.Out = modify(zipper, f)
  }
}
