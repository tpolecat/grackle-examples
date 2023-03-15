// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package binding

import edu.gemini.grackle.Value.StringValue

val StringBinding: Matcher[String] =
  primitiveBinding("String") { case StringValue(value) => value }