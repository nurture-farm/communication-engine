pipeline {
    agent { label 'master' }

    environment {
        PATH = "/opt/jdk/jdk-11.0.8/bin:$PATH"
        JAVA_HOME="/opt/jdk/jdk-11.0.8"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    parameters {
		string(name: 'PROJECT_NAME', defaultValue: 'communication-engine', description: '')
		string(name: 'DOCKER_IMG_NAME', defaultValue: 'platform/communication-engine', description: '')
		string(name: 'ECR_URL', defaultValue: '455516961477.dkr.ecr.ap-south-1.amazonaws.com/', description: '')
		choice(name: 'RELEASE_MODE', choices: ['major', 'minor', 'patch'], description: 'Pick one.')
		gitParameter branchFilter: 'origin/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
	}

    stages {
        stage ('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Auto tagging') {
          steps {
            script {
              FINALTAG = sh (script: "bash /opt/jenkins-tag/tag.sh ${params.RELEASE_MODE} ${params.BRANCH}", returnStdout: true).trim()
              echo "Tag is : ${FINALTAG}"
            }
            echo "Returned Tag is : ${FINALTAG}"
          }
        }
    
 
	stage('Build') {
            steps {
                echo "Returned Tag in build : ${FINALTAG}"
                sh "mvn clean install ; cp -p /var/lib/jenkins/tmp/* . ; docker build -t ${params.DOCKER_IMG_NAME} ."
            }
        }

                stage('Quality Scan') {
                    steps {
                        script {
                            def scannerHome = tool 'sonarscanner';
                            withSonarQubeEnv('sonar') {sh "${tool("sonarscanner")}/bin/sonar-scanner \
                            -Dsonar.projectKey=${params.PROJECT_NAME} \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.sources=. \
                            -Dsonar.host.url=$SONAR_HOST_URL \
                            -Dsonar.login=$SONAR_LOGIN_KEY"
                        }
        		    }
        	   }
        }

        stage('Publish') {
            steps {
                 script {
                    sh "docker tag ${params.DOCKER_IMG_NAME}:latest ${params.ECR_URL}${params.DOCKER_IMG_NAME}:${FINALTAG}"
		            docker.withRegistry("https://${params.ECR_URL}", "ecr:ap-south-1:AWSECR") {
                        sh "docker push ${params.ECR_URL}${params.DOCKER_IMG_NAME}:${FINALTAG}"
                    }
                }
            }
        }

    }

    post {
	success {
	   writeFile file: "output/tag.txt", text: "tag=${FINALTAG}"
	   archiveArtifacts artifacts: 'output/*.txt'
	   emailext (recipientProviders: [[$class: 'RequesterRecipientProvider'], [$class: 'DevelopersRecipientProvider']], to: "ramkumar@nurture.farm", subject:"RELEASE BUILD SUCCESS: ${currentBuild.fullDisplayName}", body: "Release Build Successful! Reports Attached. Please review the reports and take necessary actions.")
	}

	failure {
	   emailext (recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], to: "ramkumar@nurture.farm", subject:"RELEASE BUILD FAILURE: ${currentBuild.fullDisplayName}", body: "Release Build Failed! Your commits is suspected to have caused the build failure. Please go to ${BUILD_URL} for details and resolve the build failure at the earliest.", attachLog: true, compressLog: true)
	}

	aborted {
	   emailext (recipientProviders: [[$class: 'RequesterRecipientProvider'], [$class: 'DevelopersRecipientProvider']], subject:"RELEASE BUILD ABORTED: ${currentBuild.fullDisplayName}", body: "Release Build Aborted! Please go to ${BUILD_URL} and verify the build.", attachLog: false, compressLog: false)
	}
   }
}