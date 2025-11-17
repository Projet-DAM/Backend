# Mode Développement - Code de Vérification

## 🔍 Comment voir le code de vérification

Puisque `SMTP_PASS` n'est pas encore configuré dans le fichier `.env`, le système fonctionne en **mode développement**.

### ✅ Le code est affiché dans les logs du backend

Lorsqu'un utilisateur s'inscrit, le code de vérification est **automatiquement affiché dans les logs du serveur backend**.

### 📋 Où voir le code

1. **Dans le terminal où le backend tourne** (où vous avez lancé `npm run start:dev`)
2. **Cherchez ces lignes** :
   ```
   ═══════════════════════════════════════════════════════════
   ⚠️  MODE DÉVELOPPEMENT - SMTP non configuré
   ═══════════════════════════════════════════════════════════
   📧 Email: hatemaidi2001@gmail.com
   👤 Nom: hatem aidi
   🔐 CODE DE VÉRIFICATION: 123456
   ═══════════════════════════════════════════════════════════
   ```

### 🧪 Test rapide

1. Inscrivez-vous depuis l'application Android
2. Regardez immédiatement les logs du backend
3. Copiez le code affiché (6 chiffres)
4. Entrez-le dans l'écran de vérification

### 📧 Pour activer l'envoi d'emails réels

Une fois que vous avez configuré `SMTP_PASS` dans le fichier `.env` :
- Le code sera envoyé par email automatiquement
- Les logs n'afficheront plus le code (sauf en cas d'erreur)

Voir `GUIDE_MOT_DE_PASSE_APPLICATION.md` pour configurer Gmail.






