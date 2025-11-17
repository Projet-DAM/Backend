import {
  registerDecorator,
  ValidationOptions,
  ValidatorConstraint,
  ValidatorConstraintInterface,
  ValidationArguments,
} from 'class-validator';
import { UserRole } from '../../users/interfaces/user-role.enum';

@ValidatorConstraint({ name: 'isRegisterRole', async: false })
export class IsRegisterRoleConstraint implements ValidatorConstraintInterface {
  validate(role: any, args: ValidationArguments) {
    return role === UserRole.PARENT || role === UserRole.COACH;
  }

  defaultMessage(args: ValidationArguments) {
    return 'Seuls les rôles "parent" et "coach" peuvent s\'inscrire via cette interface';
  }
}

export function IsRegisterRole(validationOptions?: ValidationOptions) {
  return function (object: Object, propertyName: string) {
    registerDecorator({
      target: object.constructor,
      propertyName: propertyName,
      options: validationOptions,
      constraints: [],
      validator: IsRegisterRoleConstraint,
    });
  };
}






