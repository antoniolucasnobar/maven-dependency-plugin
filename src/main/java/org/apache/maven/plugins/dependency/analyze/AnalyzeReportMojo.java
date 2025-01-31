package org.apache.maven.plugins.dependency.analyze;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalysis;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzer;
import org.apache.maven.shared.dependency.analyzer.ProjectDependencyAnalyzerException;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Analyzes the dependencies of this project and produces a report that summarizes which are: used and declared; used
 * and undeclared; unused and declared.
 *
 * @since 2.0-alpha-5
 */
@Mojo( name = "analyze-report", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
@Execute( phase = LifecyclePhase.TEST_COMPILE )
public class AnalyzeReportMojo
    extends AbstractMavenReport
{
    // fields -----------------------------------------------------------------

    /**
     * The Maven project dependency analyzer to use.
     */
    @Component
    private ProjectDependencyAnalyzer analyzer;

    /**
     * Ignore Runtime/Provided/Test/System scopes for unused dependency analysis
     *
     * @since 2.2
     */
    @Parameter( property = "ignoreNonCompile", defaultValue = "false" )
    private boolean ignoreNonCompile;

    /**
     * Force dependencies as used, to override incomplete result caused by bytecode-level analysis. Dependency format is
     * <code>groupId:artifactId</code>.
     *
     * @since 2.6
     */
    @Parameter
    private String[] usedDependencies;

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "mdep.analyze.skip", defaultValue = "false" )
    private boolean skip;

    // Mojo methods -----------------------------------------------------------

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        // Step 1: Analyze the project
        ProjectDependencyAnalysis analysis;
        try
        {
            analysis = analyzer.analyze( project );

            if ( usedDependencies != null )
            {
                analysis = analysis.forceDeclaredDependenciesUsage( usedDependencies );
            }
        }
        catch ( ProjectDependencyAnalyzerException exception )
        {
            throw new MavenReportException( "Cannot analyze dependencies", exception );
        }

        // remove everything that's not in the compile scope
        if ( ignoreNonCompile )
        {
            analysis = analysis.ignoreNonCompile();
        }

        // Step 2: Create sink and bundle
        Sink sink = getSink();
        ResourceBundle bundle = getBundle( locale );

        // Step 3: Generate the report
        AnalyzeReportView analyzethis = new AnalyzeReportView();
        analyzethis.generateReport( analysis, sink, bundle );
    }

    // MavenReport methods ----------------------------------------------------


    @Override
    public boolean canGenerateReport()
    {
        if ( skip )
        {
            getLog().info( "Skipping plugin execution" );
            return false;
        }

        // Step 0: Checking pom availability
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping pom project" );
            return false;
        }

        return true;
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputName()
     */
    @Override
    public String getOutputName()
    {
        return "dependency-analysis";
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getName(java.util.Locale)
     */
    @Override
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "analyze.report.name" );
    }

    /*
     * @see org.apache.maven.reporting.AbstractMavenReport#getDescription(java.util.Locale)
     */
    @Override
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "analyze.report.description" );
    }

    // protected methods ------------------------------------------------------

    /**
     * @param locale the current locale
     * @return The resource bundle {@link ResourceBundle}
     */
    protected ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "analyze-report", locale, this.getClass().getClassLoader() );
    }
}
