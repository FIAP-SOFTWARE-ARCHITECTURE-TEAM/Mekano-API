provider "aws" {

  region = var.region

  default_tags {

    tags = local.tags

  }

}

data "aws_availability_zones" "available" {

  state = "available"

}

locals {

  azs = slice(data.aws_availability_zones.available.names, 0, 2)

  tags = {

    Project = "mekano"

    Environment = var.environment

    ManagedBy = "terraform"

  }

}

# ============================================================

# VPC

# ============================================================

module "vpc" {

  source = "terraform-aws-modules/vpc/aws"

  version = "5.21.0"

  name = "mekano-${var.environment}"

  cidr = var.vpc_cidr

  azs = local.azs

  private_subnets = [

    cidrsubnet(var.vpc_cidr, 4, 0),

    cidrsubnet(var.vpc_cidr, 4, 1)

  ]

  public_subnets = [

    cidrsubnet(var.vpc_cidr, 4, 8),

    cidrsubnet(var.vpc_cidr, 4, 9)

  ]

  database_subnets = [

    cidrsubnet(var.vpc_cidr, 4, 12),

    cidrsubnet(var.vpc_cidr, 4, 13)

  ]

  enable_nat_gateway = true

  single_nat_gateway = true

  enable_dns_support = true

  enable_dns_hostnames = true

  # RDS modules below create their own DB subnet groups.

  create_database_subnet_group = false

  public_subnet_tags = {

    "kubernetes.io/role/elb" = "1"

  }

  private_subnet_tags = {

    "kubernetes.io/role/internal-elb" = "1"

  }

}

# ============================================================

# EKS

# ============================================================

module "eks" {

  source = "terraform-aws-modules/eks/aws"

  version = "20.37.2"

  cluster_name = var.cluster_name

  cluster_version = var.kubernetes_version

  cluster_endpoint_private_access = true

  cluster_endpoint_public_access = true

  authentication_mode = "API_AND_CONFIG_MAP"

  enable_cluster_creator_admin_permissions = true

  enable_irsa = true

  vpc_id = module.vpc.vpc_id

  subnet_ids = module.vpc.private_subnets

  cluster_addons = {

    coredns = {}

    kube-proxy = {}

    vpc-cni = {

      before_compute = true

    }

    metrics-server = {}

  }

  eks_managed_node_group_defaults = {

    ami_type = "AL2023_x86_64_STANDARD"

  }

  eks_managed_node_groups = {

    mekano = {

      name = "mekano-workers"

      instance_types = var.node_instance_types

      min_size = 2

      max_size = 6

      desired_size = 2

      capacity_type = "ON_DEMAND"

    }

  }

  access_entries = var.github_actions_principal_arn == "" ? {} : {

    github_actions = {

      principal_arn = var.github_actions_principal_arn

      policy_associations = {

        cluster_admin = {

          policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

          access_scope = {

            type = "cluster"

          }

        }

      }

    }

  }

}

# ============================================================

# EBS CSI Driver

# Used by Evolution API PVC (/evolution/instances)

# ============================================================

data "aws_iam_policy_document" "ebs_csi_assume_role" {

  statement {

    effect = "Allow"

    actions = [

      "sts:AssumeRoleWithWebIdentity"

    ]

    principals {

      type = "Federated"

      identifiers = [

        module.eks.oidc_provider_arn

      ]

    }

    condition {

      test = "StringEquals"

      variable = "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:aud"

      values = [

        "sts.amazonaws.com"

      ]

    }

    condition {

      test = "StringEquals"

      variable = "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:sub"

      values = [

        "system:serviceaccount:kube-system:ebs-csi-controller-sa"

      ]

    }

  }

}

resource "aws_iam_role" "ebs_csi" {

  name = "mekano-ebs-csi-${var.environment}"

  assume_role_policy = data.aws_iam_policy_document.ebs_csi_assume_role.json

}

resource "aws_iam_role_policy_attachment" "ebs_csi" {

  role = aws_iam_role.ebs_csi.name

  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"

}

resource "aws_eks_addon" "ebs_csi" {

  cluster_name = module.eks.cluster_name

  addon_name = "aws-ebs-csi-driver"

  service_account_role_arn = aws_iam_role.ebs_csi.arn

  resolve_conflicts_on_create = "OVERWRITE"

  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [

    module.eks,

    aws_iam_role_policy_attachment.ebs_csi

  ]

}

# ============================================================

# Mekano - PostgreSQL Security Group

# ============================================================

resource "aws_security_group" "rds" {

  name_prefix = "mekano-rds-"

  description = "Allow PostgreSQL only from Mekano EKS worker nodes"

  vpc_id = module.vpc.vpc_id

  ingress {

    description = "PostgreSQL from EKS worker nodes"

    from_port = 5432

    to_port = 5432

    protocol = "tcp"

    security_groups = [

      module.eks.node_security_group_id

    ]

  }

  egress {

    from_port = 0

    to_port = 0

    protocol = "-1"

    cidr_blocks = [

      "0.0.0.0/0"

    ]

  }

  lifecycle {

    create_before_destroy = true

  }

}

