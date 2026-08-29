# ============================================================
# Existing EKS Access Entry
#
# This import reconciles the EKS access entry that already exists
# in AWS with the Terraform state.
#
# Keep this file only until the import has been successfully applied
# and the resource appears in the Terraform state.
# ============================================================

import {
  to = module.eks.aws_eks_access_entry.this["github_actions"]

  id = "mekano-eks:arn:aws:iam::070165420894:role/MekanoGitHubActionsRole"
}

# ============================================================
# Existing EKS Access Policy Association
#
# IMPORTANT:
# Uncomment this block ONLY if the policy association already exists
# in AWS for MekanoGitHubActionsRole.
#
# Check with:
#
# aws eks list-associated-access-policies \
#   --cluster-name mekano-eks \
#   --principal-arn arn:aws:iam::070165420894:role/MekanoGitHubActionsRole \
#   --region us-east-1
#
# If AmazonEKSClusterAdminPolicy appears in the result, uncomment
# the block below before running terraform plan/apply.
# ============================================================

# import {
#   to = module.eks.aws_eks_access_policy_association.this["github_actions_cluster_admin"]
#
#   id = "mekano-eks#arn:aws:iam::070165420894:role/MekanoGitHubActionsRole#arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
# }
