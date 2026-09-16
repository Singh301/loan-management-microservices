pipeline {
    agent any

    environment {
        REGISTRY = 'ghcr.io/singh301'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Unit Test') {
            steps { sh 'mvn -B clean verify -DskipTests=false' }
        }

        stage('Quality & Security') {
            steps { sh 'mvn -B verify -Pquality -DskipTests' }
        }

        stage('Build Images') {
            steps {
                sh '''
                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    docker build -t ${REGISTRY}/${service}:${IMAGE_TAG} -f ${service}/Dockerfile .
                  done
                '''
            }
        }

        stage('Push Images') {
            when { branch 'master' }
            steps {
                sh '''
                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    docker push ${REGISTRY}/${service}:${IMAGE_TAG}
                  done
                '''
            }
        }

        stage('Deploy Kubernetes') {
            when { branch 'master' }
            steps {
                sh '''
                  kubectl apply -f k8s/namespace.yaml
                  kubectl apply -f k8s/configmap.yaml
                  kubectl apply -f k8s/services.yaml
                  kubectl apply -f k8s/loan-services.yaml
                  kubectl -n loan-management rollout status deployment/loan-service --timeout=180s
                  kubectl -n loan-management rollout status deployment/api-gateway --timeout=180s
                '''
            }
        }

        stage('Smoke Test') {
            when { branch 'master' }
            steps {
                sh 'kubectl -n loan-management get pods -o wide'
                sh 'kubectl -n loan-management get svc'
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
        }
        failure {
            echo 'Pipeline failed. Check the stage logs and Kubernetes rollout status.'
        }
    }
}
