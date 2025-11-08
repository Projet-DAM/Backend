import { ApiProperty } from '@nestjs/swagger';
import { UserResponseDto } from '../../users/dto/user-response.dto';

export class AuthResponseDto {
  @ApiProperty({ 
    example: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1MDdmMWY3N2JjZjg2Y2Q3OTk0MzkwMTEiLCJlbWFpbCI6ImplYW4uZHVwb250QGV4YW1wbGUuY29tIiwicm9sZSI6InBhcmVudCIsImlhdCI6MTYxNjE2MjQwMCwiZXhwIjoxNjE2MjQ4ODAwfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c',
    description: 'Token JWT pour l\'authentification'
  })
  access_token: string;

  @ApiProperty({ 
    type: UserResponseDto,
    description: 'Informations de l\'utilisateur authentifié'
  })
  user: UserResponseDto;
}


