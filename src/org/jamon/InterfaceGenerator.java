package org.modusponens.jtt;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import org.modusponens.jtt.node.*;
import org.modusponens.jtt.analysis.*;

public class InterfaceGenerator extends BaseGenerator
{
    private final static String TEMPLATE =
        Template.class.getName();
    private final static String BASE_FACTORY =
        AbstractTemplateFactory.class.getName();
    private final static String TEMPLATE_MANAGER =
        TemplateManager.class.getName();
    private final static String JTT_EXCEPTION =
        JttException.class.getName();

    public InterfaceGenerator(Writer p_writer,
                              String p_templatePath,
                              String p_packageName,
                              String p_className)
    {
        super(p_writer,p_packageName,p_className);
        m_templatePath = p_templatePath;
    }

    private final String m_templatePath;

    public void generateClassSource()
        throws IOException
    {
        generatePrologue();
        generateImports();
        generateDeclaration();
        generateFactoryClass();
        generateRender();
        generateOptionalArgs();
        generateRequiredArgsField();
        generateEpilogue();
    }

    private void generateRequiredArgsField()
        throws IOException
    {
        println();
        println("  public static final String[] RENDER_ARGS = {");
        for (Iterator i = getRequiredArgNames(MAIN_UNIT_NAME);
             i.hasNext();
             /* */)
        {
            print("    \"");
            print(i.next());
            print("\"");
            println(i.hasNext() ? "," : "");
        }
        println("  };");
    }

    private void generateFactoryClass()
        throws IOException
    {
        println("  public static class Factory");
        print  ("    extends ");
        println(BASE_FACTORY);
        println("  {");
        print  ("    public Factory(");
        print  (TEMPLATE_MANAGER);
        println(" p_templateManager)");
        println("    {");
        println("      super(p_templateManager);");
        println("    }");
        println();
        print  ("    public ");
        print  (getClassName());
        println(" getInstance(java.io.Writer p_writer)");
        print  ("      throws ");
        println(JTT_EXCEPTION);
        println("    {");
        print  ("      return (");
        print  (getClassName());
        println(") ");
        print  ("        getInstance(\"");
        print  ("/");
        print  (m_templatePath);
        println("\", p_writer);");
        println("    }");
        println("  }");
        println();
    }

    private void generateDeclaration()
        throws IOException
    {
        print("public interface ");
        println(getClassName());
        print("  extends ");
        println(TEMPLATE);
        println("{");
    }
    private void generateRender()
        throws IOException
    {
        print("  public void render(");
        for (Iterator i = getRequiredArgNames(MAIN_UNIT_NAME);
             i.hasNext();
             /* */)
        {
            String name = (String) i.next();
            print(getArgType(MAIN_UNIT_NAME,name));
            print(" p_");
            print(name);
            if (i.hasNext())
            {
                print(", ");
            }
        }
        println(")");

        println("    throws java.io.IOException;");
    }

    private void generateOptionalArgs()
        throws IOException
    {
        for (Iterator i = getOptionalArgNames(MAIN_UNIT_NAME);
             i.hasNext();
             /* */)
        {
            println();
            String name = (String) i.next();
            print("  public ");
            print(getPackageName());
            print(".");
            print(getClassName());
            print(" set");
            print(capitalize(name));
            print("(");
            print(getArgType(MAIN_UNIT_NAME,name));
            print(" p_");
            print(name);
            println(");");
        }
    }

    private void generateEpilogue()
        throws IOException
    {
        println();
        println("}");
    }
}
