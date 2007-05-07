package org.jamon.nodegen;

import java.io.PrintWriter;

public class AnalysisGenerator
{
    private AnalysisGenerator() {}

    public static void generateAnalysisInterface(
        PrintWriter p_writer, Iterable<NodeDescriptor> p_nodes)
    {
        p_writer.println("package org.jamon.node;");
        p_writer.println("public interface Analysis");
        p_writer.println("{");
        for (NodeDescriptor node : p_nodes)
        {
            String name = node.getName();
            p_writer.println("    void case" + name + "(" + name + " p_node);");
        }
        p_writer.println("}");
        p_writer.close();
    }

    public static void generateAnalysisAdapterClass(
        PrintWriter p_writer, Iterable<NodeDescriptor> p_nodes)
    {
        p_writer.println("package org.jamon.node;");
        p_writer.println("public class AnalysisAdapter implements Analysis");
        p_writer.println("{");
        for (NodeDescriptor node : p_nodes)
        {
            String name = node.getName();
            p_writer.println(
                "  public void case" + name + "(" + name + " p_node) {}");
        }
        p_writer.println("}");
        p_writer.close();
    }

    public static void generateDepthFirstAdapterClass(
        PrintWriter p_writer, Iterable<NodeDescriptor> p_nodes)
    {
        p_writer.println("package org.jamon.node;");
        p_writer.println(
            "public class DepthFirstAnalysisAdapter implements Analysis");
        p_writer.println("{");
        for (NodeDescriptor node : p_nodes)
        {
            String name = node.getName();
            p_writer.println(
                "  public void in" + name
                + "(@SuppressWarnings(\"unused\") " + name + " p_node) {}");
            p_writer.println(
                "  public void out" + name
                + "(@SuppressWarnings(\"unused\") " + name + " p_node) {}");
            p_writer.println(
                "  public void case" + name + "(" + name + " p_node)");
            p_writer.println("  {");
            p_writer.println("    in" + name + "(p_node);");
            for (NodeMember member : node.getAllMembers())
            {
                if (member.isNode())
                {
                    if (member.isList())
                    {
                        p_writer.println(
                            "    for (AbstractNode node : p_node."
                            + member.getGetter() + ")");
                        p_writer.println("    {");
                        p_writer.println("      node.apply(this);");
                        p_writer.println("    }");
                    }
                    else
                    {
                        p_writer.println("    p_node." + member.getGetter()
                                         + ".apply(this);");
                    }
                }
            }
            p_writer.println("    out" + name + "(p_node);");
            p_writer.println("  }");
            p_writer.println();
        }
        p_writer.println("}");
        p_writer.close();
    }

}