// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.math

import cats.Eq
import cats.Show
import cats.implicits._
import cats.kernel.laws.discipline._
import eu.timepit.refined.auto._
import lucuma.core.arb.ArbTime
import lucuma.core.math.arb._
import lucuma.core.math.parser.EpochParsers._
import lucuma.core.math.refined._
import lucuma.core.optics.laws.discipline._
import lucuma.core.syntax.parser._
import monocle.law.discipline.PrismTests
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

import java.time.LocalDateTime

final class EpochSuite extends munit.DisciplineSuite {
  import ArbEpoch._
  import ArbTime._

  // provide an Arbitrary[String] for the Prism tests.
  implicit val arbEpochString: Arbitrary[String] = Arbitrary(ArbEpoch.strings)

  // Laws
  checkAll("Epoch", OrderTests[Epoch].order)
  checkAll("fromString", PrismTests(Epoch.fromString))
  checkAll("fromStringNoScheme",
           FormatTests(Epoch.fromStringNoScheme)
             .formatWith(ArbEpoch.stringsNoScheme)(Eq[String], arbJulianEpoch, Eq[Epoch])
  )

  test("Epoch.eq.natural") {
    forAll { (a: Epoch, b: Epoch) =>
      assertEquals(a.equals(b), Eq[Epoch].eqv(a, b))
    }
  }

  test("Epoch.show.natural") {
    forAll { (a: Epoch) =>
      assertEquals(a.toString, Show[Epoch].show(a))
    }
  }

  test("Epoch.until.identity") {
    forAll { (a: Epoch) =>
      assertEquals(a.untilEpochYear(a.epochYear), 0.0)
    }
  }

  test("Epoch.until.sanity") {
    forAll { (a: Epoch, s: Short) =>
      assertEqualsDouble(a.untilEpochYear(a.epochYear + s.toDouble), s.toDouble, 0.005)
    }
  }

  test("Epoch.until.sanity2") {
    forAll { (s: Epoch.Scheme, d1: LocalDateTime, d2: LocalDateTime) =>
      // the dates generated by ArbTime are between years 2000 and 2020 and are safe for this.
      val Δ1 = s.fromLocalDateTime(d1).get.untilLocalDateTime(d2)
      val Δ2 = s.fromLocalDateTime(d2).get.epochYear - s.fromLocalDateTime(d1).get.epochYear
      assertEqualsDouble(Δ1, Δ2, 0.005)
    }
  }

  test("epochLenientNoScheme") {
    assertEquals(epochLenientNoScheme.parseExact("2014.123"),
                 Epoch.Julian.fromMilliyears(2014123.refined).some
    )
    assertEquals(epochLenientNoScheme.parseExact("2014"), Epoch.Julian.fromMilliyears(2014000.refined).some)
    assertEquals(epochLenientNoScheme.parseExact("2014."),
                 Epoch.Julian.fromMilliyears(2014000.refined).some
    )
    assertEquals(epochLenientNoScheme.parseExact("2014.1"),
                 Epoch.Julian.fromMilliyears(2014100.refined).some
    )
    assertEquals(epochLenientNoScheme.parseExact("2014.092"),
                 Epoch.Julian.fromMilliyears(2014092.refined).some
    )
    assertEquals(epochLenientNoScheme.parseExact("2014.002"),
                 Epoch.Julian.fromMilliyears(2014002.refined).some
    )
    assertEquals(epochLenientNoScheme.parseExact("J2014.123"), None)
    assertEquals(epochLenientNoScheme.parseExact("J2014"), None)
    assertEquals(epochLenientNoScheme.parseExact("J2014."), None)
    assertEquals(epochLenientNoScheme.parseExact("J2014.1"), None)
    assertEquals(epochLenientNoScheme.parseExact("B2014.123"), None)
    assertEquals(epochLenientNoScheme.parseExact("B2014"), None)
    assertEquals(epochLenientNoScheme.parseExact("B2014."), None)
    assertEquals(epochLenientNoScheme.parseExact("B2014.1"), None)
  }

  test("epochFormatNoScheme") {
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014123.refined)),
                 "2014.123"
    )
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014000.refined)), "2014")
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014300.refined)),
                 "2014.3"
    )
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014092.refined)),
                 "2014.092"
    )
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014002.refined)),
                 "2014.002"
    )
    assertEquals(Epoch.fromStringNoScheme.reverseGet(Epoch.Julian.fromMilliyears(2014350.refined)),
                 "2014.35"
    )
  }
}
