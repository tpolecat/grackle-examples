// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example 

import _root_.skunk.Session
import _root_.skunk.codec.all.*
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits._
import edu.gemini.grackle.Predicate._
import edu.gemini.grackle.Query._
import edu.gemini.grackle.QueryCompiler._
import edu.gemini.grackle.Value._
import edu.gemini.grackle._
import edu.gemini.grackle.skunk.SkunkMapping
import edu.gemini.grackle.skunk.SkunkMonitor
import edu.gemini.grackle.sql.Like
import edu.gemini.grackle.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.IO
import natchez.Trace.Implicits.noop

// add a join
object WorldMapping2 extends ExampleMain:
  def mapping =
    Session.pooled[IO](
      host = "localhost",
      user = "test",
      database = "test",
      password = Some("test"),
      max = 3,
    ).map(new WorldMapping2(_))

class WorldMapping2[F[_]: Sync](pool: Resource[F, Session[F]]) extends SkunkMapping[F](pool, SkunkMonitor.noopMonitor):

  object country extends TableDef("country") {
    val code       = col("code", bpchar(3))
    val name       = col("name", text)
    val population = col("population", int4)
  }

  object city extends TableDef("city") {
    val id          = col("id", int4)
    val countrycode = col("countrycode", text)
    val name        = col("name", text)
    val district    = col("district", text)
    val population  = col("population", int4)
  }

  val schema =
    schema"""
      type Query {
        countries: [Country!]
      }
      type Country {
        code: String!
        name: String!
        population: Int!
        cities: [City!]!
      }
      type City {
        name: String!
        country: Country!
        district: String!
        population: Int!
      }
    """

  val QueryType   = schema.ref("Query")
  val CountryType = schema.ref("Country")
  val CityType    = schema.ref("City")

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings = List(
          SqlObject("countries"),
        )
      ),
      ObjectMapping(
        tpe = CountryType,
        fieldMappings = List(
          SqlField("code",       country.code, key = true),
          SqlField("name",       country.name),
          SqlField("population", country.population),
          SqlObject("cities",    Join(country.code, city.countrycode)),
        ),
      ),
      ObjectMapping(
        tpe = CityType,
        fieldMappings = List(
          SqlField("id", city.id, key = true, hidden = true),
          SqlField("name", city.name),
          SqlField("district", city.district),
          SqlField("population", city.population),
          SqlObject("country", Join(city.countrycode, country.code)),
        )
      ),
    )


  