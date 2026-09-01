output "cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS API endpoint"
  value       = module.eks.cluster_endpoint
}

output "cluster_version" {
  description = "EKS Kubernetes version"
  value       = module.eks.cluster_version
}

# ============================================================
# Mekano - PostgreSQL
# ============================================================

output "db_endpoint" {
  description = "RDS PostgreSQL hostname"
  value       = module.db.db_instance_address
}

output "db_port" {
  description = "RDS PostgreSQL port"
  value       = module.db.db_instance_port
}

output "db_name" {
  description = "RDS database name"
  value       = var.db_name
}

# ============================================================
# EKS / kubectl
# ============================================================

output "configure_kubectl" {
  description = "Command to configure kubectl for EKS"
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${module.eks.cluster_name}"
}

output "evolution_service_url" {
  description = "Internal Kubernetes Evolution API URL"
  value       = "http://evolution-api:5033"
}