pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Prepare') {
      steps {
        sh '''
          set -e
          chmod +x mvnw || true
          # Si alguna vez te falla por CRLF (bad interpreter), descomenta esto:
          # sed -i 's/\r$//' mvnw
        '''
      }
    }

    stage('Test') {
      steps { sh './mvnw -B test' }
    }

    stage('Build') {
      steps { sh './mvnw -B -DskipTests package' }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      junit 'target/surefire-reports/*.xml'
    }
  }
}
