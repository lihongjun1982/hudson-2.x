package org.jvnet.hudson.maven3.launcher;


import org.apache.maven.execution.ExecutionListener;
import org.eclipse.hudson.legacy.maven3.interceptor.listeners.HudsonMavenExecutionResult;


/**
 * Exists solely for backward compatibility
 * @author Winston Prakash
 * @see org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher
 */
public class Maven3Launcher  {

    public static ExecutionListener getMavenExecutionListener() {
        return org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher 
.getMavenExecutionListener();
    }

    public static void setMavenExecutionListener( ExecutionListener listener ) {
    	org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher.setMavenExecutionListener(listener);
    }

    public static HudsonMavenExecutionResult getMavenExecutionResult() {
        return org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher.getMavenExecutionResult();
    }

    public static void setMavenExecutionResult( HudsonMavenExecutionResult result ) {
    	org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher.setMavenExecutionResult(result);
    }

    public static int main( String[] args ) throws Exception {
    	return org.eclipse.hudson.legacy.maven3.interceptor.launcher.Maven3Launcher.main(args);
    }

}