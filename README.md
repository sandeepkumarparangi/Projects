# AI Resume Analyzer

Upload a resume and get an ATS-style score, resume improvements, missing keywords, suggested keywords, and formatting tips.

## Tech Stack

- React 19 + Vite frontend
- Spring Boot 3 backend
- OpenAI Responses API integration
- PDF, DOCX, and TXT resume parsing
- GitHub Actions CI for backend tests and frontend build

## Features

- Resume upload with file validation
- Optional job description matching
- ATS score from 0-100
- Strengths, improvements, keyword gaps, suggested keywords, and formatting tips
- OpenAI-powered analysis when `OPENAI_API_KEY` is configured
- Local fallback analyzer when no API key is present, so demos still work
- Result diagnostics showing whether the score came from OpenAI or the local fallback

## Run Locally

### 1. Configure environment

Copy `.env.example` values into your shell or IDE run configuration.

```bash
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-5-mini
export OPENAI_TIMEOUT_MILLIS=4500
export FRONTEND_ORIGIN=http://localhost:5173,http://localhost:5175,https://sandeepkumarparangi.github.io
export VITE_API_BASE_URL=http://localhost:8080
```

The app still runs without `OPENAI_API_KEY`, but results will use the local fallback analyzer.
To keep analysis fast, the backend limits OpenAI input size, caps output length, caches repeated analyses, and falls back to the local ATS analyzer when the OpenAI call exceeds `OPENAI_TIMEOUT_MILLIS`.
If the UI says `OPENAI_API_KEY is not configured`, the API is working but OpenAI is not enabled for that run.

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

Health check:

```bash
curl http://localhost:8080/api/health
```

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

### Docker

```bash
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## API

`POST /api/resumes/analyze`

Multipart form fields:

- `resume`: required PDF, DOCX, or TXT file, max 5 MB
- `jobDescription`: optional text

Example:

```bash
curl -X POST http://localhost:8080/api/resumes/analyze \
  -F "resume=@resume.pdf" \
  -F "jobDescription=Java Spring Boot React developer"
```

## GitHub Actions

The workflow in `.github/workflows/ci.yml` runs on pushes and pull requests to `main`.

- Backend job: installs Java 17 and runs `mvn -B test`
- Frontend job: installs Node 22 dependencies with `npm ci` and runs `npm run build`

Deployment workflows are included:

- `.github/workflows/pages.yml`: builds and deploys the React frontend to GitHub Pages.
- `.github/workflows/deploy.yml`: builds backend and frontend Docker images and publishes them to GitHub Container Registry.

For GitHub Pages, set repository variable `VITE_API_BASE_URL` to your deployed backend URL, for example `https://your-api.example.com`.
For OpenAI in production, store `OPENAI_API_KEY` as a secret in the platform where the Spring Boot backend runs. Do not put it in frontend variables.

## Public Web Application

GitHub Pages can publish the React frontend at:

```text
https://sandeepkumarparangi.github.io/Projects/
```

The Spring Boot backend cannot run on GitHub Pages because Pages only serves static files. To make the full AI analyzer public:

1. Deploy the Spring Boot backend Docker image to a backend host such as Render, Railway, Fly.io, AWS, GCP, or Azure.
2. Add `OPENAI_API_KEY` as a backend secret on that host.
3. Set the backend environment variable `FRONTEND_ORIGIN=https://sandeepkumarparangi.github.io`.
4. Set the GitHub repository variable `VITE_API_BASE_URL` to the public backend URL, for example `https://ai-resume-analyzer-api.example.com`.
5. Push to `main` or manually run **Deploy frontend to GitHub Pages** from the GitHub Actions tab.

For local verification, use the sample files:

```bash
curl -X POST http://localhost:8080/api/resumes/analyze \
  -F "resume=@examples/sample-resume.txt" \
  -F "jobDescription=<examples/sample-job-description.txt"
```

Expected demo result without an OpenAI key:

- `atsScore`: `92`
- `verdict`: `Strong ATS match`
- `analysisSource`: `local`
- `missingKeywords`: `postgresql`, `observability`, `cloud`, `oauth`, `performance`
- `diagnostic`: `OPENAI_API_KEY is not configured; using local analyzer.`

The React app also includes a **Use sample resume and job** button. Click it, then click **Analyze resume** to demo the full upload-to-results flow in the browser.

## Step-by-step deployment

1. Push this project to GitHub.
2. In GitHub, open **Settings > Pages** and enable Pages from the `gh-pages` branch after the first deployment run.
3. Open **Settings > Secrets and variables > Actions > Variables**.
4. Add `VITE_API_BASE_URL` with your public Spring Boot backend URL.
5. Open **Actions > Deploy frontend to GitHub Pages** and run the workflow.
6. Open **Actions > Build and publish containers** to publish Docker images to GitHub Container Registry.
7. Deploy the backend image to a Java/Docker host such as Render, Railway, Fly.io, AWS, GCP, or Azure.
8. Add `OPENAI_API_KEY` as a backend secret on that host.
9. Set backend `FRONTEND_ORIGIN` to `https://sandeepkumarparangi.github.io`.

GitHub Pages can host the React frontend publicly. The Spring Boot + OpenAI backend must run on a backend host because GitHub Pages only serves static files.

Before pushing to GitHub for the first time:

```bash
git init
git add .
git commit -m "Build AI resume analyzer"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/ai-resume-analyzer.git
git push -u origin main
```

Do not commit `.env` files or API keys. Store deployment secrets in GitHub repository secrets or in the backend hosting provider.

After pushing, open the repository's **Actions** tab. The `CI`, `Deploy frontend to GitHub Pages`, and `Build and publish containers` workflows can be run from there.

## Project Structure

```text
backend/                 Spring Boot API
frontend/                React + Vite app
.github/workflows/       GitHub Actions CI
```
