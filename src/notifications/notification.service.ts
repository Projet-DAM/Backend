import { Injectable, Logger } from '@nestjs/common';
import { FirebaseService } from '../firebase/firebase.service';
import { InscriptionDocument } from '../inscriptions/inscription.schema';
import { TournoiDocument } from '../tournoi/schemas/tournoi.schema';

@Injectable()
export class NotificationService {
  private readonly logger = new Logger(NotificationService.name);
  private static readonly COACH_TOPIC = 'coaches';

  constructor(private readonly firebaseService: FirebaseService) {}

  async sendToCoaches(inscription: InscriptionDocument, tournoi: TournoiDocument): Promise<void> {
    if (!this.firebaseService.isEnabled()) {
      this.logger.warn('Firebase est désactivé. Notification ignorée.');
      return;
    }

    const notification = {
      title: `Nouvelle inscription - ${tournoi.nom}`,
      body: `${inscription.enfantPrenom} ${inscription.enfantNom} vient d'être inscrit par ${inscription.parentPrenom}`,
    };

    const data = {
      tournoiId: tournoi._id.toString(),
      tournoiNom: tournoi.nom,
      sport: tournoi.sport || '',
      categorieAge: tournoi.categorieAge || '',
      enfantPrenom: inscription.enfantPrenom,
      enfantNom: inscription.enfantNom,
      parentPrenom: inscription.parentPrenom,
      parentNom: inscription.parentNom,
      parentTelephone: inscription.parentTelephone || '',
    };

    try {
      await this.firebaseService.sendToTopic(NotificationService.COACH_TOPIC, {
        notification,
        data,
      });
      this.logger.log('Notification envoyée au topic coaches');
    } catch (error) {
      this.logger.error('Erreur lors de l\'envoi de la notification Firebase', (error as Error).stack);
    }
  }
}




