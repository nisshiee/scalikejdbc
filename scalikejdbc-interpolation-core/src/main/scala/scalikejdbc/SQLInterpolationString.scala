/*
 * Copyright 2013 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

private[scalikejdbc] object LastParameter

/**
 * SQLInterpolation definition
 */
class SQLInterpolationString(val s: StringContext) extends AnyVal {

  import scalikejdbc.interpolation.SQLSyntax

  def sql[A](params: Any*): SQL[A, NoExtractor] = {
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.parameters: _*)
  }

  def sqls(params: Any*): SQLSyntax = SQLSyntax(buildQuery(params), buildParams(params))

  private def buildQuery(params: Seq[Any]): String =
    s.parts.zipAll(params, "", LastParameter).foldLeft(new StringBuilder) {
      case (sb, (previousQueryPart, param)) =>
        sb ++= previousQueryPart
        addPlaceholders(sb, param)
    }.result()

  private def addPlaceholders(sb: StringBuilder, param: Any): StringBuilder = param match {
    case _: String => sb += '?'
    case t: Traversable[_] => t.map(_ => "?").addString(sb, ", ") // e.g. in clause
    case LastParameter => sb
    case SQLSyntax(s, _) => sb ++= s
    case _ => sb += '?'
  }

  private def buildParams(params: Seq[Any]): Seq[Any] = params.foldLeft(Seq.newBuilder[Any]) {
    case (b, s: String) => b += s
    case (b, t: Traversable[_]) => b ++= t
    case (b, SQLSyntax(_, params)) => b ++= params
    case (b, n) => b += n
  }.result()

}
