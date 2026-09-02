terraform {
  backend "s3" {
    key          = "mekano/infra/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}