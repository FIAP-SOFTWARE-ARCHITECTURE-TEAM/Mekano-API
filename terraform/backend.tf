terraform {
  backend "s3" {
    key            = "mekano/infra/terraform.tfstate"
    encrypt        = true

    # Lock nativo no S3
    use_lockfile = true

    # Mantido temporariamente por requisito acadêmico.
    # Deprecated no Terraform atual.
    dynamodb_table = "mekano-terraform-locks"
  }
}