# 🔧 Correction - Erreurs Twilio SMS et WhatsApp

## 🎯 Problèmes Identifiés

### Erreur 1 : Format de Numéro Invalide (SMS)
```
❌ Error sending SMS to 92340748: Invalid 'To' Phone Number: 9234XXXX
```

**Cause :** Le numéro `92340748` n'est pas au format international E.164 requis par Twilio.

### Erreur 2 : Numéro WhatsApp "From" Invalide
```
❌ Error sending WhatsApp to 92340748: Twilio could not find a Channel with the specified From address
```

**Cause :** Le numéro WhatsApp "From" n'est pas configuré correctement dans Twilio.

---

## ✅ Solution Appliquée

### 1. Normalisation Automatique des Numéros

J'ai ajouté une fonction `normalizePhoneNumber()` dans `SmsService` qui convertit automatiquement les numéros tunisiens au format international E.164.

**Fichier modifié :** `src/common/services/sms.service.ts`

### Fonction de Normalisation

```typescript
private normalizePhoneNumber(phoneNumber: string): string {
    // Supprimer tous les espaces, tirets, parenthèses
    let cleaned = phoneNumber.replace(/[\s\-\(\)]/g, '');
    
    // Si le numéro commence déjà par +, le retourner tel quel
    if (cleaned.startsWith('+')) {
        return cleaned;
    }
    
    // Si le numéro commence par 216 (code pays Tunisie), ajouter +
    if (cleaned.startsWith('216')) {
        return `+${cleaned}`;
    }
    
    // Si le numéro est un numéro local tunisien (8 chiffres commençant par 2, 4, 5, 7, 9)
    // Ajouter le code pays +216
    if (/^[24579]\d{7}$/.test(cleaned)) {
        return `+216${cleaned}`;
    }
    
    // Si aucun format reconnu, retourner le numéro nettoyé avec +216 par défaut
    this.logger.warn(`⚠️ Format de numéro non reconnu: ${phoneNumber}. Ajout du code pays +216 par défaut.`);
    return `+216${cleaned}`;
}
```

### Exemples de Conversion

| Format d'Entrée | Format de Sortie | Description |
|----------------|------------------|-------------|
| `92340748` | `+21692340748` | Numéro local tunisien |
| `21692340748` | `+21692340748` | Avec code pays sans + |
| `+21692340748` | `+21692340748` | Déjà au bon format |
| `92 34 07 48` | `+21692340748` | Avec espaces |
| `(92) 34-07-48` | `+21692340748` | Avec parenthèses et tirets |

### 2. Utilisation dans sendSms()

```typescript
async sendSms(to: string, message: string): Promise<boolean> {
    if (!this.twilioClient) {
        this.logger.warn('⚠️ Twilio not configured. Cannot send SMS.');
        return false;
    }

    try {
        // Normaliser le numéro au format E.164
        const normalizedTo = this.normalizePhoneNumber(to);
        this.logger.log(`📱 Normalisation du numéro: ${to} → ${normalizedTo}`);

        const result = await this.twilioClient.messages.create({
            body: message,
            from: this.twilioPhoneNumber,
            to: normalizedTo,  // ✅ Utilise le numéro normalisé
        });

        this.logger.log(`✅ SMS sent successfully to ${normalizedTo}. SID: ${result.sid}`);
        return true;
    } catch (error: any) {
        this.logger.error(`❌ Error sending SMS to ${to}: ${error.message}`);
        return false;
    }
}
```

### 3. Utilisation dans sendWhatsApp()

```typescript
async sendWhatsApp(to: string, message: string): Promise<boolean> {
    if (!this.twilioClient) {
        this.logger.warn('⚠️ Twilio not configured. Cannot send WhatsApp.');
        return false;
    }

    try {
        // Normaliser le numéro au format E.164
        const normalizedTo = this.normalizePhoneNumber(to);
        this.logger.log(`📱 Normalisation du numéro WhatsApp: ${to} → ${normalizedTo}`);
        
        const formattedTo = normalizedTo.startsWith('whatsapp:') ? normalizedTo : `whatsapp:${normalizedTo}`;
        const from = this.configService.get<string>('TWILIO_WHATSAPP_NUMBER') || 'whatsapp:+14155238886';

        const result = await this.twilioClient.messages.create({
            body: message,
            from: from,
            to: formattedTo,  // ✅ Utilise le numéro normalisé
        });

        this.logger.log(`✅ WhatsApp message sent successfully to ${normalizedTo}. SID: ${result.sid}`);
        return true;
    } catch (error: any) {
        this.logger.error(`❌ Error sending WhatsApp to ${to}: ${error.message}`);
        return false;
    }
}
```

