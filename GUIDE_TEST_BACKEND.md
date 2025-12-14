# Guide Rapide - Test Backend

## Etape 1: Modifier le Script

Ouvrez `test-simple.ps1` et modifiez:

```powershell
$email = "votre-email@example.com"     # Email d'un parent dans votre DB
$password = "votre-mot-de-passe"       # Son mot de passe
```

## Etape 2: Executer le Script

```powershell
.\test-simple.ps1
```

## Resultats Attendus

### Si ca fonctionne:
```
Login OK
Nombre d'options: 3
  - Tenue sportive: 50 TND
  - Assurance: 30 TND
  - Transport: 40 TND

SUCCES!
```

### Si erreur 401:
```
ERREUR: {"message":"Unauthorized","statusCode":401}
```
→ Email ou mot de passe incorrect

### Si erreur connexion:
```
ERREUR: Unable to connect
```
→ Le serveur n'est pas demarre

## Solutions

### Serveur non demarre:
```bash
npm run start:dev
```

### Pas d'utilisateur parent:
Creez un parent via:
- POST /auth/register
- Ou directement dans MongoDB

### Mot de passe oublie:
Verifiez dans votre base de donnees MongoDB

## Fichiers Disponibles

- `test-simple.ps1` - Version simple (recommande)
- `test-subscription-options.ps1` - Version detaillee
