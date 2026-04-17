# TMS — AWS Deployment Guide

## Architecture
```
GitHub → GitHub Actions CI/CD → ECR → ECS Fargate
                                         ↓
                              ALB: tms-application-alb-1363434811.us-west-2.elb.amazonaws.com
                                    ├── /api/*  → tms-application-qa-api (port 8080)
                                    └── /*      → tms-application-qa-ui  (port 80)
```

## GitHub Secrets Required

Go to **GitHub → Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|--------|-------|
| `AWS_ACCESS_KEY_ID` | Your AWS IAM access key |
| `AWS_SECRET_ACCESS_KEY` | Your AWS IAM secret key |

## AWS Secrets Manager Setup

Create these secrets in AWS Secrets Manager (us-west-2):

```bash
aws secretsmanager create-secret --name tms/db-url \
  --secret-string "jdbc:sqlserver://YOUR_RDS_ENDPOINT:1433;databaseName=TMS;encrypt=true;trustServerCertificate=true"

aws secretsmanager create-secret --name tms/db-username --secret-string "tmsuser"
aws secretsmanager create-secret --name tms/db-password --secret-string "YOUR_DB_PASSWORD"
aws secretsmanager create-secret --name tms/jwt-secret  --secret-string "YOUR_STRONG_JWT_SECRET_MIN_32_CHARS"
aws secretsmanager create-secret --name tms/mail-username --secret-string "noreply@yourcompany.com"
aws secretsmanager create-secret --name tms/mail-password --secret-string "YOUR_MAIL_PASSWORD"
```

## IAM Permissions Required

The ECS task execution role needs:
- `secretsmanager:GetSecretValue` on `arn:aws:secretsmanager:us-west-2:119346096827:secret:tms/*`
- `ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`
- `logs:CreateLogStream`, `logs:PutLogEvents`

## CloudWatch Log Groups

Create log groups before first deploy:
```bash
aws logs create-log-group --log-group-name /ecs/tms-application-qa-api --region us-west-2
aws logs create-log-group --log-group-name /ecs/tms-application-qa-ui  --region us-west-2
```

## ALB Target Groups

Configure ALB listener rules:
- `/api/*` → target group pointing to ECS API service (port 8080)
- `/actuator/*` → target group pointing to ECS API service (port 8080)
- `/*` → target group pointing to ECS UI service (port 80)

## Deploy

Push to `main` or `develop` branch — GitHub Actions automatically:
1. Runs backend unit tests
2. Builds Docker images
3. Pushes to ECR
4. Updates ECS task definitions
5. Deploys to ECS Fargate
6. Runs smoke tests

## Local Docker Test

```bash
# Copy and fill in env vars
cp .env.example .env

# Build and run locally
docker-compose up --build

# App available at http://localhost
# API at http://localhost:8080
```
