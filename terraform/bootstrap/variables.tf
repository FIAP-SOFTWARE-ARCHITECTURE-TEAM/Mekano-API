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
# Evolution API (deprecated — now shares Mekano's RDS)
# ============================================================
