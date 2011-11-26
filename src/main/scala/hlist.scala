import PolyFun._
import Castable._
import Nat._

// TODO zip/unzip
// TODO hApply
// TODO Value/type contains
// TODO take/drop
// TODO Lenses
// TODO Unified comprehensions
// TODO http://stackoverflow.com/questions/8270526

sealed trait HList

final case class HCons[+H, +T <: HList](head : H, tail : T) extends HList {
  override def toString = head+" :: "+tail.toString
}

trait HNil extends HList {
  def ::[H](h : H) = HCons(h, this)
  override def toString = "HNil"
}

case object HNil extends HNil

trait LowPriorityHList {
  type ::[+H, +T <: HList] = HCons[H, T]

  final class Ops[L <: HList](l : L) {
    def head(implicit c : IsHCons[L]) : c.H = c.head(l) 

    def tail(implicit c : IsHCons[L]) : c.T = c.tail(l)
    
    def ::[H](h : H) : H :: L = HCons(h, l)

    def :::[P <: HList](prefix : P)(implicit prepend : Prepend[P, L]) : prepend.Out = prepend(prefix, l)
    
    def apply[N <: Nat](implicit at : At[L, N]) : at.Out = at(l)

    def apply[N <: Nat](n : N)(implicit at : At[L, N]) : at.Out = at(l)
    
    def reverse_:::[P <: HList](prefix : P)(implicit prepend : ReversePrepend[P, L]) : prepend.Out = prepend(prefix, l)

    def last(implicit last : Last[L]) : last.Out = last(l)

    def init(implicit init : Init[L]) : init.Out = init(l)
    
    def select[U](implicit selector : Selector[L, U]) : U = selector(l)
    
    def split[N <: Nat](implicit split : Split[L, N]) : split.R = split(l)

    def split[N <: Nat](n : N)(implicit split : Split[L, N]) : split.R = split(l)

    def reverse_split[N <: Nat](implicit split : ReverseSplit[L, N]) : split.R = split(l)

    def reverse_split[N <: Nat](n : N)(implicit split : ReverseSplit[L, N]) : split.R = split(l)

    def splitLeft[U](implicit splitLeft : SplitLeft[L, U]) : splitLeft.R = splitLeft(l)

    def reverse_splitLeft[U](implicit splitLeft : ReverseSplitLeft[L, U]) : splitLeft.R = splitLeft(l)

    def splitRight[U](implicit splitRight : SplitRight[L, U]) : splitRight.R = splitRight(l)

    def reverse_splitRight[U](implicit splitRight : ReverseSplitRight[L, U]) : splitRight.R = splitRight(l)

    def reverse(implicit reverse : Reverse[L]) : reverse.Out = reverse(l)

    def map[HF <: HRFn](f : HF)(implicit mapper : Mapper[HF, L]) : mapper.Out = mapper(l)
    
    def foldLeft[R, HF <: HRFn](z : R)(f : HF)(op : (R, R) => R)(implicit folder : LeftFolder[L, R, HF]) : R = folder(l, z, op)

    def unify(implicit unifier : Unifier[L]) : unifier.Out = unifier(l)
  
    def toList[Lub](implicit toList : ToList[L, Lub]) : List[Lub] = toList(l)

    def cast[M <: HList](implicit cast : Cast[L, M]) : Option[M] = cast(l)
  }

  implicit def hlistOps[L <: HList](l : L) = new Ops(l)
  
  trait IsHCons[L <: HList] {
    type H
    type T <: HList
      
    def head(l : L) : H
    def tail(l : L) : T
  }

  implicit def hlistIsHCons[H0, T0 <: HList] = new IsHCons[H0 :: T0] {
    type H = H0
    type T = T0

    def head(l : H0 :: T0) : H = l.head
    def tail(l : H0 :: T0) : T = l.tail
  }

  trait Mapper[HF <: HRFn, In <: HList] {
    type Out <: HList
    def apply(in: In) : Out
  }

  implicit def mapper[HF <: HRFn, In <: HList, Out0 <: HList](implicit mapper : Mapper0[HF, In, Out0]) = new Mapper[HF, In] {
    type Out = Out0
    def apply(in: In) : Out = mapper(in)
  }

