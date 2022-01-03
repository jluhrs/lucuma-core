// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.math.dimensional

import cats.Eq
import coulomb.unitops.UnitString
import lucuma.core.util.Display
import lucuma.core.util.Tag
import lucuma.core.util.TypeString
import shapeless.tag

import java.util.Objects

/**
 * Runtime representation of a physical unit. Wraps:
 *   - A type (usually phantom), meant to be `coulomb`'s type for the unit; and
 *   - A `UnitDefinition`, which is a common trait of `coulomb`'s `BaseUnit` and `DerivedUnit`.
 *
 * In `coulomb` units are types. However in some cases we want to have runtime representations of
 * units.
 */
trait Units { self =>
  type Type

  /** The full name of a unit, e.g. "squared meter" */
  def name: String

  /** The abbreviation of a unit, e.g. "m²" for "squared meter" */
  def abbv: String

  /**
   * The serialized form of a unit, e.g. "METER_2" for "squared meter"
   *
   * May be particularly useful in limited-charset codecs.
   *
   * The name `tag` was intentinally avoided to prevent confusion with the type-tags used in
   * `TaggedUnit` or `Units Of T`.
   */
  def serialized: String

  /**
   * Create a `Measure` with the specified value, keeping the runtime represantation of the units.
   */
  def withValue[N](value: N): Measure[N] = Measure[N](value, self)

  override def equals(obj: Any): Boolean = obj match {
    case that: Units => name == that.name && abbv == that.abbv
    case _           => false
  }

  override def hashCode: Int = Objects.hash(name, abbv, serialized)

  override def toString: String = abbv
}

object Units {
  implicit val eqUnitType: Eq[Units] = Eq.fromUniversalEquals

  implicit def displayUnitType: Display[Units] = Display.by(_.abbv, _.name)

  implicit class TaggedUnitTypeOps[T](val unitType: Units Of T) extends AnyVal {

    /**
     * Create a `Measure` with the specified value, keeping the runtime represantation of the units,
     * propagating the unit tag into the `Measure`.
     */
    def withValueTagged[N](value: N): Measure[N] Of T = {
      val tagged = tag[T](Measure[N](value, unitType))
      tagged
    }
  }
}

/**
 * Type-parametrized runtime representation of physical unit `U`.
 *
 * Can be automatically derived if there's an implicit `BaseUnit[U]` or `DerivedUnit[U, _]` in
 * scope.
 */
final case class UnitOfMeasure[U](name: String, abbv: String, serialized: String) extends Units {
  type Type = U

  def withSerialized(serialized: String): UnitOfMeasure[U] = copy(serialized = serialized)
}

object UnitOfMeasure {
  def apply[U: UnitOfMeasure]: UnitOfMeasure[U] = implicitly[UnitOfMeasure[U]]

  implicit def unitOfMeasureFromUnitString[U](implicit
    ev: UnitString[U],
    s:  TypeString[U]
  ): UnitOfMeasure[U] =
    UnitOfMeasure(ev.full, ev.abbv, s.serialized)

  implicit def eqUnitOfMeasure[U]: Eq[UnitOfMeasure[U]] = Eq.fromUniversalEquals
}

/**
 * Type class placing a `Tag` on a unit of measure.
 */
class TaggedUnit[U, T](implicit ev: UnitOfMeasure[U]) extends Tag[UnitOfMeasure[U], T] {
  def unit: UnitOfMeasure[U] Of T = {
    val tagged = tag[T](ev)
    tagged
  }
}
