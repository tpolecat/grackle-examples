// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import binding.StringBinding
import cats.*
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.syntax.all.*
import edu.gemini.grackle.Mapping
import edu.gemini.grackle.Predicate.Const
import edu.gemini.grackle.Predicate.Eql
import edu.gemini.grackle.Query.Binding
import edu.gemini.grackle.Query.Select
import edu.gemini.grackle.Query.*
import edu.gemini.grackle.QueryCompiler.SelectElaborator
import edu.gemini.grackle.Result
import edu.gemini.grackle.Schema
import edu.gemini.grackle.Value.StringValue
import edu.gemini.grackle.ValueMapping
import edu.gemini.grackle.syntax.*

import java.time.ZoneId
import scala.io.Source
import scala.jdk.CollectionConverters.*

object TimeMapping4 extends ExampleMain:
  def mapping =
    Resource.eval {
      Ref[IO].of(ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of).toList).map { ref =>
        new TimeMapping4[IO](ref)
      }
    }

// introduce an effect
class TimeMapping4[F[_]: Monad](zones: Ref[F, List[ZoneId]]) extends ValueMapping[F]:

  val schema: Schema =
    schema"""
    
      type Query {
        timeZones: [TimeZone!]!
        timeZone(id: String!): TimeZone
      }

      type TimeZone {
        id: String!
      }

    """

  // References to types in the schema
  val QueryType  = schema.ref("Query")
  val TimeZoneType = schema.ref("TimeZone")

  // Mapping our schema types into the database
  val typeMappings: List[TypeMapping] =
    List(
      ValueObjectMapping[Unit](
        QueryType,
        List(
          RootEffect.computeCursor("timeZones") { (q, p, e) => zones.get.map(zs => Result(valueCursor(p, e, zs))) },
          RootEffect.computeCursor("timeZone") { (q, p, e) => zones.get.map(zs => Result(valueCursor(p, e, zs))) },
        ),
      ),
      ValueObjectMapping[ZoneId](
        TimeZoneType,
        List(
          ValueField("id", z => z.getId)
        ),
      ),
    )

  // Queries that take arguemnst require elaboration
  override val selectElaborator: SelectElaborator =
    SelectElaborator(Map(
      QueryType -> {
        case Select("timeZone", List(StringBinding("id", rId)), child) =>
          rId.map { id =>
            Select("timeZone", Nil, Unique(Filter(Eql(TimeZoneType / "id", Const(id)), child)))
          }
      }
    ))

  