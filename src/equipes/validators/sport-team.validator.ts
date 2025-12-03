import { BadRequestException } from '@nestjs/common';

export class SportTeamValidator {
  private static readonly FOOTBALL_FORMATS = ['5v5', '7v7', '11v11'];
  private static readonly BASKETBALL_FORMATS = ['5v5'];
  private static readonly TENNIS_ALLOWED = [2, 4, 8, 16, 32];
  private static readonly INDIVIDUAL_SPORTS = new Set([
    'natation',
    'athletisme',
    'athlétisme',
    'gymnastique',
    'escrime',
    'karate',
    'karaté',
    'judo',
    'cyclisme',
    'escalade',
  ]);

  static validateComposition(sport: string, format: string, enfantsCount: number): void {
    if (!enfantsCount || enfantsCount < 1) {
      throw new BadRequestException('Une équipe doit contenir au moins un enfant');
    }

    const normalizedSport = (sport || '').trim().toLowerCase();
    const normalizedFormat = (format || '').trim().toLowerCase();

    if (!normalizedSport) {
      throw new BadRequestException('Le sport doit être renseigné pour valider une équipe');
    }

    if (this.INDIVIDUAL_SPORTS.has(normalizedSport)) {
      return;
    }

    if (normalizedSport === 'football') {
      if (!this.FOOTBALL_FORMATS.includes(normalizedFormat)) {
        throw new BadRequestException('Format football invalide (formats acceptés: 5v5, 7v7, 11v11)');
      }
      const expected = this.extractPlayersPerTeam(normalizedFormat);
      if (enfantsCount !== expected) {
        throw new BadRequestException(`Le football en ${normalizedFormat} exige ${expected} enfants par équipe`);
      }
      return;
    }

    if (normalizedSport === 'basketball' || normalizedSport === 'basket') {
      if (!this.BASKETBALL_FORMATS.includes(normalizedFormat)) {
        throw new BadRequestException('Le basketball se joue en format 5v5');
      }
      const expected = 5;
      if (enfantsCount !== expected) {
        throw new BadRequestException('Le basketball nécessite exactement 5 enfants par équipe');
      }
      return;
    }

    if (normalizedSport === 'tennis') {
      if (!this.TENNIS_ALLOWED.includes(enfantsCount)) {
        throw new BadRequestException('Le tennis simple nécessite un nombre d\'enfants égal à une puissance de 2 (2, 4, 8, 16, 32)');
      }
      return;
    }
  }

  private static extractPlayersPerTeam(format: string): number {
    const match = format.match(/^(\d+)/);
    if (!match) {
      throw new BadRequestException('Format d\'équipe invalide, utilisez la notation XvX (ex: 5v5)');
    }
    return Number(match[1]);
  }
}

