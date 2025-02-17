# Spring SaaS Starter

Ce projet est un modèle de démarrage SaaS basé sur Spring Boot.

## Table des matières
- [Fonctionnalités](#fonctionnalités)
- [Installation](#installation)
- [Développement](#développement)
- [Compilation](#compilation)
- [Tests](#tests)
- [Docker](#docker)
- [Déploiement](#déploiement)
- [Variables d'environnement](#variables-denvironnement)
- [Contribution](#contribution)
- [Licence](#licence)

## Fonctionnalités
- Spring Boot 3 avec sécurité intégrée (JWT, authentification & autorisation)
- Gestion des utilisateurs et des abonnements
- Intégration des paiements avec Stripe
- Génération et gestion des factures
- Support des emails via Spring Mail et Thymeleaf
- API REST bien documentée avec OpenAPI
- Conteneurisation avec Docker

## Installation
### Prérequis
- Java 17 ou plus récent
- Maven 3.9+
- Docker (optionnel, pour le déploiement en conteneur)
- PostgreSQL (par défaut, mais configurable)

### Étapes
1. Cloner le dépôt :
   ```sh
   git clone <repository-url>
   cd spring-saas-starter
   ```
2. Installer les dépendances :
   ```sh
   mvn clean install
   ```
3. Configurer la base de données en modifiant `src/main/resources/application.yml`

## Développement
Lancer le serveur de développement :
```sh
mvn spring-boot:run
```
L'application sera accessible à `http://localhost:8080/`.

## Compilation
Pour créer une version prête pour la production :
```sh
mvn package
```
Le fichier JAR résultant se trouve dans `target/`.

## Tests
Exécuter les tests unitaires avec Maven :
```sh
mvn test
```

## Docker
### Exécution avec Docker Compose
```sh
docker-compose up --build
```

### Construction et exécution en production
```sh
docker build -t spring-saas-starter .
docker run -p 8080:8080 spring-saas-starter
```

## Déploiement
Pour le déploiement en CI/CD, configurez GitHub Actions ou un pipeline similaire. Un exemple de workflow est disponible dans `.github/workflows/main.yml`.

## Variables d'environnement
Définissez les variables suivantes pour la production :
- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL=jdbc:postgresql://<host>:<port>/<db>`
- `JWT_SECRET=<votre_clé_secrète>`
- `STRIPE_SECRET_KEY=<clé_secret_stripe>`

## Contribution
Les contributions sont les bienvenues ! Veuillez forker le dépôt, apporter vos modifications et soumettre une pull request.

## Licence
Ce projet est sous licence MIT.
