pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    DEFAULT_JAVA_HOME = '/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home'
    DEFAULT_NODE_HOME = '/opt/homebrew/opt/node@22'
    CI = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'git status --short --branch'
      }
    }

    stage('Preflight') {
      steps {
        sh 'bash scripts/ci/verify.sh preflight'
      }
    }

    stage('Build Metadata') {
      steps {
        sh 'bash scripts/ci/verify.sh build-metadata'
      }
    }

    stage('Frontend Dependencies') {
      steps {
        sh 'bash scripts/ci/verify.sh frontend-dependencies'
      }
    }

    stage('Frontend Unit Tests') {
      steps {
        sh 'bash scripts/ci/verify.sh frontend-unit'
      }
    }

    stage('Frontend Typecheck') {
      steps {
        sh 'bash scripts/ci/verify.sh frontend-typecheck'
      }
    }

    stage('Backend Tests') {
      steps {
        sh 'bash scripts/ci/verify.sh backend-test'
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'server/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Frontend Build') {
      steps {
        sh 'bash scripts/ci/verify.sh frontend-build'
      }
    }

    stage('Repository Guard') {
      steps {
        sh 'bash scripts/ci/verify.sh repository-guard'
      }
    }

    stage('Database Migration Report') {
      steps {
        sh 'bash scripts/ci/verify.sh database-migration-report'
      }
    }

    stage('API Governance Report') {
      steps {
        sh 'bash scripts/ci/verify.sh api-governance-report'
      }
    }

    stage('Verification Summary') {
      steps {
        sh 'bash scripts/ci/verify.sh verification-summary'
      }
    }
  }

  post {
    always {
      archiveArtifacts allowEmptyArchive: true, artifacts: '.ci-artifacts/**,web/playground/dist/**,server/target/surefire-reports/*.xml'
    }
  }
}
