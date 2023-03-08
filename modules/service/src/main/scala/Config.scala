// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package example

import cats.effect.kernel.Async
import com.comcast.ip4s.Port

case class Config(port: Port)

object Config:
  def load[F[_]: Async]: F[Config] =
    Async[F].pure(Config(Port.fromInt(8080).get)) // todo