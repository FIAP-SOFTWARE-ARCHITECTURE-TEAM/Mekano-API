variable "region" {
  description = "AWS region used by Mekano"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "mekano-eks"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes minor version"
  type        = string
  default     = "1.34"
}

variable "vpc_cidr" {
  description = "CIDR block for the Mekano VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "node_instance_types" {
  description = "EC2 instance types for the EKS managed node group"
  type        = list(string)
  default     = ["t3.medium"]
}

# ============================================================
# Mekano Database
# ============================================================

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "mekano"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "mekano"
}

variable "db_password" {
  description = "PostgreSQL master password. Prefer TF_VAR_db_password in CI."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.db_password) >= 12
    error_message = "db_password must contain at least 12 characters."
  }
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Initial RDS storage in GiB"
  type        = number
  default     = 20
}

variable "db_multi_az" {
  description = "Enable RDS Multi-AZ"
  type        = bool
  default     = false
}

# ============================================================
# GitHub Actions / EKS access
# ============================================================

variable "github_actions_principal_arn" {
  description = "Optional IAM user/role ARN used by GitHub Actions to access the EKS API"
  type        = string
  default     = ""
}

# ============================================================
# Evolution API - PostgreSQL
# ============================================================

variable "evolution_db_name" {
  description = "Evolution API PostgreSQL database name"
  type        = string
  default     = "evolution"
}

variable "evolution_db_username" {
  description = "Evolution API PostgreSQL master username"
  type        = string
  default     = "evolution"
}

variable "evolution_db_password" {
  description = "Evolution API PostgreSQL password. Prefer TF_VAR_evolution_db_password in CI."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.evolution_db_password) >= 12
    error_message = "evolution_db_password must contain at least 12 characters."
  }
}

variable "evolution_db_instance_class" {
  description = "RDS instance class for Evolution API"
  type        = string
  default     = "db.t4g.micro"
}

# ============================================================
# Evolution API - Redis
# ============================================================

variable "evolution_redis_node_type" {
  description = "ElastiCache Redis node type for Evolution API"
  type        = string
  default     = "cache.t4g.micro"
}