  type MapperAux[HF <: HRFn, In <: HList, Out <: HList] = Mapper0[HF, In, Out]
  
  trait Mapper0[HF <: HRFn, In <: HList, Out <: HList] {
    def apply(in: In) : Out
  }

  implicit def hnilMapper1[HF <: HRFn] = new Mapper0[HF, HNil, HNil] {
    def apply(l : HNil) = HNil
  }
  
  implicit def hlistMapper1[HF <: HRFn, InH, OutH, InT <: HList, OutT <: HList]
    (implicit hc : Case[HF, InH => OutH], mt : Mapper0[HF, InT, OutT]) = new Mapper0[HF, InH :: InT, OutH :: OutT] {
      def apply(l : InH :: InT) = hc.f(l.head) :: mt(l.tail)
  }
  
  trait LeftFolder[L <: HList, R, HF <: HRFn] {
    def apply(l : L, in : R, op : (R, R) => R) : R 
  }
  
  implicit def hnilLeftFolder[R, HF <: HRFn] = new LeftFolder[HNil, R, HF] {
    def apply(l : HNil, in : R, op : (R, R) => R) = in
  }
  
  implicit def hlistLeftFolder[H, T <: HList, R, HF <: HRFn](implicit hc : Case[HF, H => R], tf : LeftFolder[T, R, HF]) = new LeftFolder[H :: T, R, HF] {
    def apply(l : H :: T, in : R, op : (R, R) => R) = tf(l.tail, op(in, hc.f(l.head)), op)
  }
  
  trait Lub[-A, -B, +Out] {
    def left(a : A) : Out
    def right(b : B) : Out
  }
  
  implicit def lub[T] = new Lub[T, T, T] {
    def left(a : T) : T = a
    def right(b : T) : T = b
  }
  
  trait Unifier[L <: HList] {
    type Out
    def apply(l : L) : Out
  }

  implicit def unifier[L <: HList, Out0 <: HList](implicit unifier : Unifier0[L, Out0]) = new Unifier[L] {
    type Out = Out0
    def apply(l : L) : Out = unifier(l)
  }

  type UnifierAux[L <: HList, Out <: HList] = Unifier0[L, Out]
  
  trait Unifier0[L <: HList, Out <: HList] {
    def apply(l : L) : Out
  }
  
  implicit def hsingleUnifier[T] = new Unifier0[T :: HNil, T :: HNil] {
    def apply(l : T :: HNil) = l
  }
  
  implicit def hlistUnifier[H1, H2, L, T <: HList, Out <: HList](implicit u : Lub[H1, H2, L], lt : Unifier0[L :: T, L :: Out]) = new Unifier0[H1 :: H2 :: T, L :: L :: Out] {
    def apply(l : H1 :: H2 :: T) : L :: L :: Out = u.left(l.head) :: lt(u.right(l.tail.head) :: l.tail.tail)
  }

  trait ToList[L <: HList, +Lub] {
    def apply(l : L) : List[Lub]
  }
  
  implicit def hsingleToList[T] : ToList[T :: HNil, T] = new ToList[T :: HNil, T] {
    def apply(l : T :: HNil) = List(l.head)
  }
  
  implicit def hlistToList[H1, H2, T <: HList, L](implicit u : Lub[H1, H2, L], ttl : ToList[H2 :: T, L]) = new ToList[H1 :: H2 :: T, L] {
    def apply(l : H1 :: H2 :: T) = u.left(l.head) :: ttl(l.tail)
  }
  
  trait Last[L <: HList] {
    type Out
    def apply(l : L) : Out
  }

  implicit def last[L <: HList, Out0](implicit last : Last0[L, Out0]) = new Last[L] {
    type Out = Out0
    def apply(l : L) : Out = last(l)
  }

  type LastAux[L <: HList, Out] = Last0[L, Out]
  
  trait Last0[L <: HList, Out] {
    def apply(l : L) : Out
  }
  
  implicit def hsingleLast[H] = new Last0[H :: HNil, H] {
    def apply(l : H :: HNil) : H = l.head
  }
  
