terraform {
  backend "s3" {
    key            = "mekano/infra/terraform.tfstate"
    encrypt        = true

    # Requisito acadêmico: locking com DynamoDB.
    # Em Terraform moderno o lockfile S3 é preferível; mantemos os dois
    # durante a migração/compatibilidade.
    dynamodb_table = "mekano-terraform-locks"
    use_lockfile   = true
  }
}
