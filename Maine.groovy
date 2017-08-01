// Folders
folder( "${Application_Name}" ){

}

def sampleFolder = "${Application_Name}"
def applicationName =  "${Application_Name}"

// Jobs
def generateBuildPipelineView = sampleFolder + "/Pipeline_View_" + applicationName
def generateBuildJob = sampleFolder + "/mavenBuildJob_" + applicationName
def generateCodeAnalysisJob = sampleFolder + "/codeAnalysisJob_" + applicationName
def generateUploadToNexusJob = sampleFolder + "/uploadToNexusJob_" + applicationName
def generateDeploymentToTOmcatJob = sampleFolder + "/deploymentToTomcat_" + applicationName

// ##### GENERATE BUILD PIPELINE VIEW #####
buildPipelineView(generateBuildPipelineView) {
	title('Pipeline_View')
    displayedBuilds(5)
    selectedJob(generateBuildJob)
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(5)
}	
// ##### END OF BUILD PIPELINE VIEW #####

// ##### GENERATE MAVEN BUILD JOB #####
freeStyleJob(generateBuildJob) {
    scm {
        git {
            remote {
                url("https://github.com/rjparanda/Simulation.git")
            }
        }
    }
    
	wrappers {
	preBuildCleanup()
	timestamps()
    }
    
	steps {
		maven {
			mavenInstallation('maven-3.5.0')
			goals('clean')
			goals('package')
		}
    }
    
	publishers{
    	downstreamParameterized {
            trigger(generateCodeAnalysisJob) {
                condition('SUCCESS')
                parameters {
			currentBuild()
			predefinedProps([CUSTOM_WORKSPACE: '$WORKSPACE'])
                }
            }
        }
    }
}
// ##### END MAVEN BUILD JOB #####

// ##### GENERATE CODE ANALYSIS JOB #####
freeStyleJob(generateCodeAnalysisJob) {
    parameters {
        stringParam('CUSTOM_WORKSPACE', '', '')
    }
	
	customWorkspace('$CUSTOM_WORKSPACE')
    
	wrappers {
		timestamps()
    }
	
	configure { project ->
			project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
				
			}
	}
    
    publishers{
    	downstreamParameterized {
            trigger(generateUploadToNexusJob) {
                condition('SUCCESS')
                parameters {
			currentBuild()
			predefinedProps([CUSTOM_WORKSPACE: '$WORKSPACE'])
                }
            }
        }
    }
}
// ##### END CODE ANALYSIS JOB #####

// ##### GENERATE UPLOAD TO NEXUS JOB #####
freeStyleJob(generateUploadToNexusJob) {
    parameters {
        stringParam('CUSTOM_WORKSPACE', '', '')
    }
	
	customWorkspace('$CUSTOM_WORKSPACE')
    
	wrappers {
		timestamps()
    }
	
	configure { project ->
			project / 'builders' / 'sp.sd.nexusartifactuploader.NexusArtifactUploader' {
				'nexusVersion'('nexus3')
				'protocol'('http')
				'nexusUrl'('localhost:8081')
				'groupId'('sample')
				'version'('0.0.1')
				'repository'('sample')
				'credentialsId'('nexus-admin')
				'artifacts'{
					'sp.sd.nexusartifactuploader.Artifact'{
						'artifactId'('sample')
						'type'('war')
						'file'('CounterWebApp.war')
					}
				}
			}
	}
    
    publishers{
    	downstreamParameterized {
            trigger(generateDeploymentToTOmcatJob) {
                condition('SUCCESS')
                parameters {
			currentBuild()
			predefinedProps([CUSTOM_WORKSPACE: '$WORKSPACE'])
                }
            }
        }
    }
}
// ##### END UPLOAD TO NEXUS JOB #####
// ##### GENERATE DEPLOY TO TOMCAT JOB #####
freeStyleJob(generateDeploymentToTOmcatJob) {
    parameters {
        stringParam('CUSTOM_WORKSPACE', '', '')
    }
	
	customWorkspace('$CUSTOM_WORKSPACE')
    
	wrappers {
		timestamps()
    }
	
	steps {
        batchFile('''copy /y target\\*.war C:\\apache-tomcat-8.5.16\\webapps\\''')
    }
    
}
// ##### END DEPLOY TO TOMCAT JOB #####