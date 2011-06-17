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

import java.io.OutputStream;

import org.jamon.compiler.ParserErrorImpl;
import org.jamon.compiler.ParserErrorsImpl;

public class ImplGenerator extends AbstractSourceGenerator
{
    public ImplGenerator(TemplateDescriber p_describer,
                         TemplateUnit p_templateUnit)
    {
        super(p_describer, p_templateUnit);
    }

    @Override
    public void generateSource(OutputStream p_out)
        throws java.io.IOException
    {
        m_writer = new CodeWriter(p_out);
        try
        {
            generateHeader();
            generatePrologue();
            generateImports();
            generateDeclaration();
            generateSetOptionalArguments();
            generateConstructor();
            generateRender();
            generateDefs();
            generateMethods();
            generateEpilogue();
            m_writer.finish();
        }
        catch (ParserErrorImpl e)
        {
            throw new ParserErrorsImpl(e);
        }
    }

    private final String getPath()
    {
        return m_templateUnit.getName();
    }

    private String getClassName()
    {
        return PathUtils.getImplClassName(getPath());
    }

    private void generateHeader()
    {
        m_writer.println("// Autogenerated Jamon implementation");
        m_writer.println("// "
                         + m_describer.getExternalIdentifier(
                             getPath()).replace('\\','/'));
        m_writer.println();
    }

    private void generateDeclaration()
    {
        generateCustomAnnotations(m_templateUnit.getAnnotations(), AnnotationType.IMPL);
        m_writer.print("public");
        if(m_templateUnit.isParent())
        {
            m_writer.print(" abstract");
        }
        m_writer.println(
            " class " + getClassName()
            + m_templateUnit.getGenericParams().generateGenericsDeclaration());
        m_writer.println("  extends "
                         + (m_templateUnit.hasParentPath()
                            ? PathUtils.getFullyQualifiedImplClassName(
                                m_templateUnit.getParentPath())
                            : ClassNames.BASE_TEMPLATE));
        m_writer.println(
            "  implements " + getProxyClassName() + ".Intf"
            + m_templateUnit.getGenericParams().generateGenericParamsList());
        m_writer.println();
        m_writer.openBlock();
        if (m_templateUnit.getJamonContextType() != null)
        {
            m_writer.println(
                "protected final " + m_templateUnit.getJamonContextType() +
                " jamonContext;");
        }
        for (AbstractArgument arg: m_templateUnit.getVisibleArgs())
        {
            m_writer.println(
                "private final " + arg.getType() + " " + arg.getName() + ";");
        }
        m_templateUnit.printClassContent(m_writer);
    }

    private void generateSetOptionalArguments()
    {
        m_writer.println(
            "protected static "
            + m_templateUnit.getGenericParams().generateGenericsDeclaration()
            + getImplDataClassName()
            + " " + SET_OPTS + "("
            + getImplDataClassName() + " p_implData)");
        m_writer.openBlock();
        for (OptionalArgument arg: m_templateUnit.getSignatureOptionalArgs())
        {
            String value = m_templateUnit.getOptionalArgDefault(arg);
            if (value != null)
            {
                m_writer.println(
                    "if(! p_implData." + arg.getIsNotDefaultName() + "())");
                m_writer.openBlock();
                m_writer.println("p_implData." + arg.getSetterName() + "("
                                 + value + ");");
                m_writer.closeBlock();
            }
        }
        if (m_templateUnit.hasParentPath())
        {
            m_writer.println(getParentImplClassName() + "."
                             + SET_OPTS + "(p_implData);");
        }
        m_writer.println("return p_implData;");
        m_writer.closeBlock();
    }

    private void generateConstructor()
    {
        m_writer.println("public " +  getClassName()
                         + "(" + ClassNames.TEMPLATE_MANAGER
                         + " p_templateManager, "
                         + getImplDataClassName() + " p_implData)");
        m_writer.openBlock();
        m_writer.println(
            "super(p_templateManager, " + SET_OPTS + "(p_implData));");
        if (m_templateUnit.getJamonContextType() != null)
        {
            m_writer.println("jamonContext = p_implData.getJamonContext();");
        }
        for (AbstractArgument arg: m_templateUnit.getVisibleArgs())
        {
            m_writer.println(arg.getName()
                             + " = p_implData." + arg.getGetterName() + "();");
        }
        m_writer.closeBlock();
        m_writer.println();
    }

    private void generatePrologue()
    {
        String pkgName = PathUtils.getImplPackageName(getPath());
        if (pkgName.length() > 0)
        {
            m_writer.println("package " + pkgName + ";");
            m_writer.println();
        }
    }

    private void generateInnerUnitFargInterface(
        FragmentUnit p_fragmentUnit, boolean p_private)
    {
        m_writer.println();
        m_writer.printLocation(p_fragmentUnit.getLocation());
        p_fragmentUnit.printInterface(m_writer,
                                      p_private ? "private" : "protected",
                                      false);
    }


