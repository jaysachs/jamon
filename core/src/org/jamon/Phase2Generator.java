package org.modusponens.jtt;

import java.util.*;
import org.modusponens.jtt.node.*;
import org.modusponens.jtt.analysis.*;

public class Phase2Generator extends BaseGenerator
{
    private List m_body = new ArrayList();
    private StringBuffer m_current = new StringBuffer();

    public Phase2Generator(String p_packageName,String p_className)
    {
        super(p_packageName,p_className);
    }

    public void generateClassSource()
    {
        generatePrologue();
        generateImports();
        generateDeclaration();
        generateRender();
        generateOptionalArgs();
        generateEpilogue();
    }

    public void caseABodyComponent(ABodyComponent node)
    {
        m_current.append(node.getAny().getText());
    }

    public void caseAJavaComponent(AJavaComponent node)
    {
    }

    public void caseAJlineComponent(AJlineComponent node)
    {
    }

    private void handleBody()
    {
        if (m_current.length() > 0)
        {
            m_body.add("    write(\""
                       + javaEscape(m_current.toString())
                       + "\");");
            m_current = new StringBuffer();
        }
    }

    public void caseAEmitComponent(AEmitComponent node)
    {
        handleBody();
        StringBuffer expr = new StringBuffer();
        expr.append("    write(");
        for (Iterator i = node.getAny().iterator(); i.hasNext(); /* */)
        {
            expr.append(((TAny)i.next()).getText());
        }
        expr.append(");");
        m_body.add(expr);
    }

    public void caseACallComponent(ACallComponent node)
    {
    }

    public void caseEOF(EOF node)
    {
        handleBody();
    }

    private String javaEscape(String p_string)
    {
        // assert p_string != null
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < p_string.length(); ++i)
        {
            char c = p_string.charAt(i);
            if (c == '\n')
            {
                s.append("\\n");
            }
            else if (c == '\t')
            {
                s.append("\\t");
            }
            else if (c == '\"')
            {
                s.append("\\\"");
            }
            else
            {
                s.append(c);
            }
        }
        return s.toString();
    }

    private void generateDeclaration()
    {
        print("class ");
        print(getClassName());
        println("Impl");
        print("  extends ");
        println(BASE_TEMPLATE);
        println("{");
    }

    private static final String BASE_TEMPLATE =
        "org.modusponens.jtt.AbstractTemplate";

    private void generateRender()
    {
        print("  public void render(");
        for (Iterator i = getRequiredArgs(); i.hasNext(); /* */)
        {
            String name = (String) i.next();
            print(getArgType(name));
            print(" ");
            print(name);
            if (i.hasNext())
            {
                print(", ");
            }
        }
        println(")");

        println("    throws IOException");
        println("  {");
        for (Iterator i = m_body.iterator(); i.hasNext(); /* */)
        {
            println(i.next());
        }
        println("  }");
    }

    private void generateOptionalArgs()
    {
        for (Iterator i = getOptionalArgs(); i.hasNext(); /* */)
        {
            println();
            String name = (String) i.next();
            print("  public void set");
            print(capitalize(name));
            print("(");
            String type = getArgType(name);
            print(type);
            print(" p_");
            print(name);
            println(")");
            println("  {");
            print("    ");
            print(name);
            print(" = p_");
            print(name);
            println(";");
            println("  }");
            println();
            print("  private ");
            print(type);
            print(" ");
            print(name);
            print(" = ");
            print(getDefault(name));
            println(";");
        }
    }

    private void generateEpilogue()
    {
        println();
        println("}");
    }

}