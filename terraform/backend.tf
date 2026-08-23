terraform {
  backend "s3" {
    key            = "mekano/infra/terraform.tfstate"
    encrypt        = true

    # Requisito acadêmico: locking com DynamoDB.
    # A tabela DynamoDB é usada para locking; parametrizar o nome via variável quando necessário.
    dynamodb_table = "mekano-terraform-locks"
  }
}