    private void generateDefs() throws ParserErrorImpl
    {
        for (DefUnit defUnit: m_templateUnit.getDefUnits())
        {
            for (FragmentArgument frag: defUnit.getFragmentArgs())
            {
                generateInnerUnitFargInterface(frag.getFragmentUnit(), true);
            }

            m_writer.println();
            m_writer.printLocation(defUnit.getLocation());
            m_writer.print("private void __jamon_innerUnit__");
            m_writer.print(defUnit.getName());
            m_writer.openList();
            m_writer.printListElement(ArgNames.ANNOTATED_WRITER_DECL);
            defUnit.printRenderArgsDecl(m_writer);
            m_writer.closeList();
            m_writer.println();
            if (defUnit.canThrowIOException())
            {
                m_writer.println("  throws " + ClassNames.IOEXCEPTION);
            }
            defUnit.generateRenderBody(m_writer, m_describer);
            m_writer.println();
        }
    }

    private void generateMethods() throws ParserErrorImpl
    {
        for (MethodUnit methodUnit: m_templateUnit.getDeclaredMethodUnits())
        {
            generateMethodIntf(methodUnit);
        }
        for (MethodUnit methodUnit: m_templateUnit.getImplementedMethodUnits())
        {
            generateMethodImpl(methodUnit);
        }
    }

    private void generateMethodIntf(MethodUnit p_methodUnit)
    {
        m_writer.println();
        for (FragmentArgument frag: p_methodUnit.getFragmentArgs())
        {
            generateInnerUnitFargInterface(frag.getFragmentUnit(), false);
        }

    }

    private void generateMethodImpl(MethodUnit p_methodUnit) throws ParserErrorImpl
    {
        //FIXME - cut'n'pasted from generateDefs
        m_writer.println();
        m_writer.printLocation(p_methodUnit.getLocation());
        if (p_methodUnit.isOverride())
        {
           m_writer.print("@Override ");
        }
        m_writer.print("protected "
                       + (p_methodUnit.isAbstract() ? "abstract " : "")
                       + "void __jamon_innerUnit__");
        m_writer.print(p_methodUnit.getName());
        m_writer.openList();
        m_writer.printListElement(ArgNames.ANNOTATED_WRITER_DECL);
        p_methodUnit.printRenderArgsDecl(m_writer);
        m_writer.closeList();
        m_writer.println();
        if (p_methodUnit.canThrowIOException())
        {
            m_writer.println("  throws " + ClassNames.IOEXCEPTION);
        }
        if (p_methodUnit.isAbstract())
        {
            m_writer.println("  ;");
        }
        else
        {
            p_methodUnit.generateRenderBody(m_writer, m_describer);
        }
        m_writer.println();

        for (OptionalArgument arg:  p_methodUnit.getOptionalArgsWithDefaults())
        {
            if (p_methodUnit.isOverride()) {
                m_writer.print("@Override ");
            }
            m_writer.println("protected " + arg.getType() + " "
                             + p_methodUnit.getOptionalArgDefaultMethod(arg)
                             + "()");
            m_writer.openBlock();
            m_writer.println(
                "return " + p_methodUnit.getDefaultForArg(arg) + ";");
            m_writer.closeBlock();
        }
    }

    private void generateRender() throws ParserErrorImpl
    {
        if (m_templateUnit.hasParentPath())
        {
            m_writer.println(
                "@Override protected void child_render_"
                + m_templateUnit.getInheritanceDepth()
                + "("  + ArgNames.ANNOTATED_WRITER_DECL + ")");
        }
        else
        {
            m_writer.println("public void renderNoFlush("
                             + ArgNames.ANNOTATED_WRITER_DECL + ")");
        }
        if (m_templateUnit.canThrowIOException())
        {
            m_writer.println("  throws " + ClassNames.IOEXCEPTION);
        }
        m_templateUnit.generateRenderBody(m_writer, m_describer);

        m_writer.println();
        if (m_templateUnit.isParent())
        {
            m_writer.println("protected abstract void child_render_"
                             + (m_templateUnit.getInheritanceDepth() + 1)
                             + "("  + ArgNames.WRITER_DECL + ") throws "
                             + ClassNames.IOEXCEPTION
                             + ";");
            m_writer.println();
        }
    }

    private void generateEpilogue()
    {
        m_writer.println();
        m_writer.closeBlock();
    }

    private void generateImports()
    {
        m_templateUnit.printImports(m_writer);
    }

    private String getProxyClassName()
    {
        return PathUtils.getFullyQualifiedIntfClassName(getPath());
    }

    private String getImplDataClassName()
    {
        return getProxyClassName() + ".ImplData"
            + m_templateUnit.getGenericParams().generateGenericParamsList();
    }

    private String getParentImplClassName()
    {
        return PathUtils.getFullyQualifiedImplClassName(
            m_templateUnit.getParentPath());
    }


    private final static String SET_OPTS = "__jamon_setOptionalArguments";
}
