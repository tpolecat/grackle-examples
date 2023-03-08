// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import cats.*
import cats.syntax.all.*
import edu.gemini.grackle.Schema
import edu.gemini.grackle.ValueMapping
import scala.io.Source

class TimeMapping[F[_]: Monad] extends ValueMapping[F]:

  def unsafeLoadSchema(fileName: String): Schema = {
    val stream = getClass.getResourceAsStream(fileName)
    val src  = Source.fromInputStream(stream, "UTF-8")
    try Schema(src.getLines().mkString("\n")).toEither.fold(x => sys.error(s"Invalid schema: $fileName: ${x.toList.mkString(", ")}"), identity)
    finally src.close()
  }

  val schema: Schema =
    unsafeLoadSchema("Time.graphql")

  val typeMappings: List[TypeMapping] = Nil