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

import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jamon.ParserErrorsImpl;
import org.jamon.node.AnnotationNode;
import org.jamon.node.ClassNode;
import org.jamon.node.GenericsParamNode;
import org.jamon.node.ImportNode;
import org.jamon.node.ParentArgNode;
import org.jamon.node.StaticImportNode;
import org.jamon.util.StringUtils;

public class TemplateUnit
    extends AbstractUnit
    implements InheritedUnit
{
    public TemplateUnit(String p_path, ParserErrorsImpl p_errors)
    {
        super(p_path, null, p_errors, null);
    }

    public int getInheritanceDepth()
    {
        return m_parentDescription == null
            ? 0
            : 1 + m_parentDescription.getInheritanceDepth();
    }


    /**
     * public for unit testing purposes
     **/

    public void setParentDescription(TemplateDescription p_parent)
    {
        m_parentDescription = p_parent;
        //FIXME - join them later.
        m_fragmentArgs.addAll(p_parent.getFragmentInterfaces());

        m_inheritedArgs = new InheritedArgs(getName(),
                                            getParentPath(),
                                            p_parent.getRequiredArgs(),
                                            p_parent.getOptionalArgs(),
                                            p_parent.getFragmentInterfaces(),
                                            getErrors());

        for (AbstractArgument arg: new Concatenation<AbstractArgument>(
                p_parent.getRequiredArgs(),
                p_parent.getOptionalArgs(),
                p_parent.getFragmentInterfaces()))
        {
            addArgName(arg);
        }

        m_callNames.addAll(p_parent.getMethodUnits().keySet());
        m_abstractMethodNames.addAll(p_parent.getAbstractMethodNames());
        if (m_jamonContextType == null) {
            setJamonContextType(p_parent.getJamonContextType());
        }
    }

    public void addParentArg(ParentArgNode p_arg)
    {
        m_inheritedArgs.addParentArg(p_arg);
    }

    @Override public List<FragmentArgument> getFragmentArgs()
    {
        return m_fragmentArgs;
    }

    @Override
    public void addFragmentArg(FragmentArgument p_arg)
    {
        m_fragmentArgs.add(p_arg);
        m_declaredFragmentArgs.add(p_arg);
    }

    public Collection<FragmentArgument> getDeclaredFragmentArgs()
    {
        return m_declaredFragmentArgs;
    }

    @Override
    public void addRequiredArg(RequiredArgument p_arg)
    {
        m_declaredRequiredArgs.add(p_arg);
    }

    @Override
    public void addOptionalArg(OptionalArgument p_arg)
    {
        m_declaredOptionalArgs.add(p_arg);
    }

    @Override
    public List<RequiredArgument> getSignatureRequiredArgs()
    {
        return new SequentialList<RequiredArgument>
            (m_parentDescription.getRequiredArgs(),
             m_declaredRequiredArgs);
    }

    @Override
    public Collection<OptionalArgument> getSignatureOptionalArgs()
    {
        return new Concatenation<OptionalArgument>
            (m_parentDescription.getOptionalArgs(),
             m_declaredOptionalArgs);
    }

    public String getOptionalArgDefault(OptionalArgument p_arg)
    {
        return m_declaredOptionalArgs.contains(p_arg)
            ? p_arg.getDefault()
            : m_inheritedArgs.getDefaultValue(p_arg);
    }

    @Override
    public Collection<AbstractArgument> getVisibleArgs()
    {
        return m_inheritedArgs == null
            ? new Concatenation<AbstractArgument>(
                    getDeclaredRenderArgs(),
                    getDeclaredOptionalArgs())
            : new Concatenation<AbstractArgument>(
                    m_inheritedArgs.getVisibleArgs(),
                    getDeclaredRenderArgs(),
                    getDeclaredOptionalArgs());
    }

    public Collection<OptionalArgument> getDeclaredOptionalArgs()
    {
        return m_declaredOptionalArgs;
    }

    public Collection<String> getTemplateDependencies()
    {
        return m_dependencies;
    }

    private void checkCallName(String p_name, org.jamon.api.Location p_location)
    {
        if (! m_callNames.add(p_name))
        {
            getErrors().addError(
                "multiple defs and/or methods named " + p_name, p_location);
        }
    }

    public void makeDefUnit(String p_defName, org.jamon.api.Location p_location)
    {
        checkCallName(p_defName, p_location);
        m_defs.put(p_defName, new DefUnit(p_defName, this, getErrors(), p_location));
    }

    public Collection<DefUnit> getDefUnits()
    {
        return m_defs.values();
    }

    public DefUnit getDefUnit(String p_name)
    {
        return m_defs.get(p_name);
    }

    public void makeMethodUnit(
            String p_methodName, org.jamon.api.Location p_location, boolean p_isAbstract)
    {
        checkCallName(p_methodName, p_location);
        m_methods.put(p_methodName,
                      new DeclaredMethodUnit(
                          p_methodName, this, getErrors(), p_location, p_isAbstract));
        if(p_isAbstract)
        {
            m_abstractMethodNames.add(p_methodName);
        }
    }

    public OverriddenMethodUnit makeOverridenMethodUnit(
            String p_name, org.jamon.api.Location p_location)
    {
        DeclaredMethodUnit methodUnit = (DeclaredMethodUnit)
            m_parentDescription.getMethodUnits().get(p_name);
        if(methodUnit == null)
        {
            getErrors().addError(
                "There is no such method " + p_name + " to override",
                p_location);
            // Provide a dummy parent, to allow us to catch errors inside the
            // override
            methodUnit = new DeclaredMethodUnit(p_name, this, getErrors(), p_location);
        }

        m_abstractMethodNames.remove(p_name);
        OverriddenMethodUnit override =
            new OverriddenMethodUnit(methodUnit, this, getErrors(), p_location);
        m_overrides.add(override);
        return override;
    }

    public MethodUnit getMethodUnit(String p_name)
    {
        return (m_methods.containsKey(p_name)
               ? m_methods.get(p_name)
               : m_parentDescription.getMethodUnits().get(p_name));
    }

    public Collection<MethodUnit> getSignatureMethodUnits()
    {
        return new Concatenation<MethodUnit>
            (getDeclaredMethodUnits(), m_parentDescription.getMethodUnits().values());
    }

    public Collection<MethodUnit> getDeclaredMethodUnits()
    {
        return m_methods.values();
    }

    public Collection<MethodUnit> getImplementedMethodUnits()
    {
        return new Concatenation<MethodUnit>(getDeclaredMethodUnits(), m_overrides);
    }

    public Collection<String> getAbstractMethodNames()
    {
        return m_abstractMethodNames;
    }

    private Iterable<ImportNode> getImports()
    {
        return m_imports;
    }

    public void addStaticImport(StaticImportNode p_node)
    {
        m_staticImports.add(p_node);
    }

    private Iterable<StaticImportNode> getStaticImports()
    {
        return m_staticImports;
    }

    public void addImport(ImportNode p_node)
    {
        m_imports.add(p_node);
    }

    public void addInterface(String p_interface)
    {
        m_interfaces.add(p_interface);
    }

    public void setParentPath(String p_parentPath)
    {
        m_parentPath = p_parentPath;
        m_dependencies.add(m_parentPath);
    }

    public String getParentPath()
    {
        return m_parentPath;
    }

    public boolean hasParentPath()
    {
        return m_parentPath != null;
    }

    public boolean isParent()
    {
        return m_isParent;
    }

    public void setIsParent()
    {
        m_isParent = true;
    }

    public void printClassContent(CodeWriter p_writer)
    {
        for (ClassNode node : m_classContent)
        {
            p_writer.printLocation(node.getLocation());
            p_writer.println(node.getContent());
        }
    }

    public void addClassContent(ClassNode p_node)
    {
        m_classContent.add(p_node);
    }

    public void addCallPath(String p_callPath)
    {
        m_dependencies.add(p_callPath);
    }

    private InheritedArgs m_inheritedArgs;
    private TemplateDescription m_parentDescription =
        TemplateDescription.EMPTY;
    private final List<RequiredArgument> m_declaredRequiredArgs =
        new LinkedList<RequiredArgument>();
    private final List<FragmentArgument> m_fragmentArgs =
        new LinkedList<FragmentArgument>();
    private final Set<OptionalArgument> m_declaredOptionalArgs =
        new HashSet<OptionalArgument>();
    private final Set<FragmentArgument> m_declaredFragmentArgs =
        new HashSet<FragmentArgument>();

    private final Map<String, DefUnit> m_defs = new HashMap<String, DefUnit>();
    private final Map<String, MethodUnit> m_methods =
        new HashMap<String, MethodUnit>();
    private final List<OverriddenMethodUnit> m_overrides =
        new LinkedList<OverriddenMethodUnit>();
    private final List<ImportNode> m_imports = new LinkedList<ImportNode>();
    private final List<StaticImportNode> m_staticImports =
        new LinkedList<StaticImportNode>();
    private final List<String> m_interfaces = new LinkedList<String>();
    private String m_parentPath;
    private boolean m_isParent = false;
    private final List<ClassNode> m_classContent = new LinkedList<ClassNode>();
    private final Set<String> m_dependencies = new HashSet<String>();
    private final Set<String> m_callNames = new HashSet<String>();
    private final Collection<String> m_abstractMethodNames =
        new HashSet<String>();
    private final GenericParams m_genericParams = new GenericParams();
    private String m_jamonContextType;
    private final List<AnnotationNode> m_annotations = new LinkedList<AnnotationNode>();

    public Collection<RequiredArgument> getParentRenderArgs()
    {
        return new Concatenation<RequiredArgument>
            (m_parentDescription.getRequiredArgs(),
             m_parentDescription.getFragmentInterfaces());
    }

    public void printImports(CodeWriter p_writer)
    {
        for (ImportNode node : getImports())
        {
            p_writer.printLocation(node.getLocation());
            p_writer.println("import " + node.getName() + ";");
        }
        for (StaticImportNode node : getStaticImports())
        {
            p_writer.printLocation(node.getLocation());
            p_writer.println("import static " + node.getName() + ";");
        }
        p_writer.println();
    }

    public void printParentRenderArgs(CodeWriter p_writer)
    {
        printArgs(p_writer, getParentRenderArgs());
    }

    public void printParentRenderArgsDecl(CodeWriter p_writer)
    {
        printArgsDecl(p_writer, getParentRenderArgs());
    }

    public Collection<? extends RequiredArgument> getDeclaredRenderArgs()
    {
        return new Concatenation<RequiredArgument>(
            m_declaredRequiredArgs, m_declaredFragmentArgs);
    }

    public Collection<AbstractArgument> getDeclaredArgs()
    {
        return new Concatenation<AbstractArgument>( getDeclaredRenderArgs(), m_declaredOptionalArgs);
    }

    public void printDeclaredRenderArgs(CodeWriter p_writer)
    {
        printArgs(p_writer, getDeclaredRenderArgs());
    }

    public void printDeclaredRenderArgsDecl(CodeWriter p_writer)
    {
        printArgsDecl(p_writer, getDeclaredRenderArgs());
    }

    public void printInterfaces(CodeWriter p_writer)
    {
        if (m_interfaces.size() > 0)
        {
            p_writer.print("  implements ");
            p_writer.openList("", false);
            for (String intrface: m_interfaces)
            {
                p_writer.printListElement(intrface);
            }
            p_writer.closeList("");
        }
    }

    @Override
    protected void generateInterfaceSummary(StringBuilder p_buf)
    {
        super.generateInterfaceSummary(p_buf);
        p_buf.append("GenericParams:");
        p_buf.append(getGenericParams().generateGenericsDeclaration());
        p_buf.append("\n");
        if(m_parentDescription != null)
        {
            p_buf.append("Parent sig: ");
            p_buf.append(m_parentDescription.getSignature());
            p_buf.append("\n");
        }
        for(FragmentArgument arg: getFragmentArgs())
        {
            p_buf.append("Fragment: ");
            p_buf.append(arg.getName());
            p_buf.append("\n");
            arg.getFragmentUnit().generateInterfaceSummary(p_buf);
        }
    }

    public String getSignature()
    {
        try
        {
            StringBuilder buf = new StringBuilder();
            generateInterfaceSummary(buf);
            return StringUtils.byteArrayToHexString
                (MessageDigest.getInstance("MD5").digest
                     (buf.toString().getBytes("UTF-8")));
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to generate signature", e);
        }
    }

    public GenericParams getGenericParams() { return m_genericParams; }

    public void setJamonContextType(String p_jamonContextType)
    {
        m_jamonContextType = p_jamonContextType;
    }

    public String getJamonContextType() { return m_jamonContextType; }

    public boolean isOriginatingJamonContext()
    {
        return m_jamonContextType != null
            && (! hasParentPath()
                || m_parentDescription.getJamonContextType() == null);
    }

    public void addGenericsParamNode(GenericsParamNode p_node)
    {
        m_genericParams.addParam(p_node);
    }

    public void addAnnotationNode(AnnotationNode p_node)
    {
        m_annotations.add(p_node);
    }

    public Iterable<AnnotationNode> getAnnotations() { return m_annotations; }
}
