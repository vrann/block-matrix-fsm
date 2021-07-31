resource "aws_instance" "actorchoreography" {
  count = 5
  ami           = "ami-0988755aec6cca950"
  instance_type = "m5.2xlarge"
  key_name = "aws-nvirgin"
  subnet_id = "subnet-3e90de14"
  security_groups = ["sg-034f90166dab3d091"]
  tags = {
    Name = "ActorChoreography"
  }
}
