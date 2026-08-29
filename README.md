# ExamGenAI — Backend

Générateur d'examens personnalisés par chapitre avec IA (Spring Boot + PostgreSQL).

**Équipe :** Ladina · Herihasina · Tsiory · **Master 1 — Génie Logiciel Avancé (2026)**

| Membre | Rôle | Branche |
|--------|------|---------|
| **Ladina** | Extraction chapitres + pages Upload/Cours | `feat/chapter-extraction` |
| **Herihasina** | Génération IA + page Génération examen | `feat/ai-generation` |
| **Tsiory** | CRUD questions, Export + pages Édition/Export | `feat/question-crud` |

---

## Description

Application backend qui permet de :

1. Uploader un cours (PDF, Word, TXT)
2. Extraire automatiquement les chapitres
3. Générer un examen via OpenAI (QCM, vrai/faux, questions ouvertes)
4. Modifier les questions générées
5. Exporter l'examen en PDF ou Word

```
Upload → Extraction chapitres → Génération IA → Édition → Export
```

> Plan détaillé par étape : voir [PLAN_PROJET.md](./PLAN_PROJET.md)

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Java 21, Spring Boot 3.5 |
| Base de données | PostgreSQL |
| Extraction documents | Apache PDFBox, Apache POI |
| IA | OpenAI GPT API |
| Frontend (à venir) | React + Vite |

---

## Prérequis

- Java 21
- Maven 3.9+
- PostgreSQL (base `examgenai`)

---

## Installation

### 1. Cloner le repo

```bash
git clone <url-du-repo>
cd backend-examgenai
```

### 2. Configuration locale

Créer `src/main/resources/application-local.properties` (fichier ignoré par Git) :

```properties
spring.datasource.password=votre_mot_de_passe
openai.api-key=sk-votre-cle
```

Activer le profil local dans `application.properties` ou lancer avec :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Lancer l'application

```bash
mvn spring-boot:run
```

API disponible sur **http://localhost:8080**

### 4. Tester les endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/courses/upload` | Upload d'un cours |
| `GET` | `/api/courses` | Liste des cours |
| `GET` | `/api/chapters/course/{courseId}` | Chapitres d'un cours |

---

## Workflow Git (équipe)

### Bonnes pratiques (cours GLA)

- Travailler sur des **branches** dédiées, jamais directement sur `main`
- **Tester** avant de commiter (`mvn test` ou `mvn spring-boot:run`)
- Messages de commit clairs : `feat:`, `fix:`, `chore:`, `test:`
- **Pull Request** + review avant merge
- Mettre à jour sa branche : `git pull origin main`

### Commandes type

```bash
git checkout main
git pull origin main
git checkout -b feat/nom-de-la-fonctionnalite

# ... développement ...

git add .
git commit -m "feat: description du changement"
git push -u origin feat/nom-de-la-fonctionnalite
```

### Branches en cours

| Branche | Responsable | Tâche |
|---------|-------------|-------|
| `feat/chapter-extraction` | Ladina | Contenu réel des chapitres |
| `feat/ai-generation` | Herihasina | OpenAI + génération examens |
| `feat/question-crud` | Tsiory | CRUD questions + export |

### Fichiers à ne jamais committer

- `uploads/` — fichiers uploadés
- `application-local.properties` — mots de passe, clés API

---

## Alignement avec les cours GLA

Comparaison entre ce que nous faisons sur le projet et ce qu'enseignent les cours **Génie Logiciel Avancé M1 (2026)**.

### Verdict global

| Thème du cours | Statut | Détail |
|----------------|--------|--------|
| Git / SGV | ✅ En place | Branches, PR, merge, collaboration à 3 |
| Refactoring | ⚠️ À faire | `CourseService` à découper avant nouvelles features |
| Tests JUnit | ❌ À faire | Un seul test smoke (`contextLoads`) |
| Jenkins / CI | ❌ À faire | Pas de pipeline automatisé |

---

### Git — ✅ Conforme

**Cours :** *Initiation GIT*, *Systèmes de gestion de version*

- Travailler par branches
- Fusionner via merge / Pull Request
- Commits cohérents avec messages explicites
- Collaboration à plusieurs sur un dépôt distribué

**Ce que nous faisons :**

