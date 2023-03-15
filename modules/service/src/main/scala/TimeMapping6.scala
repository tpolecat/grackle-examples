// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import binding.StringBinding
import cats.*
import cats.effect.Async
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import cats.syntax.all.*
import edu.gemini.grackle.Cursor.Env
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
import fs2.Stream
import io.circe.Encoder
import io.circe.Json

import java.time.Instant
import java.time.ZoneId
import scala.concurrent.duration.*
import scala.io.Source
import scala.jdk.CollectionConverters.*

object TimeMapping6 extends ExampleMain:
   def mapping = Resource.eval {
      Ref[IO].of(ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of).toList).map { ref =>
        new TimeMapping6[IO](ref)
      }
    }

// add a subscription
class TimeMapping6[F[_]: Async](zones: Ref[F, List[ZoneId]]) extends ValueMapping[F]:

  val schema: Schema =
    schema"""
    
      type Query {
        timeZones: [TimeZone!]!
        timeZone(id: String!): TimeZone
      }

      type Mutation {
        delete(id: String!): Deletion!
      }

      type Subscription {
        now: String!
      }

      enum Deletion {
        DELETED
        NOT_FOUND
      }

      type TimeZone {
        id: String!
      }

    """

  // References to types in the schema
  val DeletionType = schema.ref("Deletion")
  val MutationType  = schema.ref("Mutation")
  val QueryType  = schema.ref("Query")
  val SubscriptionType  = schema.ref("Subscription")
  val TimeZoneType = schema.ref("TimeZone")

  enum Deletion:
    case Deleted
    case NotFound

  given Encoder[Deletion] =
    Encoder[String].contramap {
      case Deletion.Deleted  => "DELETED"
      case Deletion.NotFound => "NOT_FOUND"
    }
    
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
      ValueObjectMapping[Unit](
        MutationType,
        List(
          RootEffect.computeCursor("delete") { (q, p, e) => 
            zones.modify { zs => 
              e.get[String]("id") match
                case None => (zs, Result.failure(s"Implementation error; no id in $e."))
                case Some(id) =>
                  val zsʹ = zs.filterNot(_.getId == id)
                  val deleted = zsʹ.length < zs.length
                  (zsʹ, Result(valueCursor(p, e, if deleted then Deletion.Deleted else Deletion.NotFound)))
            }
          },
        ),
      ),
      ValueObjectMapping[Unit](
        SubscriptionType,
        List(
          RootEffect.computeCursorStream("now") { (q, p, e) => 
            Stream.awakeEvery(1.second).evalMap { _ =>
              Async[F].delay(Instant.now().toString()).map { 
                s => Result(valueCursor(p, e, s))
              }
            }
          }
        )
      ),
      ValueObjectMapping[ZoneId](
        TimeZoneType,
        List(
          ValueField("id", z => z.getId)
        ),
      ),
      LeafMapping[Deletion](DeletionType),
    )

  // Queries that take arguemnst require elaboration
  override val selectElaborator: SelectElaborator =
    SelectElaborator(Map(
      QueryType -> {
        case Select("timeZone", List(StringBinding("id", rId)), child) =>
          rId.map { id =>
            Select("timeZone", Nil, Unique(Filter(Eql(TimeZoneType / "id", Const(id)), child)))
          }
      },
      MutationType -> {
        case Select("delete", List(StringBinding("id", rId)), child) =>
          rId.map { id =>
            Environment(Env("id" -> id), Select("delete", Nil, child))
          }
      }
    ))

  