  implicit def hlistLast[H, T <: HList, Out](implicit lt : Last0[T, Out]) = new Last0[H :: T, Out] {
    def apply(l : H :: T) : Out = lt(l.tail) 
  }

  trait Init[L <: HList] {
    type Out <: HList
    def apply(l : L) : Out
  }

  implicit def init[L <: HList, Out0 <: HList](implicit init : Init0[L, Out0]) = new Init[L] {
    type Out = Out0
    def apply(l : L) : Out = init(l)
  }

  type InitAux[L <: HList, Out <: HList] = Init0[L, Out]
  
  trait Init0[L <: HList, Out <: HList] {
    def apply(l : L) : Out
  }
  
  implicit def hsingleInit[H] = new Init0[H :: HNil, HNil] {
    def apply(l : H :: HNil) : HNil = HNil
  }
  
  implicit def hlistInit[H, T <: HList, OutH, OutT <: HList](implicit it : Init0[T, OutT]) = new Init0[H :: T, H :: OutT] {
    def apply(l : H :: T) : H :: OutT = l.head :: it(l.tail)
  }
  
  trait Selector[L <: HList, U] {
    def apply(l : L) : U
  }

  implicit def hlistSelect1[H, T <: HList] = new Selector[H :: T, H] {
    def apply(l : H :: T) = l.head
  }

  implicit def hlistSelect[H, T <: HList, U](implicit st : Selector[T, U]) = new Selector[H :: T, U] {
    def apply(l : H :: T) = st(l.tail)
  }
  
  trait At[L <: HList, N <: Nat] {
    type Out
    def apply(l : L) : Out 
  }
  
  implicit def at[L <: HList, N <: Nat, Out0](implicit at : At0[L, N, Out0]) = new At[L, N] {
    type Out = Out0
    def apply(l : L) : Out = at(l)
  }
  
  type AtAux[L <: HList, N <: Nat, Out] = At0[L, N, Out]
  
  trait At0[L <: HList, N <: Nat, Out] {
    def apply(l : L) : Out
  }
  
  implicit def hlistAtZero[H, T <: HList] = new At0[H :: T, _0, H] {
    def apply(l : H :: T) : H = l.head
  }
  
  implicit def hlistAtN[H, T <: HList, N <: Nat, Out](implicit att : At0[T, N, Out]) = new At0[H :: T, Succ[N], Out] {
    def apply(l : H :: T) : Out = att(l.tail) 
  }
  
  trait Split[L <: HList, N <: Nat] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def split[L <: HList, N <: Nat, P0 <: HList, S0 <: HList]
    (implicit split : Split0[HNil, L, N, P0, S0]) = new Split[L, N] {
    type P = P0
    type S = S0
    def apply(l : L) : R = split(HNil, l)
  }
  
  type SplitAux[L <: HList, N <: Nat, P <: HList, S <: HList] = Split0[HNil, L, N, P, S]
  
  trait Split0[AccP <: HList, AccS <: HList, N <: Nat, P <: HList, S <: HList] {
    def apply(accP : AccP, accS : AccS) : (P, S)
  }

  implicit def hlistSplit1[P <: HList, S <: HList] = new Split0[P, S, _0, P, S] {
    def apply(accP : P, accS : S) : (P, S) = (accP, accS)
  }

  implicit def hlistSplit2[AccP <: HList, AccSH, AccST <: HList, N <: Nat, P <: HList, S <: HList]
    (implicit st : Split0[AccP, AccST, N, P, S]) = new Split0[AccP, AccSH :: AccST, Succ[N], AccSH :: P, S] {
    def apply(accP : AccP, accS : AccSH :: AccST) : (AccSH :: P, S) =
      st(accP, accS.tail) match { case (prefix, suffix) => (accS.head :: prefix, suffix) }
  }

  trait ReverseSplit[L <: HList, N <: Nat] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def reverseSplit[L <: HList, N <: Nat, P0 <: HList, S0 <: HList]
    (implicit split : ReverseSplit0[HNil, L, N, P0, S0]) = new ReverseSplit[L, N] {
    type P = P0
    type S = S0
    def apply(l : L) : R = split(HNil, l)
  }
  
