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
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            java -version
            node --version
            corepack --version
            git diff --check
            bash scripts/ci/version-guard.sh
          '
        '''
      }
    }

    stage('Frontend Dependencies') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            cd web
            corepack pnpm install --frozen-lockfile
          '
        '''
      }
    }

    stage('Frontend Unit Tests') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            cd web
            corepack pnpm -F @vben/playground run test:unit
            corepack pnpm test:unit
          '
        '''
      }
    }

    stage('Frontend Typecheck') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            cd web
            corepack pnpm -F @vben/playground run typecheck
          '
        '''
      }
    }

    stage('Backend Tests') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            cd server
            ./mvnw test
          '
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'server/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Frontend Build') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/run-with-toolchain.sh bash -c '
            cd web
            corepack pnpm -F @vben/playground run build
          '
        '''
      }
    }

    stage('Repository Guard') {
      steps {
        sh '''
          set -eu
          bash scripts/ci/version-guard.sh
          bash scripts/ci/repository-guard.sh
        '''
      }
    }
  }

  post {
    always {
      archiveArtifacts allowEmptyArchive: true, artifacts: 'web/playground/dist/**,server/target/surefire-reports/*.xml'
    }
  }
}
