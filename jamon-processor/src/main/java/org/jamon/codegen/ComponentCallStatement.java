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

import org.jamon.api.Location;
import org.jamon.compiler.ParserErrorImpl;
import org.jamon.node.GenericCallParam;

public class ComponentCallStatement extends AbstractCallStatement {
  ComponentCallStatement(
    String path,
    ParamValues params,
    Location location,
    String templateIdentifier,
    List<GenericCallParam> genericParams,
    String callingTemplateJamonContextType) {
    super(path, params, location, templateIdentifier);
    this.genericParams = genericParams;
    this.callingTemplateJamonContextType = callingTemplateJamonContextType;
  }

  @Override
  protected String getFragmentIntfName(FragmentUnit fragmentUnitIntf) {
    return getComponentProxyClassName() + "." + fragmentUnitIntf.getFragmentInterfaceName(false)
      + getGenericParams();
  }

  @Override
  public void generateSource(CodeWriter writer, TemplateDescriber describer)
  throws ParserErrorImpl {
    generateSourceLine(writer);
    writer.openBlock();
    TemplateDescription desc;
    try {
      desc = describer.getTemplateDescription(getPath(), getLocation());
    }
    catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }

    if (desc.getJamonContextType() != null && callingTemplateJamonContextType == null) {
      throw new ParserErrorImpl(
        getLocation(),
        "Calling component does not have a jamonContext, but called component " + getPath()
        + " expects one of type " + desc.getJamonContextType());
    }
    if (hasGenericParams()) {
      if (desc.getGenericParamsCount() != getGenericParamCount()) {
        throw new ParserErrorImpl(
          getLocation(),
          "Call to component " + getPath() + " provides "
          + getGenericParamCount() + " generic params, but " + getPath() + " only expects "
          + desc.getGenericParamsCount());
      }
    }

    makeFragmentImplClasses(desc.getFragmentInterfaces(), writer, describer);
    String instanceVar = getUniqueName();
    writer
        .println(
          getComponentProxyClassName() + getGenericParams() + " " + instanceVar + " = new "
          + getComponentProxyClassName() + getGenericParams() + "(this.getTemplateManager());");

    if (desc.getJamonContextType() != null) {
      writer.println(instanceVar + ".setJamonContext(jamonContext);");
    }

    for (OptionalArgument arg : desc.getOptionalArgs()) {
      String value = getParams().getOptionalArgValue(arg.getName());
      if (value != null) {
        writer.println(instanceVar + "." + arg.getSetterName() + "(" + value + ");");
      }
    }
    writer.print(instanceVar + ".renderNoFlush");
    writer.openList();
    writer.printListElement(ArgNames.WRITER);
    getParams().generateRequiredArgs(desc.getRequiredArgs(), writer);
    generateFragmentParams(writer, desc.getFragmentInterfaces());
    writer.closeList();
    writer.println(";");
    checkSuppliedParams();
    writer.closeBlock();
  }

  protected boolean hasGenericParams() {
    return !genericParams.isEmpty();
  }

  protected int getGenericParamCount() {
    return genericParams.size();
  }

  protected String getGenericParams() {
    if (hasGenericParams()) {
      StringBuilder builder = new StringBuilder();
      builder.append('<');
      boolean paramsAdded = false;
      for (GenericCallParam param : genericParams) {
        if (paramsAdded) {
          builder.append(", ");
        }
        builder.append(param.getClassName());
        paramsAdded = true;
      }
      builder.append('>');
      return builder.toString();
    }
    else {
      return "";
    }
  }

  private String getComponentProxyClassName() {
    return PathUtils.getFullyQualifiedIntfClassName(getPath());
  }

  private static String getUniqueName() {
    return "__jamon__var_" + uniqueId++;
  }

  private static int uniqueId = 0;

  private final List<GenericCallParam> genericParams;

  private final String callingTemplateJamonContextType;
}
