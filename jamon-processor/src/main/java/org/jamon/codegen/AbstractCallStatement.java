/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Jamon code, released February, 2003.
 *
 * The Initial Developer of the Original Code is Jay Sachs.  Portions
 * created by Jay Sachs are Copyright (C) 2003 Jay Sachs.  All Rights
 * Reserved.
 *
 * Contributor(s): Ian Robertson
 */

package org.jamon.codegen;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jamon.api.Location;
import org.jamon.compiler.ParserErrorImpl;
import org.jamon.compiler.ParserErrorsImpl;
import org.jamon.util.StringUtils;

public abstract class AbstractCallStatement extends AbstractStatement implements CallStatement {
  AbstractCallStatement(
    String path, ParamValues params, Location location, String templateIdentifier) {
    super(location, templateIdentifier);
    this.path = path;
    this.params = params;
  }

  @Override
  public void addFragmentImpl(FragmentUnit unit, ParserErrorsImpl errors) {
    fragParams.put(unit.getName(), unit);
  }

  private final String path;

  private final ParamValues params;

  private final Map<String, FragmentUnit> fragParams = new HashMap<String, FragmentUnit>();

  private final static String FRAGMENT_IMPL_PREFIX = "__jamon__instanceOf__";

  private final Map<FragmentUnit, String> m_fragmentImplNames = new HashMap<FragmentUnit, String>();

  protected abstract String getFragmentIntfName(FragmentUnit p_fragmentUnitIntf);

  private String getFragmentImplName(CodeWriter writer, FragmentUnit fragmentUnitIntf) {
    if (!m_fragmentImplNames.containsKey(fragmentUnitIntf)) {
      m_fragmentImplNames.put(fragmentUnitIntf, FRAGMENT_IMPL_PREFIX
        + writer.nextFragmentImplCounter() + "__"
        + fragmentUnitIntf.getFragmentInterfaceName(false));
    }
    return m_fragmentImplNames.get(fragmentUnitIntf);
  }

  private void makeFragmentImplClass(
    FragmentUnit fragmentUnitIntf, CodeWriter writer, TemplateDescriber describer)
  throws ParserErrorImpl {
    final FragmentUnit fragmentUnitImpl = fragParams.remove(fragmentUnitIntf.getName());
    if (fragmentUnitImpl == null) {
      throw new ParserErrorImpl(getLocation(), "Call is missing fragment "
        + fragmentUnitIntf.getName());
    }

    writer.println("class " + getFragmentImplName(writer, fragmentUnitIntf));
    writer.println("  extends " + ClassNames.BASE_TEMPLATE);
    writer.println("  implements " + getFragmentIntfName(fragmentUnitIntf));
    writer.openBlock();
    writer.println("public " + getFragmentImplName(writer, fragmentUnitIntf) + "("
      + ClassNames.TEMPLATE_MANAGER + " p_manager)");
    writer.openBlock();
    writer.println("super(p_manager);");
    writer.closeBlock();
    writer.print("@Override public " + ClassNames.RENDERER + " makeRenderer");
    writer.openList();
    fragmentUnitImpl.printRenderArgsDecl(writer);
    writer.closeList();
    writer.println();
    writer.openBlock();
    writer.print("return new " + ClassNames.ABSTRACT_RENDERER + "()");
    writer.openBlock();
    writer.println("@Override");
    writer.println("public void renderTo(" + ArgNames.WRITER_DECL + ")");
    fragmentUnitImpl.generateThrowsIOExceptionIfNecessary(writer);
    writer.openBlock();
    writer.print("renderNoFlush");
    writer.openList();
    writer.printListElement(ArgNames.WRITER);
    fragmentUnitImpl.printRenderArgs(writer);
    writer.closeList();
    writer.println(";");
    writer.closeBlock();
    writer.closeBlock(";");
    writer.closeBlock();

    writer.print("@Override public void renderNoFlush");
    writer.openList();
    writer.printListElement(ArgNames.WRITER_DECL);
    fragmentUnitImpl.printRenderArgsDecl(writer);
    writer.closeList();
    fragmentUnitImpl.generateThrowsIOExceptionIfNecessary(writer);
    fragmentUnitImpl.generateRenderBody(writer, describer);

    writer.closeBlock();
  }

  protected void makeFragmentImplClasses(
    List<FragmentArgument> fragmentInterfaces, CodeWriter writer, TemplateDescriber describer)
  throws ParserErrorImpl {
    if (fragParams.size() == 1 && fragParams.keySet().iterator().next() == null) {
      if (fragmentInterfaces.size() == 0) {
        throw new ParserErrorImpl(getLocation(), "Call provides a fragment, but none are expected");
      }
      else if (fragmentInterfaces.size() > 1) {
        throw new ParserErrorImpl(getLocation(), "Call must provide multiple fragments");
      }
      else {
        fragParams.put(fragmentInterfaces.get(0).getName(), fragParams.remove(null));
      }
    }
    for (FragmentArgument arg : fragmentInterfaces) {
      makeFragmentImplClass(arg.getFragmentUnit(), writer, describer);
    }
  }

  protected void generateFragmentParams(
    CodeWriter writer, List<FragmentArgument> fragmentInterfaces) {
    for (FragmentArgument fragmentArgument : fragmentInterfaces) {
      writer.printListElement("new "
        + getFragmentImplName(writer, fragmentArgument.getFragmentUnit())
        + "(this.getTemplateManager())");
    }
  }

  protected void checkSuppliedParams() throws ParserErrorImpl {
    if (getParams().hasUnusedParams()) {
      throw constructExtraParamsException("arguments", getParams().getUnusedParams());
    }
    if (!fragParams.isEmpty()) {
      throw constructExtraParamsException("fragments", fragParams.keySet());
    }
  }

  ParserErrorImpl constructExtraParamsException(String paramType, Iterable<String> extraParams) {
    StringBuilder message = new StringBuilder("Call provides unused ");
    message.append(paramType);
    message.append(" ");
    StringUtils.commaJoin(message, extraParams);
    return new ParserErrorImpl(getLocation(), message.toString());
  }

  protected final String getPath() {
    return path;
  }

  protected final ParamValues getParams() {
    return params;
  }
}
