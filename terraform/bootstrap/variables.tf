variable "region" {
  type    = string
  default = "us-east-1"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for Terraform state"
  type        = string
}

variable "lock_table_name" {
  type    = string
  default = "mekano-terraform-locks"
}

# ============================================================
# Evolution API
# ============================================================

variable "evolution_db_name" {
  description = "Evolution API PostgreSQL database name"
  type        = string
  default     = "evolution"
}

variable "evolution_db_username" {
  description = "Evolution API PostgreSQL username"
  type        = string
  default     = "evolution"
}

variable "evolution_db_password" {
  description = "Evolution API PostgreSQL password"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.evolution_db_password) >= 12
    error_message = "evolution_db_password must contain at least 12 characters."
  }
}

variable "evolution_db_instance_class" {
  description = "Evolution API RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "evolution_redis_node_type" {
  description = "Redis instance type used by Evolution API"
  type        = string
  default     = "cache.t4g.micro"
}
