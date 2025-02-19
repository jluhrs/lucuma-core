// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.model.sequence

import cats.Eq
import cats.Order.catsKernelOrderingForOrder
import cats.data.NonEmptySet
import cats.syntax.all._
import lucuma.core.enums._
import lucuma.core.math.Offset
import monocle.Focus
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.macros.GenPrism
import monocle.std.either.*

import scala.collection.immutable.SortedSet

sealed abstract class StepConfig(val stepType: StepType)

object StepConfig {

  case object Bias extends StepConfig(StepType.Bias)

  case object Dark extends StepConfig(StepType.Dark)

  final case class Gcal(
    lamp:      Gcal.Lamp,
    filter:    GcalFilter,
    diffuser:  GcalDiffuser,
    shutter:   GcalShutter
  ) extends StepConfig(StepType.Gcal) {

    def lampType: GcalLampType =
      lamp.lampType

  }

  object Gcal {
    implicit val eqStepConfigGcal: Eq[Gcal] =
      Eq.by(x => (x.lamp, x.filter, x.diffuser, x.shutter))

    opaque type Lamp = Either[GcalContinuum, NonEmptySet[GcalArc]]

    object Lamp {

      extension (lamp: Lamp) {

        def toEither: Either[GcalContinuum, NonEmptySet[GcalArc]] =
          lamp

        def fold[A](fc: GcalContinuum => A, fas: NonEmptySet[GcalArc] => A): A =
          lamp.fold(fc, fas)

        def lampType: GcalLampType =
          lamp.fold(_ => GcalLampType.Flat, _ => GcalLampType.Arc)

        def continuum: Option[GcalContinuum] =
          lamp.swap.toOption

        def arcs: Option[NonEmptySet[GcalArc]] =
          lamp.toOption

        def toArcsSortedSet: SortedSet[GcalArc] =
          arcs.fold(SortedSet.empty[GcalArc])(_.toSortedSet)

      }

      def fromEither(e: Either[GcalContinuum, NonEmptySet[GcalArc]]): Lamp =
        e

      def fromContinuum(c: GcalContinuum): Lamp =
        c.asLeft[NonEmptySet[GcalArc]]

      def fromArcs(as: NonEmptySet[GcalArc]): Lamp =
        as.asRight[GcalContinuum]

      def fromContinuumOrArcs(
        continuum: Option[GcalContinuum],
        arcs:      Iterable[GcalArc]
      ): Either[String, Lamp] =
        (continuum, arcs.toList) match {
          case (None, Nil)        => "Exactly one of continuum or arcs must be provided, received neither".asLeft
          case (Some(_), a :: as) => "Exactly one of continuum or arcs must be provided, received both".asLeft
          case (Some(u), _)       => u.asLeft[NonEmptySet[GcalArc]].asRight
          case (_, a :: as)       => NonEmptySet.of(a, as*).asRight[GcalContinuum].asRight
        }

      implicit val eqLamp: Eq[Lamp] =
        Eq.by { lamp => (lamp.continuum, lamp.arcs) }

    }

    /** @group Optics */
    val lamp: Lens[Gcal, Either[GcalContinuum, NonEmptySet[GcalArc]]] =
      Focus[Gcal](_.lamp)

    /** @group Optics */
    val continuum: Optional[Gcal, GcalContinuum] =
      lamp.andThen(stdLeft)

    /** @group Optics */
    val arcs: Optional[Gcal, NonEmptySet[GcalArc]] =
      lamp.andThen(stdRight)

    /** @group Optics */
    val filter: Lens[Gcal, GcalFilter] =
      Focus[Gcal](_.filter)

    /** @group Optics */
    val diffuser: Lens[Gcal, GcalDiffuser] =
      Focus[Gcal](_.diffuser)

    /** @group Optics */
    val shutter: Lens[Gcal, GcalShutter] =
      Focus[Gcal](_.shutter)
  }

  final case class Science(offset: Offset) extends StepConfig(StepType.Science)

  object Science {
    implicit val eqStepConfigScience: Eq[Science] = Eq.by(_.offset)

    /** @group Optics */
    val offset: Lens[Science, Offset] =
      Focus[Science](_.offset)
  }

  final case class SmartGcal(smartGcalType: SmartGcalType) extends StepConfig(StepType.SmartGcal)

  object SmartGcal {

    implicit val EqSmartGcal: Eq[SmartGcal] =
      Eq.by(_.smartGcalType)

    val smartGcalType: Lens[SmartGcal, SmartGcalType] =
      Focus[SmartGcal](_.smartGcalType)

  }

  implicit val eqStepConfig: Eq[StepConfig] = Eq.instance {
    case (Bias, Bias)                                 => true
    case (Dark, Dark)                                 => true
    case (a @ Gcal(_, _, _, _), b @ Gcal(_, _, _, _)) => a === b
    case (a @ Science(_), b @ Science(_))             => a === b
    case (a @ SmartGcal(_), b @ SmartGcal(_))         => a === b
    case _                                            => false
  }

  /** @group Optics */
  val gcal: Prism[StepConfig, Gcal] =
    GenPrism[StepConfig, Gcal]

  /** @group Optics */
  val science: Prism[StepConfig, Science] =
    GenPrism[StepConfig, Science]

  val smartGcal: Prism[StepConfig, SmartGcal] =
    GenPrism[StepConfig, SmartGcal]

}
