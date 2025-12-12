import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsNotEmpty, IsString, IsOptional, IsMongoId } from 'class-validator';

export class LoginUserDto {
  @ApiProperty({ example: 'jean.dupont@example.com', description: 'Adresse email' })
  @IsNotEmpty()
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'password123', description: 'Mot de passe' })
  @IsNotEmpty()
  @IsString()
  motDePasse: string;
}

 