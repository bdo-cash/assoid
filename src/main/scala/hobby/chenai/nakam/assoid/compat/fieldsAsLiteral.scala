package hobby.chenai.nakam.assoid.compat

import hobby.chenai.nakam.tool.macros.impl
import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros

@compileTimeOnly("enable macro paradise to expand macro annotations")
class fieldsAsLiteral extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro impl.fieldsAsLiteral
}
