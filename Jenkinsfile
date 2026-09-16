pipeline {
    agent any

    environment {
        REGISTRY = 'ghcr.io/singh301'
        IMAGE_TAG = "${BUILD_NUMBER}"
        GHCR_CREDENTIALS = 'ghcr-credentials'
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
                  set -e
                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    docker build --pull -t ${REGISTRY}/${service}:${IMAGE_TAG} -f ${service}/Dockerfile .
                  done
                '''
            }
        }

        stage('Push Images') {
            when { branch 'master' }
            steps {
                withCredentials([usernamePassword(credentialsId: env.GHCR_CREDENTIALS,
                        usernameVariable: 'GHCR_USERNAME', passwordVariable: 'GHCR_TOKEN')]) {
                    sh '''
                      set -e
                      echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
                      for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                        docker push ${REGISTRY}/${service}:${IMAGE_TAG}
                      done
                      docker logout ghcr.io
                    '''
                }
            }
        }

        stage('Deploy Kubernetes') {
            when { branch 'master' }
            steps {
                sh '''
                  set -e
                  kubectl apply -f k8s/namespace.yaml
                  kubectl apply -f k8s/configmap.yaml
                  kubectl apply -f k8s/services.yaml
                  kubectl apply -f k8s/loan-services.yaml
                  kubectl apply -f k8s/support-services.yaml

                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    kubectl -n loan-management set image deployment/${service} ${service}=${REGISTRY}/${service}:${IMAGE_TAG}
                  done

                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    kubectl -n loan-management rollout status deployment/${service} --timeout=180s
                  done
                '''
            }
        }

        stage('Smoke Test') {
            when { branch 'master' }
            steps {
                sh 'kubectl -n loan-management get deployments -o wide'
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