- Branche `fix/file-upload-config` → PR #1 → merge sur `main` ✅
- Messages `feat:`, `fix:` ✅
- `.gitignore` pour secrets et uploads ✅
- Équipe synchronisée via `git pull origin main` ✅

---

### Refactoring — ⚠️ Partiel

**Cours :** *Refactoring GLA M1*, *TP Refactoring*

- Améliorer le code **sans changer le comportement**
- Extraire méthodes / classes quand une classe grossit
- Refactoriser **avant** d'ajouter des fonctionnalités
- Historiser chaque étape dans Git

**État actuel :**

`CourseService` cumule upload, extraction PDF/Word/TXT, détection chapitres et persistance BDD.

**Actions prévues :**

- [ ] Extraire `FileTextExtractor` (PDF, Word, TXT)
- [ ] Extraire `ChapterExtractionService`
- [ ] Renommer `createChaptersFromTitles` → `createChaptersFromText`
- [ ] Commit Git à chaque petit refactoring

---

### Tests JUnit — ❌ À implémenter

**Cours :** *TP Tests unitaires avec JUnit*

- Approche **coder → tester → coder → tester**
- Tests unitaires avec `@Test`, `@Before`, assertions
- `mvn test` doit passer avant chaque merge

**État actuel :**

Un seul test dans `ExamgenaiBackendApplicationTests` :

```java
@Test
void contextLoads() { }
```

**Tests à ajouter :**

| Classe | Tests prévus |
|--------|--------------|
| `CourseService` | `extractChapterTitles`, `uploadCourseFile` |
| `ChapterService` | `getChaptersByCourse` |
| `ExamService` | `generateExam` (mock OpenAI) |
| `QuestionService` | CRUD questions |

**Commande :**

```bash
mvn test
```

---

### Jenkins / Intégration continue — ❌ À mettre en place

**Cours :** *Intégration continue avec Jenkins*, *TP0/TP1 Jenkins*

- Job Jenkins lié au dépôt Git
- Build automatique à chaque push
- Exécution de `mvn test`
- Historique des builds

**État actuel :** lancement manuel uniquement (`mvn spring-boot:run`).

**Étapes prévues :**

1. [ ] Installer Jenkins
2. [ ] Créer un job Maven ou Pipeline
3. [ ] Lier le repo GitHub
4. [ ] Pipeline : `mvn clean test` → `mvn package`
5. [ ] (Optionnel) SonarQube pour la qualité de code

**Exemple de `Jenkinsfile` :**

```groovy
pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn clean test'
            }
        }
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }
}
```

---

## Plan de développement

| Priorité | Tâche | Branche | Cours associé |
|----------|-------|---------|---------------|
| 🔴 1 | Extraction contenu chapitres | `feat/chapter-extraction` | Refactoring + JUnit |
| 🔴 2 | Génération IA (OpenAI) | `feat/ai-generation` | JUnit (mock API) |
| 🟡 3 | CRUD questions | `feat/question-crud` | JUnit |
| 🟡 4 | Export PDF/Word | `feat/export` | JUnit |
| 🟢 5 | Frontend React | repo séparé | — |
| 🟢 6 | Jenkins CI | `chore/jenkins-pipeline` | TP Jenkins |

Détail complet : [PLAN_PROJET.md](./PLAN_PROJET.md)

---

## Structure du projet

```
src/main/java/com/mycompany/examgenai_backend/
├── config/          # Security, CORS, ModelMapper
├── controller/      # REST API
├── dto/             # Data Transfer Objects
├── entity/          # Entités JPA
├── enums/           # FileType, QuestionType, DifficultyLevel
├── exception/       # GlobalExceptionHandler
├── repository/      # Spring Data JPA
└── service/         # Logique métier
```

---

## Ressources cours GLA

| Document | Sujet |
|----------|-------|
| Initiation GIT 2026 | Branches, merge, conflits, bonnes pratiques |
| Systèmes de gestion de version | Concepts SGV, modèles distribués |
| Refactoring GLA M1 | Techniques de refactoring |
| TP Refactoring | Exercices Eclipse + Git |
| TP Tests unitaires JUnit | TDD, Money/MoneyBag |
| Intégration continue Jenkins | CI, jobs, plugins |
| TP0/TP1 Jenkins | Installation, pipeline Maven |

---

## Licence

Projet académique — Master 1 ENI / GLA 2026.
