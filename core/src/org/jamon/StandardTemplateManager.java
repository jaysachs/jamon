package org.jamon;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.jamon.node.Start;


public class StandardTemplateManager
    implements TemplateManager
{
    public Template getInstance(String p_path, Writer p_writer)
        throws JamonException
    {
        return getInstance(p_path, p_writer, this);
    }

    public Template getInstance(String p_path,
                                Writer p_writer,
                                TemplateManager p_manager)
        throws JamonException
    {
        try
        {
            initialize();
            AbstractTemplateImpl template = (AbstractTemplateImpl)
                getImplementationClass(p_path)
                    .getConstructor(new Class [] { TemplateManager.class })
                    .newInstance(new Object [] { p_manager });
            template.initialize(p_writer);
            return template;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new JamonException(e);
        }
    }

    public void setClassLoader(ClassLoader p_classLoader)
    {
        m_classLoader = p_classLoader;
        m_initialized = false;
    }

    public void setSourceDir(String p_templateSourceDir)
    {
        m_templateSourceDir = p_templateSourceDir;
        m_initialized = false;
    }

    public void setWorkDir(String p_workDir)
    {
        m_workDir = p_workDir;
        m_initialized = false;
    }

    public void setJavaCompiler(String p_javac)
    {
        m_javac = p_javac;
        m_javaCompiler = null;
    }

    public void setJavaCompilerNeedsRtJar(boolean p_includeRtJar)
    {
        m_includeRtJar = p_includeRtJar;
        m_javaCompiler = null;
    }

    public void setClasspath(String p_classpath)
    {
        m_classpath = p_classpath;
        m_javaCompiler = null;
    }


    private synchronized void initialize()
        throws IOException
    {
        if (! m_initialized)
        {
            System.err.println("initializing std template mgr");
            String workDir = m_workDir;
            if (workDir == null)
            {
                File tmpdir = File.createTempFile("jamon",null);
                tmpdir.mkdirs();
                workDir = tmpdir.toString();
            }
            m_loader = new WorkDirClassLoader(m_classLoader, workDir);
            m_describer =
                new TemplateDescriber(m_templateSourceDir == null
                                      ? System.getProperty("user.dir")
                                      : m_templateSourceDir);
            m_initialized = true;
        }
    }

    private String getClassName(String p_path)
    {
        return StringUtils.pathToClassName(p_path) + "Impl";
    }

    private Class loadAndResolveClass(String p_path)
        throws ClassNotFoundException
    {
        return m_loader.loadClass(getClassName(p_path));
    }

    private String getClassPath()
    {
        StringBuffer cp = new StringBuffer(m_workDir);
        if (m_classpath != null)
        {
            cp.append(PS);
            cp.append(m_classpath);
        }

        ClassLoader loader = getClass().getClassLoader();
        if (loader instanceof URLClassLoader)
        {
            URL[] urls = ((URLClassLoader)loader).getURLs();
            for (int i = 0; i < urls.length; ++i)
            {
                String url = urls[i].toExternalForm();
                if (url.startsWith("file:"))
                {
                    cp.append(PS);
                    cp.append(url.substring(5));
                }
            }
        }
        else
        {
            cp.append(PS);
            cp.append(System.getProperty("java.class.path"));
        }

        if (m_includeRtJar)
        {
            cp.append(PS);
            cp.append(getRtJarPath());
        }

        System.err.print("Jamon compilation CLASSPATH is ");
        System.err.println(cp);

        return cp.toString();
    }

    private String getRtJarPath()
    {
        StringBuffer path = new StringBuffer(System.getProperty("java.home"));
        path.append(FS);
        path.append("lib");
        path.append(FS);
        path.append("rt.jar");
        return path.toString();
    }

    private JavaCompiler getJavaCompiler()
    {
        if (m_javaCompiler == null)
        {
            m_javaCompiler = new JavaCompiler(m_javac, getClassPath());
        }
        return m_javaCompiler;
    }

    private Class getImplementationClass(String p_path)
        throws IOException,
               ClassNotFoundException
    {

        ensureUpToDate(p_path);
        return loadAndResolveClass(p_path);
    }

    private static final String PS = System.getProperty("path.separator");
    private static final String FS = System.getProperty("file.separator");

    private TemplateDescriber m_describer;
    private String m_workDir;
    private String m_templateSourceDir;
    private String m_javac =
        System.getProperty("java.home") + FS + ".." + FS + "bin" + FS +"javac";
    private boolean m_includeRtJar = false;
    private String m_classpath = null;
    private ClassLoader m_classLoader = ClassLoader.getSystemClassLoader();
    private JavaCompiler m_javaCompiler;
    private WorkDirClassLoader m_loader;
    private boolean m_initialized;

    private String prefix()
    {
        return m_workDir;
    }

    private String javaImpl(String p_path)
    {
        return prefix() + p_path + "Impl.java";
    }

    private String classImpl(String p_path)
    {
        return prefix() + p_path + "Impl.class";
    }

    private String javaIntf(String p_path)
    {
        return prefix() + p_path + ".java";
    }

    private String classIntf(String p_path)
    {
        return prefix() + p_path + ".class";
    }

    private synchronized void ensureUpToDate(String p_path)
        throws IOException
    {
        Collection seen = new HashSet();
        Collection outOfDateJavaFiles = new HashSet();
        List workQueue = new LinkedList();
        workQueue.add(p_path);

        while (!workQueue.isEmpty())
        {
            String path = (String) workQueue.remove(0);
            System.err.println("processing " + path);
            seen.add(path);

            File tf = m_describer.getTemplateFile(path);
            if (!tf.exists())
            {
                System.err.println(path
                                   + " source not found; assume class exists");
                continue;
            }

            File jm = new File(javaImpl(path));
            File ji = new File(javaIntf(path));
            long ts = Math.min(jm.lastModified(),ji.lastModified());
            if (jm.lastModified() < tf.lastModified()
                || ji.lastModified() < tf.lastModified())
            {
                generateIntf(path);
                m_dependencyCache.put(path,
                                      new DependencyEntry(generateImpl(path)));
                ts = System.currentTimeMillis();

            }
            File cm = new File(classImpl(path));
            File ci = new File(classIntf(path));
            if (cm.lastModified() < ts || ci.lastModified() < ts)
            {
                outOfDateJavaFiles.add(javaImpl(path));
                outOfDateJavaFiles.add(javaIntf(path));
            }

            DependencyEntry d = (DependencyEntry) m_dependencyCache.get(path);
            if (d == null || d.lastUpdated() < tf.lastModified())
            {
                d = new DependencyEntry(computeDependencies(path));
                m_dependencyCache.put(path, d);
            }
            for (Iterator y = d.getDependencies(); y.hasNext(); /* */)
            {
                String dp = (String) y.next();
                if (! seen.contains(dp))
                {
                    workQueue.add(dp);
                }
            }
        }

        if (!outOfDateJavaFiles.isEmpty())
        {
            compile(outOfDateJavaFiles);
            m_loader.invalidate();
        }
    }


    private File getWriteableFile(String p_filename)
        throws IOException
    {
        File file = new File(p_filename);
        File parent = file.getParentFile();
        if (parent != null)
        {
            parent.mkdirs();
        }
        return file;
    }

    /**
     * @return dependencies
     */
    private Collection generateImpl(String p_path)
        throws IOException
    {
        System.err.println("generating impl for " + p_path);

        ImplAnalyzer ia =
            new ImplAnalyzer(p_path,
                             m_describer.parseTemplate(p_path));

        File javaFile = getWriteableFile(javaImpl(p_path));
        FileWriter writer = new FileWriter(javaFile);
        try
        {
            new ImplGenerator(writer,
                              new TemplateResolver(),
                              m_describer,
                              ia)
                .generateSource();
            writer.close();
            return ia.getCalledTemplateNames();
        }
        catch (IOException e)
        {
            writer.close();
            javaFile.delete();
            throw e;
        }
    }

    private void generateIntf(String p_path)
        throws IOException
    {
        System.err.println("generating intf for " + p_path);

        BaseAnalyzer bg =
            new BaseAnalyzer(m_describer.parseTemplate(p_path));

        File javaFile = getWriteableFile(javaIntf(p_path));
        FileWriter writer = new FileWriter(javaFile);
        try
        {
            new IntfGenerator(new TemplateResolver(),
                              p_path,
                              bg,
                              writer)
                .generateClassSource();
            writer.close();
        }
        catch (IOException e)
        {
            writer.close();
            javaFile.delete();
            throw e;
        }
    }

    private void compile(Collection p_sourceFiles)
        throws IOException
    {
        if (p_sourceFiles.isEmpty())
        {
            return;
        }

        System.err.print("compiling: ");
        for (Iterator i = p_sourceFiles.iterator(); i.hasNext(); /* */)
        {
            System.err.print(i.next());
            if (i.hasNext())
            {
                System.err.print(", ");
            }
        }
        System.err.println();
        getJavaCompiler()
            .compile((String []) p_sourceFiles.toArray(new String [0]));
    }

    private Collection computeDependencies(String p_path)
        throws IOException
    {
        System.err.println("computing dependencies for " + p_path);

        return new ImplAnalyzer(p_path,
                                m_describer.parseTemplate(p_path))
            .getCalledTemplateNames();
    }


    private Map m_dependencyCache = new HashMap();

    private static class DependencyEntry
    {
        DependencyEntry(Collection p_dependencies)
        {
            m_dependencies = p_dependencies;
            m_lastUpdated = System.currentTimeMillis();
        }

        Collection m_dependencies;
        long m_lastUpdated;

        Iterator getDependencies()
        {
            return m_dependencies.iterator();
        }

        long lastUpdated()
        {
            return m_lastUpdated;
        }
    }

}
