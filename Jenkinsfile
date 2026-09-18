pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        REGISTRY = 'ghcr.io/singh301'
        IMAGE_TAG = "v${BUILD_NUMBER}"
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
            steps {
                sh 'mvn -B verify -Pquality -DskipTests'
            }
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

        stage('Validate Kubernetes Manifests') {
            when { branch 'master' }
            steps {
                sh '''
                  set -e
                  kubectl apply --dry-run=server -f k8s/namespace.yaml
                  kubectl apply --dry-run=server -f k8s/configmap.yaml
                  kubectl apply --dry-run=server -f k8s/network-policies.yaml
                  kubectl apply --dry-run=server -f k8s/services.yaml
                  kubectl apply --dry-run=server -f k8s/loan-services.yaml
                  kubectl apply --dry-run=server -f k8s/support-services.yaml
                  kubectl apply --dry-run=server -f k8s/dev-infrastructure.yaml
                  echo "Kubernetes manifest validation passed."
                '''
            }
        }

        stage('Deploy Kubernetes') {
            when { branch 'master' }
            steps {
                sh '''
                  set -e
                  kubectl apply -f k8s/namespace.yaml

                  echo "Validating required Kubernetes Secret: loan-management-secrets"
                  kubectl -n loan-management get secret loan-management-secrets >/dev/null

                  for key in DB_USERNAME DB_PASSWORD JWT_SECRET; do
                    encoded=$(kubectl -n loan-management get secret loan-management-secrets \
                      -o "jsonpath={.data.${key}}" 2>/dev/null || true)

                    if [ -z "$encoded" ]; then
                      echo "ERROR: Required secret key ${key} is missing or empty."
                      exit 1
                    fi

                    value=$(printf '%s' "$encoded" | base64 -d 2>/dev/null || true)
                    case "$value" in
                      ""|"change-me"|"change-me-with-a-long-random-value")
                        echo "ERROR: Secret key ${key} contains a placeholder value."
                        exit 1
                        ;;
                    esac
                  done

                  unset encoded value

                  kubectl apply -f k8s/configmap.yaml
                  kubectl apply -f k8s/network-policies.yaml
                  kubectl apply -f k8s/services.yaml
                  kubectl apply -f k8s/loan-services.yaml
                  kubectl apply -f k8s/support-services.yaml

                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    kubectl -n loan-management set image deployment/${service} ${service}=${REGISTRY}/${service}:${IMAGE_TAG}
                  done

                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    kubectl -n loan-management rollout status deployment/${service} --timeout=180s
                  done

                  echo "Verifying deployed image tags and available replicas..."
                  for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                    expected="${REGISTRY}/${service}:${IMAGE_TAG}"
                    actual=$(kubectl -n loan-management get deployment "${service}" -o jsonpath='{.spec.template.spec.containers[0].image}')
                    available=$(kubectl -n loan-management get deployment "${service}" -o jsonpath='{.status.availableReplicas}')
                    desired=$(kubectl -n loan-management get deployment "${service}" -o jsonpath='{.spec.replicas}')

                    if [ "$actual" != "$expected" ]; then
                      echo "ERROR: ${service} expected image ${expected}, found ${actual}"
                      exit 1
                    fi

                    if [ -z "$available" ] || [ "$available" -lt "$desired" ]; then
                      echo "ERROR: ${service} has ${available:-0}/${desired} available replicas"
                      exit 1
                    fi

                    echo "OK: ${service} -> ${actual}, available ${available}/${desired}"
                  done
                '''
            }
            post {
                failure {
                    sh '''
                      set +e
                      echo "Kubernetes deployment failed. Rolling back deployments..."
                      for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                        kubectl -n loan-management rollout undo deployment/${service} || true
                      done
                      for service in discovery-server api-gateway auth-service customer-service loan-service repayment-service document-service notification-service audit-service dashboard-service; do
                        kubectl -n loan-management rollout status deployment/${service} --timeout=120s || true
                      done
                    '''
                }
            }
        }

        stage('Smoke Test') {
            when { branch 'master' }
            steps {
                sh '''
                  set -e

                  kubectl -n loan-management get deployments -o wide
                  kubectl -n loan-management get pods -o wide
                  kubectl -n loan-management get svc

                  echo "Starting temporary API Gateway port-forward..."
                  kubectl -n loan-management port-forward svc/api-gateway 18080:80 >/tmp/loan-gateway-port-forward.log 2>&1 &
                  PF_PID=$!
                  trap 'kill "$PF_PID" >/dev/null 2>&1 || true' EXIT

                  ready=false
                  for i in $(seq 1 30); do
                    if curl --silent --show-error --fail --max-time 5 http://127.0.0.1:18080/actuator/health >/tmp/loan-gateway-health.json; then
                      ready=true
                      break
                    fi
                    sleep 2
                  done

                  if [ "$ready" != "true" ]; then
                    echo "ERROR: API Gateway smoke test failed."
                    cat /tmp/loan-gateway-port-forward.log || true
                    exit 1
                  fi

                  grep -q '"status":"UP"' /tmp/loan-gateway-health.json
                  echo "API Gateway smoke test passed."
                  cat /tmp/loan-gateway-health.json
                '''
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/dependency-check-report.html', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/dependency-check-report.json', allowEmptyArchive: true
        }
        failure {
            echo 'Pipeline failed. Check the stage logs and Kubernetes rollout status.'
        }
    }
}
