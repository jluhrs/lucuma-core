// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.core.model

import lucuma.core.util.arb._
import lucuma.core.model.arb._
import munit._
import lucuma.core.util.laws.GidTests
import cats.kernel.laws.discipline.EqTests

final class RoleSuite extends DisciplineSuite {
  import ArbGid._
  import ArbStandardRole._

  // Laws
  checkAll("StandardRole.Id", GidTests[StandardRole.Id].gid)
  checkAll("Eq[StandardRole]", EqTests[StandardRole].eqv)

}