  type ReverseSplitAux[L <: HList, N <: Nat, P <: HList, S <: HList] = ReverseSplit0[HNil, L, N, P, S]
  
  trait ReverseSplit0[AccP <: HList, AccS <: HList, N <: Nat, P, S] {
    def apply(accP : AccP, accS : AccS) : (P, S)
  }

  implicit def hlistReverseSplit1[P <: HList, S <: HList] = new ReverseSplit0[P, S, _0, P, S] {
    def apply(accP : P, accS : S) : (P, S) = (accP, accS)
  }
  
  implicit def hlistReverseSplit2[AccP <: HList, AccSH, AccST <: HList, N <: Nat, P, S]
    (implicit st : ReverseSplit0[AccSH :: AccP, AccST, N, P, S]) = new ReverseSplit0[AccP, AccSH :: AccST, Succ[N], P, S] {
    def apply(accP : AccP, accS : AccSH :: AccST) : (P, S) = st(accS.head :: accP, accS.tail)
  }

  trait SplitLeft[L <: HList, U] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def splitLeft[L <: HList, U, P0 <: HList, S0 <: HList]
    (implicit splitLeft : SplitLeft0[HNil, L, U, P0, S0]) = new SplitLeft[L, U] {
    type P = P0
    type S = S0
    def apply(l : L) : R = splitLeft(HNil, l)
  }
  
  type SplitLeftAux[L <: HList, U, P <: HList, S <: HList] = SplitLeft0[HNil, L, U, P, S]
  
  trait SplitLeft0[AccP <: HList, AccS <: HList, U, P <: HList, S <: HList] {
    def apply(accP : AccP, accS : AccS) : (P, S)
  }

  implicit def hlistSplitLeft2[AccP <: HList, AccSH, AccST <: HList, U, P <: HList, S <: HList]
    (implicit slt : SplitLeft0[AccP, AccST, U, P, S]) = new SplitLeft0[AccP, AccSH :: AccST, U, AccSH :: P, S] {
    def apply(accP : AccP, accS : AccSH :: AccST) : (AccSH :: P, S) =
      slt(accP, accS.tail) match { case (prefix, suffix) => (accS.head :: prefix, suffix) }
  }

  trait ReverseSplitLeft[L <: HList, U] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def reverseSplitLeft[L <: HList, U, P0 <: HList, S0 <: HList]
    (implicit splitLeft : ReverseSplitLeft0[HNil, L, U, P0, S0]) = new ReverseSplitLeft[L, U] {
    type P = P0
    type S = S0
    def apply(l : L) : R = splitLeft(HNil, l)
  }
  
  type ReverseSplitLeftAux[L <: HList, U, P <: HList, S <: HList] = ReverseSplitLeft0[HNil, L, U, P, S]
  
  trait ReverseSplitLeft0[AccP <: HList, AccS <: HList, U, P, S] {
    def apply(accP : AccP, accS : AccS) : (P, S)
  }

  implicit def hlistReverseSplitLeft2[AccP <: HList, AccSH, AccST <: HList, U, P, S]
    (implicit slt : ReverseSplitLeft0[AccSH :: AccP, AccST, U, P, S]) = new ReverseSplitLeft0[AccP, AccSH :: AccST, U, P, S] {
    def apply(accP : AccP, accS : AccSH :: AccST) : (P, S) = slt(accS.head :: accP, accS.tail)
  }

  trait SplitRight[L <: HList, U] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def splitRight[L <: HList, U, P0 <: HList, S0 <: HList]
    (implicit splitRight : SplitRight0[L, HNil, HNil, U, P0, S0]) = new SplitRight[L, U] {
    type P = P0
    type S = S0
    def apply(l : L) : R = splitRight(l, HNil, HNil)
  }
  
  type SplitRightAux[L <: HList, U, P <: HList, S <: HList] = SplitRight0[L, HNil, HNil, U, P, S]
  
  trait SplitRight0[Rev <: HList, AccP <: HList, AccS <: HList, U, P <: HList, S <: HList] {
    def apply(rev : Rev, accP : AccP, accS : AccS) : (P, S)
  }

