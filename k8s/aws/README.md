# AWS / EKS deployment

This overlay is the fast, self-contained EKS deployment profile for the current project.

## What it uses

- Amazon EKS for compute
- Amazon RDS MySQL for the database
- AWS Secrets Manager + Secrets Store CSI Driver + EKS Pod Identity for application secrets
- Redis and Kafka/ZooKeeper inside the EKS cluster for the initial working deployment
- AWS Load Balancer Controller for the public ALB
- GHCR for application images

The repository does not contain passwords, JWT secrets, PATs, or other credentials.

## One-command deployment

From the EC2 deployment host:

    cd ~/loan-management-microservices
    git pull --ff-only origin master
    bash deploy/aws/deploy-eks.sh

The script updates kubeconfig, ensures worker capacity, deploys Redis/Kafka, creates the Secrets Manager sync workload, applies the AWS Kustomize overlay, waits for the core services, and prints the ALB hostname.

## Required AWS-side prerequisites

These already need to exist before running the script:

1. EKS cluster: loan-management-cluster in ap-south-1.
2. RDS endpoint and worker-to-RDS security-group access.
3. AWS Load Balancer Controller.
4. Secrets Store CSI Driver with AWS provider.
5. EKS Pod Identity association for service account loan-management-secrets.
6. AWS Secrets Manager secret loan-management-app-secrets.
7. Kubernetes secret ghcr-secret for the private GHCR images.

For this deployment profile, Kafka and Redis are provisioned by Kubernetes, so no MSK or ElastiCache endpoint is required.

## Production follow-up

After the application is working end-to-end, Kafka can be moved to Amazon MSK, Redis to ElastiCache, document storage to S3, and the ALB can be switched to HTTPS with ACM. Those are hardening steps, not prerequisites for proving the application deployment path.
