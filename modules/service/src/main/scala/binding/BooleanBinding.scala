// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package binding

import edu.gemini.grackle.Value.BooleanValue

val BooleanBinding: Matcher[Boolean] =
  primitiveBinding("Boolean") { case BooleanValue(value) => value }