  implicit def hlistSplitRight1[RevH, RevT <: HList, AccP <: HList, U, P <: HList, S <: HList]
    (implicit srt : SplitRight0[RevT, RevH :: AccP, HNil, U, P, S]) = new SplitRight0[RevH :: RevT, AccP, HNil, U, P, S] {
    def apply(rev : RevH :: RevT, accP : AccP, accS : HNil) : (P, S) = srt(rev.tail, rev.head :: accP, accS)
  }

  implicit def hlistSplitRight2[AccPH, AccPT <: HList, AccS <: HList, U, P <: HList, S <: HList]
    (implicit srt : SplitRight0[HNil, AccPT, AccPH :: AccS, U, P, S]) = new SplitRight0[HNil, AccPH :: AccPT, AccS, U, P, S] {
    def apply(rev : HNil, accP : AccPH :: AccPT, accS : AccS) : (P, S) = srt(rev, accP.tail, accP.head :: accS)
  }
  
  trait ReverseSplitRight[L <: HList, U] {
    type R = (P, S)
    type P <: HList
    type S <: HList
    def apply(l : L) : R
  } 
  
  implicit def reverseSplitRight[L <: HList, U, P0 <: HList, S0 <: HList]
    (implicit splitRight : ReverseSplitRight0[L, HNil, HNil, U, P0, S0]) = new ReverseSplitRight[L, U] {
    type P = P0
    type S = S0
    def apply(l : L) : R = splitRight(l, HNil, HNil)
  }
  
  type ReverseSplitRightAux[L <: HList, U, P <: HList, S <: HList] = ReverseSplitRight0[L, HNil, HNil, U, P, S]
  
  trait ReverseSplitRight0[Rev <: HList, AccP <: HList, AccS <: HList, U, P, S] {
    def apply(rev : Rev, accP : AccP, accS : AccS) : (P, S)
  }
  
  implicit def hlistReverseSplitRight1[RevH, RevT <: HList, AccP <: HList, U, P <: HList, S <: HList]
    (implicit srt : ReverseSplitRight0[RevT, RevH :: AccP, HNil, U, P, S]) = new ReverseSplitRight0[RevH :: RevT, AccP, HNil, U, P, S] {
    def apply(rev : RevH :: RevT, accP : AccP, accS : HNil) : (P, S) = srt(rev.tail, rev.head :: accP, accS)
  }
  
  implicit def hlistReverseSplitRight2[AccPH, AccPT <: HList, AccS <: HList, U, P <: HList, S <: HList]
    (implicit srt : ReverseSplitRight0[HNil, AccPT, AccPH :: AccS, U, P, S]) = new ReverseSplitRight0[HNil, AccPH :: AccPT, AccS, U, P, S] {
    def apply(rev : HNil, accP : AccPH :: AccPT, accS : AccS) : (P, S) = srt(rev, accP.tail, accP.head :: accS)
  }

  trait Reverse[L <: HList] {
    type Out <: HList
    def apply(l : L) : Out
  }

  type ReverseAux[L <: HList, Out <: HList] = Reverse0[HNil, L, Out]
  
  implicit def reverse[L <: HList, Out0 <: HList](implicit reverse : Reverse0[HNil, L, Out0]) = new Reverse[L] {
    type Out = Out0
    def apply(l : L) : Out = reverse(HNil, l)
  }
  
  trait Reverse0[Acc <: HList, L <: HList, Out <: HList] {
    def apply(acc : Acc, l : L) : Out
  }
  
  implicit def hnilReverse[Out <: HList] = new Reverse0[Out, HNil, Out] {
    def apply(acc : Out, l : HNil) : Out = acc
  }
  
  implicit def hlistReverse[Acc <: HList, InH, InT <: HList, Out <: HList](implicit rt : Reverse0[InH :: Acc, InT, Out]) = new Reverse0[Acc, InH :: InT, Out] {
    def apply(acc : Acc, l : InH :: InT) : Out = rt(l.head :: acc, l.tail)
  }
  
  trait Prepend[P <: HList, S <: HList] {
    type Out
    def apply(prefix : P, suffix : S) : Out
  }

