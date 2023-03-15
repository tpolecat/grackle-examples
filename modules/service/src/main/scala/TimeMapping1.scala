// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import cats.*
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import edu.gemini.grackle.Mapping
import edu.gemini.grackle.Schema
import edu.gemini.grackle.ValueMapping
import edu.gemini.grackle.syntax.*

import java.time.ZoneId
import scala.io.Source
import scala.jdk.CollectionConverters.*

// basic case
object TimeMapping1 extends ExampleMain:
  def mapping = Resource.pure(new TimeMapping1[IO])

class TimeMapping1[F[_]: Monad] extends ValueMapping[F]:

  val schema: Schema =
    schema"""
    
      type Query {
        timeZones: [TimeZone!]!
      }

      type TimeZone {
        id: String!
      }

    """

  // References to types in the schema
  val QueryType  = schema.ref("Query")
  val TimeZoneType = schema.ref("TimeZone")

  // Our database
  val zones: List[ZoneId] =
    ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of).toList

  // Mapping our schema types into the database
  val typeMappings: List[TypeMapping] =
    List(
      ValueObjectMapping[Unit](
        QueryType,
        List(
          ValueField("timeZones", _ => zones)
        ),
      ),
      ValueObjectMapping[ZoneId](
        TimeZoneType,
        List(
          ValueField("id", z => z.getId)
        ),
      ),
    )

  