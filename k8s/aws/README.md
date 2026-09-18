# AWS production overlay

This directory is the AWS-oriented overlay for the application workloads.

It intentionally does not include k8s/dev-infrastructure.yaml. Production should use managed AWS services such as Amazon RDS, ElastiCache, MSK and S3.

## Prerequisites

1. An Amazon EKS cluster with the AWS Load Balancer Controller installed.
2. The EKS Pod Identity Agent installed on the cluster.
3. An ACM certificate covering the production API hostname.
4. Amazon RDS, ElastiCache and MSK endpoints.
5. A private S3 bucket for documents.
6. An IAM role for document-service-aws with the least-privilege S3 policy in deploy/aws/document-service-s3-policy.json.

## Pod Identity

Create an IAM role using deploy/aws/document-service-pod-identity-trust-policy.json, attach the S3 policy from deploy/aws/document-service-s3-policy.json, and create an EKS Pod Identity association for:

- cluster: your EKS cluster
- namespace: loan-management
- service account: document-service-aws
- IAM role: your document-service S3 role

EKS Pod Identity associations are created through the EKS API or CLI; the association itself is not represented as an IAM-role annotation in the Kubernetes ServiceAccount.

## Secrets

The base application expects Kubernetes Secret keys such as DB_USERNAME, DB_PASSWORD, JWT_SECRET, and KAFKA_SASL_JAAS_CONFIG.

For production, source these values from AWS Secrets Manager rather than committing them to Git. AWS supports mounting Secrets Manager values into EKS pods through the AWS Secrets and Configuration Provider (ASCP) with the Secrets Store CSI Driver.

## ALB

Replace these placeholders in ingress.yaml before applying:

- REPLACE_WITH_ACM_CERTIFICATE_ARN
- REPLACE_WITH_API_HOSTNAME

The ingress uses AWS Load Balancer Controller with an internet-facing ALB, HTTPS on port 443, and target-type: ip, which routes directly to pod IPs.

## Configuration

Edit configmap-patch.yaml and replace:

- REPLACE_WITH_RDS_ENDPOINT
- REPLACE_WITH_ELASTICACHE_ENDPOINT
- REPLACE_WITH_MSK_BOOTSTRAP_SERVERS

Then validate and apply:

    kubectl kustomize k8s/aws
    kubectl apply -k k8s/aws

Do not apply the overlay until all REPLACE_WITH_* values have been replaced.

The document-service deployment is switched to the s3 backend by document-service-patch.yaml.
