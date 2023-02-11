// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.model.arb

import coulomb.*
import coulomb.syntax.*
import eu.timepit.refined.types.numeric.PosBigDecimal
import lucuma.core.math.BrightnessUnits
import lucuma.core.math.LineWidthValue
import lucuma.core.math.arb.ArbLineFluxValue
import lucuma.core.math.arb.ArbLineWidthValue
import lucuma.core.math.arb.ArbRefined
import lucuma.core.math.dimensional.*
import lucuma.core.math.dimensional.arb.ArbMeasure
import lucuma.core.math.units.*
import lucuma.core.model.EmissionLine
import lucuma.core.util.*
import lucuma.core.util.arb.ArbEnumerated
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.*

trait ArbEmissionLine {
  import ArbEnumerated.*
  import ArbLineFluxValue.given
  import ArbLineWidthValue.given
  import ArbMeasure.given
  import ArbRefined.given
  import BrightnessUnits.*

  given arbEmissionLine[T](using
    arbLineFluxUnit: Arbitrary[Units Of LineFlux[T]]
  ): Arbitrary[EmissionLine[T]] =
    Arbitrary(
      for {
        lw <- arbitrary[LineWidthValue]
        lf <- arbitrary[LineFluxMeasure[T]]
      } yield EmissionLine[T](lw.withUnit[KilometersPerSecond], lf)
    )

  given cogEmissionLine[T]: Cogen[EmissionLine[T]] =
    Cogen[(LineWidthValue, LineFluxMeasure[T])].contramap(x => (x.lineWidth.value, x.lineFlux))
}

object ArbEmissionLine extends ArbEmissionLine
