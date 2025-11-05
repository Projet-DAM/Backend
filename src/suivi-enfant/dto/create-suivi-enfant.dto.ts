export class CreateSuiviEnfantDto {
  date_suivi: Date;
  presence: boolean;
  performance: number;
  commentaire?: string;
  enfantId: string; // Correspond à l'ID du User avec rôle ENFANT
}