  implicit def prepend[P <: HList, S <: HList, Out0 <: HList](implicit prepend : Prepend0[P, S, Out0]) = new Prepend[P, S] {
    type Out = Out0
    def apply(prefix : P, suffix : S) : Out = prepend(prefix, suffix)
  }

  type PrependAux[P <: HList, S <: HList, Out <: HList] = Prepend0[P, S, Out]
  
  trait Prepend0[P <: HList, S <: HList, Out <: HList] {
    def apply(prefix : P, suffix : S) : Out
  }
  
  implicit def hnilPrepend[S <: HList] = new Prepend0[HNil, S, S] {
    def apply(prefix : HNil, suffix : S) : S = suffix 
  }
  
  implicit def hlistPrepend[PH, PT <: HList, S <: HList, OutT <: HList](implicit pt : Prepend0[PT, S, OutT]) = new Prepend0[PH :: PT, S, PH :: OutT] {
    def apply(prefix : PH :: PT, suffix : S) : PH :: OutT = prefix.head :: pt(prefix.tail, suffix)
  }

  trait ReversePrepend[P <: HList, S <: HList] {
    type Out <: HList
    def apply(prefix : P, suffix : S) : Out
  }

  implicit def reversePrepend[P <: HList, S <: HList, Out0 <: HList](implicit prepend : ReversePrepend0[P, S, Out0]) = new ReversePrepend[P, S] {
    type Out = Out0
    def apply(prefix : P, suffix : S) : Out = prepend(prefix, suffix)
  }

  type ReversePrependAux[P <: HList, S <: HList, Out <: HList] = ReversePrepend0[P, S, Out]

  trait ReversePrepend0[P <: HList, S <: HList, Out <: HList] {
    def apply(prefix : P, suffix : S) : Out
  }
  
  implicit def hnilReversePrepend[S <: HList] = new ReversePrepend0[HNil, S, S] {
    def apply(prefix : HNil, suffix : S) : S = suffix 
  }
  
  implicit def hlistReversePrepend[PH, PT <: HList, S <: HList, Out <: HList](implicit rpt : ReversePrepend0[PT, PH :: S, Out]) = new ReversePrepend0[PH :: PT, S, Out] {
    def apply(prefix : PH :: PT, suffix : S) : Out = rpt(prefix.tail, prefix.head :: suffix)
  }
  
  trait Cast[In <: HList, Out <: HList] {
    def apply(in : In) : Option[Out] 
  }
  
  implicit def hnilCast = new Cast[HNil, HNil] {
    def apply(in : HNil) = Option(in)
  }
  
  implicit def hlistCast[InH, InT <: HList, OutH, OutT <: HList](implicit ct : Cast[InT, OutT], oc : Castable[OutH]) = new Cast[InH :: InT, OutH :: OutT] {
    def apply(in : InH :: InT) : Option[OutH :: OutT] = for(h <- in.head.cast[OutH]; t <- ct(in.tail)) yield h :: t
  }
}

object HList extends LowPriorityHList {
  implicit def hlistSplitLeft1[P <: HList, SH, ST <: HList] = new SplitLeft0[P, SH :: ST, SH, P, SH :: ST] {
    def apply(accP : P, accS : SH :: ST) : (P, SH :: ST) = (accP, accS)
  }

  implicit def hlistReverseSplitLeft1[P <: HList, SH, ST <: HList] = new ReverseSplitLeft0[P, SH :: ST, SH, P, SH :: ST] {
    def apply(accP : P, accS : SH :: ST) : (P, SH :: ST) = (accP, accS)
  }
  
  implicit def hlistSplitRight3[PH, PT <: HList, S <: HList](implicit reverse : Reverse[PH :: PT]) = new SplitRight0[HNil, PH :: PT, S, PH, reverse.Out, S] {
    def apply(rev : HNil, accP : PH :: PT, accS : S) : (reverse.Out, S) = (accP.reverse, accS)
  }

  implicit def hlistReverseSplitRight3[PH, PT <: HList, S <: HList] = new ReverseSplitRight0[HNil, PH :: PT, S, PH, PH :: PT, S] {
    def apply(rev : HNil, accP : PH :: PT, accS : S) = (accP, accS)
  }
}
