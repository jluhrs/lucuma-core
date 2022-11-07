// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.math

import algebra.instances.all.given
import cats.*
import cats.syntax.all.*
import coulomb.*
import coulomb.ops.algebra.cats.all.given
import coulomb.policy.spire.standard.given
import coulomb.syntax.*
import coulomb.units.accepted.*
import lucuma.core.math.ProperMotion.AngularVelocityComponent
import lucuma.core.math.units.*
import lucuma.core.optics.SplitMono
import monocle.Focus
import monocle.Iso
import monocle.Lens
import spire.math.Rational

object VelocityAxis {
  type RA
  type Dec
}

/**
  * ProperMotion contains Ra/Dec angular velocities
  */
final case class ProperMotion(
  ra:  ProperMotion.AngularVelocityComponent[VelocityAxis.RA],
  dec: ProperMotion.AngularVelocityComponent[VelocityAxis.Dec]
) {
  // Return the ra/dec components in radians, first converting to degrees/y
  inline def toRadians: (Double, Double) =
    (ra.toRadians, dec.toRadians)

}

object ProperMotion extends ProperMotionOptics {
  opaque type AngularVelocityComponent[A] = Quantity[Long, MicroArcSecondPerYear]
  extension[A](self: AngularVelocityComponent[A])
    inline def μasy: Quantity[Long, MicroArcSecondPerYear] = self

    // Direct conversion via coulomb turns to be too slow
    inline def toRadians: Double = (μasy.value.toDouble / (3600 * 1e6)).toRadians

    def masy: Quantity[Rational, MilliArcSecondPerYear] = μasy.toValue[Rational].toUnit[MilliArcSecondPerYear]

    def toString: String =
      s"AngularVelocityComponent(${masy.show})"

  object AngularVelocityComponent extends AngularVelocityComponentOptics {
    inline def apply[A](w: AngularVelocityComponent[A]): AngularVelocityComponent[A] = w

    def Zero[A]: AngularVelocityComponent[A] =
      AngularVelocityComponent(0.withUnit[MicroArcSecondPerYear])

    /** @group Typeclass Instances */
    given orderAngularVelocity[A]: Order[AngularVelocityComponent[A]] =
      Order.by(_.μasy.value)

    /** @group Typeclass Instances */
    given monoidAngularVelocity[A]: Monoid[AngularVelocityComponent[A]] =
      Monoid.instance(Zero[A], (a, b) => AngularVelocityComponent[A](a.μasy + b.μasy))

    /** @group Typeclass Instances */
    given showAngularVelocity[A]: Show[AngularVelocityComponent[A]] =
      Show.fromToString

  }

  sealed trait AngularVelocityComponentOptics {

    def μasy[A]: Iso[Long, AngularVelocityComponent[A]] =
      Iso[Long, AngularVelocityComponent[A]](v =>
        AngularVelocityComponent(v.withUnit[MicroArcSecondPerYear])
      )(_.μasy.value)

    def microarcsecondsPerYear[A]: Iso[Long, AngularVelocityComponent[A]] = μasy

    /**
      * This `AngularVelocityComponent` as signed decimal milliseconds.
      */
    def milliarcsecondsPerYear[A]: SplitMono[AngularVelocityComponent[A], BigDecimal] =
      SplitMono
        .fromIso[AngularVelocityComponent[A], Long](microarcsecondsPerYear.reverse)
        .imapB(_.underlying.movePointRight(3).longValue,
               n => new java.math.BigDecimal(n).movePointLeft(3)
        )
  }

  type RA  = AngularVelocityComponent[VelocityAxis.RA]

  type Dec = AngularVelocityComponent[VelocityAxis.Dec]

  trait AngularVelocityComponentCompanion[A] {

    def apply(μasy: Quantity[Long, MicroArcSecondPerYear]): AngularVelocityComponent[A] =
      AngularVelocityComponent[A](μasy)

    val Zero: AngularVelocityComponent[A] =
      AngularVelocityComponent.Zero[A]

    val microarcsecondsPerYear: Iso[AngularVelocityComponent[A], Long] =
      AngularVelocityComponent.microarcsecondsPerYear[A].reverse

    val milliarcsecondsPerYear: SplitMono[AngularVelocityComponent[A], BigDecimal] =
      AngularVelocityComponent.milliarcsecondsPerYear[A]

  }

  object RA extends AngularVelocityComponentCompanion[VelocityAxis.RA]

  object Dec extends AngularVelocityComponentCompanion[VelocityAxis.Dec]

  /**
    * The `No parallax`
    * @group Constructors
    */
  val Zero: ProperMotion =
    ProperMotion(AngularVelocityComponent.Zero[VelocityAxis.RA],
                   AngularVelocityComponent.Zero[VelocityAxis.Dec]
    )

  /** @group Typeclass Instances */
  given Order[ProperMotion] =
    Order.by(x => (x.ra, x.dec))

  /** @group Typeclass Instances */
  given Monoid[ProperMotion] =
    Monoid.instance(Zero, (a, b) => ProperMotion(a.ra |+| b.ra, a.dec |+| b.dec))

}

sealed trait ProperMotionOptics {

  /** @group Optics */
  val ra: Lens[ProperMotion, AngularVelocityComponent[VelocityAxis.RA]] =
    Focus[ProperMotion](_.ra)

  /** @group Optics */
  val dec: Lens[ProperMotion, AngularVelocityComponent[VelocityAxis.Dec]] =
    Focus[ProperMotion](_.dec)

  private def splitMonoFromComponents[A](
    raMono:  SplitMono[AngularVelocityComponent[VelocityAxis.RA], A],
    decMono: SplitMono[AngularVelocityComponent[VelocityAxis.Dec], A]
  ): SplitMono[ProperMotion, (A, A)] =
    SplitMono(
      v => (raMono.get(v.ra), decMono.get(v.dec)),
      t => ProperMotion(raMono.reverseGet(t._1), decMono.reverseGet(t._2))
    )

  /** @group Optics */
  val milliarcsecondsPerYear: SplitMono[ProperMotion, (BigDecimal, BigDecimal)] =
    splitMonoFromComponents(
      ProperMotion.RA.milliarcsecondsPerYear,
      ProperMotion.Dec.milliarcsecondsPerYear
    )

}
