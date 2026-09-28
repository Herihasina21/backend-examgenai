# ExamGenAI — Backend

Backend du projet de génération d'examens par chapitre (Spring Boot, PostgreSQL, Google Gemini).

Projet GLA — Master 1, 2026.

## Équipe

| Membre | Rôle | Branche |
|--------|------|---------|
| Ladina | Extraction des chapitres, pages Upload/Cours | `feat/chapter-extraction` |
| Herihasina | Génération IA, API examens | `feat/ai-generation` |
| Tsiory | CRUD questions, export PDF/Word | `feat/question-crud` |

## Fonctionnement

1. Upload d'un cours (PDF, Word ou TXT)
2. Extraction des chapitres
3. Génération d'un examen via Gemini (QCM, vrai/faux, questions ouvertes)
4. Modification des questions
5. Export en PDF ou Word

```
Upload → Chapitres → Génération IA → Édition → Export
```

Le détail des étapes et des endpoints est dans [PLAN_PROJET.md](./PLAN_PROJET.md) (fichier local, non versionné).

## Technologies

- Java 21, Spring Boot 3.5
- PostgreSQL
- Apache PDFBox, Apache POI (lecture des documents)
- Google Gemini API (tier gratuit via AI Studio)
- Frontend prévu en React (repo séparé)

## Prérequis

- Java 21
- Maven 3.9+
- PostgreSQL avec une base nommée `examgenai`

## Installation

### Cloner le dépôt

```bash
git clone git@github.com:Herihasina21/backend-examgenai.git
cd backend-examgenai
```

### Configuration locale

Chacun utilise son propre mot de passe PostgreSQL. Il ne doit pas être commité.

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Éditer `application-local.properties` :
- renseigner le mot de passe PostgreSQL local
- renseigner la cle Gemini (obtenue sur Google AI Studio)

Le profil `local` est active dans `application.properties`. Pas besoin d'argument Maven en plus.

Cle gratuite Gemini : [Google AI Studio](https://aistudio.google.com/apikey)

### Test de l'IA
Pour tester la generation d'examens :
1. git pull
2. Creer une cle sur Google AI Studio
3. Copier application-local.properties.example → application-local.properties
4. Renseigner mot de passe PostgreSQL et cle Gemini (fichier ignore par Git)
5. mvn spring-boot:run puis POST /api/exams/generate


### Lancer l'application

```bash
mvn spring-boot:run
```

L'API tourne sur http://localhost:8080

### Endpoints principaux

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/courses/upload` | Upload d'un cours |
| GET | `/api/courses` | Liste des cours |
| GET | `/api/chapters/course/{courseId}` | Chapitres d'un cours |
| POST | `/api/exams/generate` | Génération d'examen (IA) |
| GET | `/api/exams/{id}` | Détail d'un examen |
| GET | `/api/exams/chapter/{chapterId}` | Examens d'un chapitre |

Tests manuels : Bruno ou Postman.

## Git

On travaille par branches, pas directement sur `main`.

```bash
git checkout main
git pull origin main
git checkout -b feat/nom-de-la-fonctionnalite

# développement...

git add .
git commit -m "feat: description courte"
git push -u origin feat/nom-de-la-fonctionnalite
```

Conventions de commit : `feat:`, `fix:`, `chore:`, `test:`.

Merge via Pull Request après relecture.

### Fichiers à ne pas committer

- `application-local.properties` (mot de passe, clé Gemini)
- `uploads/` (fichiers uploadés)
- `PLAN_PROJET.md` (notes d'équipe en local)

### Fichiers partagés

- `application.properties` (config commune, sans secrets)
- `application-local.properties.example` (modèle à copier)

## État du projet

| Partie | Statut |
|--------|--------|
| Upload cours, CRUD cours | Fait |
| Liste des chapitres + contenu | Fait |
| Génération IA (Gemini) | Fait |
| CRUD questions | Fait |
| Export PDF / Word | Fait |
| Tests JUnit | Partiel (`ChapterExtractorTest`) |
| Jenkins / CI | À faire |
| Frontend React | À faire |

## Cours GLA

Le projet suit les pratiques vues en cours :

- **Git** : branches, merge, travail à plusieurs — déjà en place
- **Refactoring** : prévu sur `CourseService` (extraction PDF/chapitres)
- **JUnit** : un test de démarrage pour l'instant, à compléter
- **Jenkins** : pipeline Maven à mettre en place plus tard

Exemple minimal de pipeline :

```groovy
pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps { sh 'mvn clean test' }
        }
    }
}
```

## Structure

```
src/main/java/com/mycompany/examgenai_backend/
├── config/
├── controller/
├── dto/
├── entity/
├── enums/
├── exception/
├── repository/
└── service/
```

## Déploiement Render (Docker) + Supabase

Le backend est dockerise (`Dockerfile`). Render deploie cette image.
La base Postgres recommandee est **Supabase** (plus durable que le Postgres Free Render 30 jours).

1. Pousser la branche `chore/render-deploy-v2` (ou merger sur `main`).
2. Creer un projet Postgres sur Supabase ; noter host, user et mot de passe.
3. Sur [render.com](https://render.com) : **New +** → **Web Service** → repo `backend-examgenai`
4. Reglages Web Service :
   - **Branch** : `chore/render-deploy-v2` (ou `main`)
   - **Runtime** : Docker
   - **Dockerfile Path** : `./Dockerfile`
   - **Instance** : Free
5. Variables d'environnement (valeurs reelles uniquement dans Render, jamais dans Git) :
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `SPRING_DATASOURCE_URL` = JDBC vers Supabase (`jdbc:postgresql://HOST:5432/postgres`)
   - `SPRING_DATASOURCE_USERNAME` = user Supabase
   - `SPRING_DATASOURCE_PASSWORD` = mot de passe du projet Supabase
   - `GEMINI_API_KEY` = cle Google AI Studio
   - `GEMINI_MODELS` = (optionnel) liste de modeles separes par des virgules ; fallback auto si 503/429
   - `APP_CORS_ALLOWED_ORIGINS` = URL Vercel + `http://localhost:5173`
   - `FILE_UPLOAD_DIR` = `/tmp/uploads`
6. Deployer, puis tester : `https://VOTRE-SERVICE.onrender.com/api/courses`

Le plan Free Render met le service en veille apres inactivite : le 1er appel peut etre lent.

### Lancer en local avec Docker

```bash
export POSTGRES_PASSWORD=...   # mot de passe local compose
export GEMINI_API_KEY=...      # cle Gemini
docker compose up --build
```

API : http://localhost:8080

Fichiers : `Dockerfile`, `docker-compose.yml`, `.dockerignore`, `application-prod.properties`.

## Licence

Projet académique — Master 1 GLA 2026.
