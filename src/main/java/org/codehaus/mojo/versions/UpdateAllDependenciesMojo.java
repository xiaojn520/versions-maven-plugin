package org.codehaus.mojo.versions;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Update the dependencies to newest versions.
 *
 * @goal update-all-dependencies
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class UpdateAllDependenciesMojo
    extends AbstractVersionsDependencyUpdaterMojo
{

    // ------------------------------ FIELDS ------------------------------

    /**
     * Pattern to match a snapshot version.
     */
    public final Pattern matchSnapshotRegex = Pattern.compile( "^(.+)-((SNAPSHOT)|(\\d{8}\\.\\d{6}-\\d+))$" );

    // ------------------------------ METHODS --------------------------

    /**
     * @param pom the pom to update.
     * @throws org.apache.maven.plugin.MojoExecutionException when things go wrong
     * @throws org.apache.maven.plugin.MojoFailureException when things go wrong in a very bad way
     * @throws javax.xml.stream.XMLStreamException when things go wrong with XML streaming
     * @see org.codehaus.mojo.versions.AbstractVersionsUpdaterMojo#modifyPomFile(org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader)
     */
    protected void update( ModifiedPomXMLEventReader pom)
        throws MojoExecutionException, MojoFailureException, XMLStreamException, ArtifactMetadataRetrievalException
    {
        try
        {
            if ( isProcessingDependencies() )
            {
            	Collection dependencies = getProject().getDependencies();
            	Iterator currentDependency = dependencies.iterator();

                while ( currentDependency.hasNext() )
                {
                    Dependency dep = (Dependency) currentDependency.next();

                    if ( isExcludeReactor() && isProducedByReactor( dep ) )
                    {
                        getLog().info( "Skip reactor dependency: " + toString( dep ) );
                        continue;
                    }

                    String version = dep.getVersion();
                    Matcher versionMatcher = matchSnapshotRegex.matcher( version );
                    if ( !versionMatcher.matches() )
                    {
                        Artifact artifact = this.toArtifact( dep );
                        if ( !isIncluded( artifact ) )
                        {
                            continue;
                        }
                        ArtifactVersions versions = getHelper().lookupArtifactVersions( artifact, false );
                        ArtifactVersion[] newer = versions.getNewerVersions( version, -1, false );
                        if ( newer.length > 0 )
                        {
                            String newVersion = newer[newer.length - 1].toString();
                            if ( PomHelper.setDependencyVersion( pom, dep.getGroupId(), dep.getArtifactId(), version,
                                                                 newVersion ) )
                            {
                                getLog().info( "Updated " + toString( dep ) + " to version " + newVersion );
                            }
                        }
                    }
                }
            }
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}