# ============================================================

# Mekano - PostgreSQL RDS

# ============================================================

module "db" {

  source = "terraform-aws-modules/rds/aws"

  version = "6.12.0"

  identifier = "mekano-${var.environment}"

  engine = "postgres"

  engine_version = "16"

  family = "postgres16"

  major_engine_version = "16"

  instance_class = var.db_instance_class

  allocated_storage = var.db_allocated_storage

  max_allocated_storage = 100

  storage_type = "gp3"

  storage_encrypted = true

  db_name = var.db_name

  username = var.db_username

  password = var.db_password

  port = "5432"

  manage_master_user_password = false

  multi_az = var.db_multi_az

  publicly_accessible = false

  create_db_subnet_group = true

  subnet_ids = module.vpc.database_subnets

  vpc_security_group_ids = [

    aws_security_group.rds.id

  ]

  backup_retention_period = 7

  auto_minor_version_upgrade = true

  apply_immediately = false

  enabled_cloudwatch_logs_exports = [

    "postgresql",

    "upgrade"

  ]

  skip_final_snapshot = var.environment != "prod"

  deletion_protection = var.environment == "prod"

  create_db_option_group = false

}

# ============================================================

# Evolution API - PostgreSQL Security Group

# ============================================================

resource "aws_security_group" "evolution_rds" {

  name_prefix = "mekano-evolution-rds-"

  description = "Allow Evolution PostgreSQL only from Mekano EKS worker nodes"

  vpc_id = module.vpc.vpc_id

  ingress {

    description = "PostgreSQL from EKS worker nodes"

    from_port = 5432

    to_port = 5432

    protocol = "tcp"

    security_groups = [

      module.eks.node_security_group_id

    ]

  }

  egress {

    from_port = 0

    to_port = 0

    protocol = "-1"

    cidr_blocks = [

      "0.0.0.0/0"

    ]

  }

  lifecycle {

    create_before_destroy = true

  }

}

# ============================================================

# Evolution API - PostgreSQL RDS

# ============================================================

module "evolution_db" {

  source = "terraform-aws-modules/rds/aws"

  version = "6.12.0"

  identifier = "mekano-evolution-${var.environment}"

  engine = "postgres"

  engine_version = "16"

  family = "postgres16"

  major_engine_version = "16"

  instance_class = var.evolution_db_instance_class

  allocated_storage = 20

  max_allocated_storage = 100

  storage_type = "gp3"

  storage_encrypted = true

  db_name = var.evolution_db_name

  username = var.evolution_db_username

  password = var.evolution_db_password

  port = "5432"

  manage_master_user_password = false

  multi_az = var.environment == "prod"

  publicly_accessible = false

  create_db_subnet_group = true

  subnet_ids = module.vpc.database_subnets

  vpc_security_group_ids = [

    aws_security_group.evolution_rds.id

  ]

  backup_retention_period = var.environment == "prod" ? 7 : 1

  auto_minor_version_upgrade = true

  apply_immediately = false

  enabled_cloudwatch_logs_exports = [

    "postgresql",

    "upgrade"

  ]

  skip_final_snapshot = var.environment != "prod"

  deletion_protection = var.environment == "prod"

  create_db_option_group = false

}

# ============================================================

# Evolution API - Redis Security Group

# ============================================================

resource "aws_security_group" "evolution_redis" {

  name_prefix = "mekano-evolution-redis-"

  description = "Allow Redis only from Mekano EKS worker nodes"

  vpc_id = module.vpc.vpc_id

  ingress {

    description = "Redis from EKS worker nodes"

    from_port = 6379

    to_port = 6379

    protocol = "tcp"

    security_groups = [

      module.eks.node_security_group_id

    ]

  }

  egress {

    from_port = 0

    to_port = 0

    protocol = "-1"

    cidr_blocks = [

      "0.0.0.0/0"

    ]

  }

  lifecycle {

    create_before_destroy = true

  }

}

# ============================================================

# Evolution API - ElastiCache Redis

# ============================================================

resource "aws_elasticache_subnet_group" "evolution" {

  name = "mekano-evolution-${var.environment}"

  subnet_ids = module.vpc.private_subnets

}

resource "aws_elasticache_replication_group" "evolution" {

  replication_group_id = "mekano-evolution-${var.environment}"

  description = "Redis cache for Evolution API"

  engine = "redis"

  engine_version = "7.1"

  node_type = var.evolution_redis_node_type

  port = 6379

  num_cache_clusters = 1

  automatic_failover_enabled = false

  multi_az_enabled = false

  subnet_group_name = aws_elasticache_subnet_group.evolution.name

  security_group_ids = [

    aws_security_group.evolution_redis.id

  ]

  at_rest_encryption_enabled = true

  # Initial Evolution API configuration uses redis://.

  # Redis stays private and SG-restricted inside the VPC.

  transit_encryption_enabled = false

  apply_immediately = true

}
