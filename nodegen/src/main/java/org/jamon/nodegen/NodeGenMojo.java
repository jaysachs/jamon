package org.jamon.nodegen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Generate AST nodes.
 *
 * @goal generate-ast-nodes
 * @phase generate-source
 */
public class NodeGenMojo extends AbstractMojo
{
    public void execute() throws MojoExecutionException
    {
        try
        {
            getLog().info("Parsing node description file");
            Iterable<NodeDescriptor> nodes =
                NodesParser.parseNodes(new FileReader(nodeDescriptionFile));
            getLog().info("Initializing destination directory");
            File target = new File(destinationDirectory, destinationPackage.replace('.', File.separatorChar));
            initializeTargetDir(target);
            getLog().info("Generating node classes");
            new NodeGenerator(destinationPackage).generateSources(nodes, target);
            getLog().info("Generating analysis adapter classes");
            AnalysisGenerator ag = new AnalysisGenerator(destinationPackage, target, nodes);
            ag.generateAnalysisInterface();
            ag.generateAnalysisAdapterClass();
            ag.generateDepthFirstAdapterClass();
        }
        catch (IOException e)
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private void initializeTargetDir(File targetDir)
    {
        destinationDirectory.mkdirs();
        File[] files = targetDir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            files[i].delete();
        }
    }
    
    /**
     * @parameter
     * @required
     */
    private File nodeDescriptionFile;
    
    /**
     * @parameter
     * @required
     */
    private File destinationDirectory;
    
    /**
     * @parameter default-value="org.jamon.nodes"
     */
    private String destinationPackage;
}