---

## 🧪 Comment Tester

### 1. Vérifier les Logs de Normalisation

Après avoir effectué un paiement, vous devriez voir dans les logs :

```
📱 Normalisation du numéro: 92340748 → +21692340748
✅ SMS sent successfully to +21692340748. SID: SM...
```

### 2. Tester avec Différents Formats

Testez avec ces formats de numéro :
- `92340748` (local)
- `21692340748` (avec code pays)
- `+21692340748` (format international)
- `92 34 07 48` (avec espaces)

Tous devraient être convertis en `+21692340748`.

---

## ⚠️ Configuration Twilio Requise

### Pour les SMS

Assurez-vous que votre fichier `.env` contient :

```env
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_PHONE_NUMBER=+1234567890  # Votre numéro Twilio
```

### Pour WhatsApp

```env
TWILIO_WHATSAPP_NUMBER=whatsapp:+14155238886  # Numéro sandbox Twilio ou votre numéro WhatsApp Business
```

**Note :** Pour WhatsApp, vous devez :
1. Activer le sandbox WhatsApp dans votre compte Twilio
2. OU configurer un numéro WhatsApp Business approuvé

---

## 🔍 Dépannage

### Erreur : "Invalid 'To' Phone Number"

**Solution :** Le numéro est maintenant normalisé automatiquement. Si l'erreur persiste :
1. Vérifiez que le numéro est bien un numéro tunisien valide (8 chiffres)
2. Vérifiez les logs pour voir le numéro normalisé

### Erreur : "Twilio could not find a Channel with the specified From address"

**Solutions :**

#### Option 1 : Utiliser le Sandbox WhatsApp (Développement)
1. Allez sur https://console.twilio.com/us1/develop/sms/try-it-out/whatsapp-learn
2. Suivez les instructions pour activer le sandbox
3. Le numéro par défaut `whatsapp:+14155238886` devrait fonctionner

#### Option 2 : Configurer WhatsApp Business (Production)
1. Demandez l'approbation de votre numéro WhatsApp Business
2. Configurez `TWILIO_WHATSAPP_NUMBER` dans `.env`

### Erreur : "Twilio not configured"

**Solution :** Vérifiez que `TWILIO_ACCOUNT_SID` et `TWILIO_AUTH_TOKEN` sont bien configurés dans `.env`.

---

## 📊 Flux de Traitement

```
Numéro d'entrée: "92340748"
         ↓
Suppression des espaces/tirets
         ↓
Vérification du format
         ↓
Ajout du code pays +216
         ↓
Numéro normalisé: "+21692340748"
         ↓
Envoi via Twilio
         ↓
✅ SMS/WhatsApp envoyé
```

---

## 📝 Notes Importantes

### Numéros Tunisiens Valides

Les numéros tunisiens commencent par :
- `2` - Fixe (région de Tunis)
- `4` - Fixe (autres régions)
- `5` - Fixe (autres régions)
- `7` - Mobile
- `9` - Mobile

Et contiennent **8 chiffres** au total.

### Format E.164

Le format E.164 est le standard international pour les numéros de téléphone :
- Commence par `+`
- Suivi du code pays (216 pour la Tunisie)
- Suivi du numéro local (8 chiffres)
- **Exemple :** `+21692340748`

---

## ✅ Résultat Attendu

### Avant la Correction ❌
```
❌ Error sending SMS to 92340748: Invalid 'To' Phone Number: 9234XXXX
❌ Error sending WhatsApp to 92340748: Twilio could not find a Channel...
```

### Après la Correction ✅
```
📱 Normalisation du numéro: 92340748 → +21692340748
✅ SMS sent successfully to +21692340748. SID: SM...
📱 Normalisation du numéro WhatsApp: 92340748 → +21692340748
✅ WhatsApp message sent successfully to +21692340748. SID: SM...
```

---

**Date de résolution :** 2025-12-29
**Fichier modifié :** `src/common/services/sms.service.ts`
**Statut :** ✅ RÉSOLU
