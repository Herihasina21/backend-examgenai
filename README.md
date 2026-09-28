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

```properties
spring.datasource.password=votre_mot_de_passe
```

Le profil `local` est activé dans `application.properties`. Pas besoin d'argument Maven en plus.

Pour la génération IA (Herihasina), ajouter aussi :

```properties
gemini.api-key=votre_cle_gemini
```

Clé gratuite : [Google AI Studio](https://aistudio.google.com/apikey)

### Test de l'IA
Pour tester la génération d’examens :
git pull
Créer une clé gratuite sur https://aistudio.google.com/apikey
Copier application-local.properties.example → application-local.properties
Mettre votre mot de passe PostgreSQL + gemini.api-key=...
mvn spring-boot:run puis POST /api/exams/generate


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

## Déploiement Render (Docker)

Le backend est dockerisé (`Dockerfile`). Render déploie cette image.

1. Pousser la branche `chore/render-deploy` (ou merger sur `main`).
2. Créer une base Postgres durable (ex. **Supabase**) et noter Host, User, Password.
3. Sur [render.com](https://render.com) : **New +** → **Web Service** → repo `backend-examgenai`
4. Réglages Web Service :
   - **Branch** : `chore/render-deploy` (ou `main`)
   - **Runtime** : Docker
   - **Dockerfile Path** : `./Dockerfile`
   - **Instance** : Free
5. Variables d'environnement :

| Variable | Exemple |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | mot de passe Postgres (Supabase / autre) |
| `GEMINI_API_KEY` | clé [AI Studio](https://aistudio.google.com/apikey) |
| `APP_CORS_ALLOWED_ORIGINS` | `https://votre-front.vercel.app,http://localhost:5173` |
| `FILE_UPLOAD_DIR` | `/tmp/uploads` |

> URI Supabase `postgresql://...` → JDBC : préfixer avec `jdbc:` (ex. `jdbc:postgresql://db.xxx.supabase.co:5432/postgres`).

6. Déployer, puis tester : `https://VOTRE-SERVICE.onrender.com/api/courses`

Le plan Free met le service en veille après inactivité (~15 min) : le 1er appel peut être lent.

### Lancer en local avec Docker

```bash
export GEMINI_API_KEY=votre_cle
docker compose up --build
```

API : http://localhost:8080 — Postgres : localhost:5432

Fichiers : `Dockerfile`, `docker-compose.yml`, `.dockerignore`, `application-prod.properties`.

## Licence

Projet académique — Master 1 GLA 2026.
