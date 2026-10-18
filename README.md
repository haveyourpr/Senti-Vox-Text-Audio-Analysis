# Senti-Vox API v2

A Micronaut-based REST API for sentiment analysis with Upstash Redis token tracking.

---

## 📋 Table of Contents

- [Environment Variables](#environment-variables)
- [Commands](#commands)
- [Docker](#docker)
- [Deploying to Render.com](#deploying-to-rendercom)
- [API Documentation](#api-documentation)
- [Reference Links](#reference-links)

---

## Environment Variables

The app **will refuse to start** without these two variables set.

| Variable              | Description                                     |
|-----------------------|-------------------------------------------------|
| `UPSTASH_REDIS_URL`   | REST URL of your Upstash Redis database         |
| `UPSTASH_REDIS_TOKEN` | Bearer token for authenticating Upstash requests|

Micronaut automatically maps `UPSTASH_REDIS_URL` → `upstash.redis.url` and `UPSTASH_REDIS_TOKEN` → `upstash.redis.token`.

### Dev Environment Setup

**Option 1 — `.env` file (recommended for local dev)**

Create a `.env` file in the project root (already gitignored — add it if not):

```env
UPSTASH_REDIS_URL=https://your-db.upstash.io
UPSTASH_REDIS_TOKEN=your_token_here
```

Then load it before running (PowerShell):

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]*)=(.*)$") {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
    }
}
.\gradlew run
```

**Option 2 — `application-local.yml` (Micronaut native)**

Create/edit `src/main/resources/application-local.yml` (keep this out of version control):

```yaml
upstash:
    redis:
        url: https://your-db.upstash.io
        token: your_token_here
```

Then activate the `local` environment profile when running:

```bash
./gradlew run -Dmicronaut.environments=local
```

> ⚠️ Make sure `.env` and `application-local.yml` are listed in `.gitignore` — **never commit real credentials**.

---

## Commands

### Run in Development

```bash
# Windows
.\gradlew run -Dmicronaut.environments=local

# macOS / Linux
./gradlew run -Dmicronaut.environments=local
```

The server starts on **port `8086`** by default.  
Swagger UI → `http://localhost:8086/swagger/`  
Redoc UI  → `http://localhost:8086/redoc/`

### Build (Production Fat JAR)

```bash
# Windows
.\gradlew shadowJar

# macOS / Linux
./gradlew shadowJar
```

Output: `build/libs/api_v2-0.1-all.jar`

### Run the Production JAR

```bash
java -jar build/libs/api_v2-0.1-all.jar
```

---

## Docker

The multi-stage [Dockerfile](Dockerfile) builds the fat JAR with JDK 25 and runs it on a slim Alpine JRE 25 image as a non-root user.

### Build the image

```bash
docker build -t senti-vox-api .
```

### Run the container

```bash
docker run -d --name senti-vox-api -p 8086:8086   -e UPSTASH_REDIS_URL=https://your-db.upstash.io   -e UPSTASH_REDIS_TOKEN=your_token_here   senti-vox-api
```

Or reuse your `.env` file:

```bash
docker run -d --name senti-vox-api -p 8086:8086 --env-file .env senti-vox-api
```

The API is then available at `http://localhost:8086` (Swagger UI at `/swagger/`).

> `.dockerignore` excludes `.env` and `application-local.yml`, so credentials are never baked into the image. Always pass them at runtime.

---

## Deploying to Render.com

### Prerequisites

- A [Render](https://render.com) account
- Your Upstash Redis credentials ready
- The repository pushed to GitHub / GitLab

### Step-by-step

**1. Create a New Web Service**

Go to [Render Dashboard](https://dashboard.render.com) → **New** → **Web Service** → connect your repository.

**2. Configure the Build & Start Settings**

| Field             | Value                                        |
|-------------------|----------------------------------------------|
| **Environment**   | `Java`                                       |
| **Build Command** | `./gradlew shadowJar`                        |
| **Start Command** | `java -jar build/libs/api_v2-0.1-all.jar`   |

> 💡 Render detects the JDK automatically. If you need Java 25 specifically, set the **Environment Variable** `JAVA_VERSION=25`.

**3. Add Environment Variables**

In your Render service → **Environment** tab, add:

| Key                   | Value                        |
|-----------------------|------------------------------|
| `UPSTASH_REDIS_URL`   | `https://your-db.upstash.io` |
| `UPSTASH_REDIS_TOKEN` | `your_token_here`            |
| `MICRONAUT_SERVER_PORT` | `10000`  *(Render's default expected port)* |

> Render injects a `PORT` environment variable. Make sure your start command or `application.properties` uses it:
> ```
> micronaut.server.port=${PORT:8086}
> ```
> You can edit `src/main/resources/application.properties` to replace the hardcoded `8086` with `${PORT:8086}`.

**4. Set Health Check Path**

In Render → **Health & Alerts**, set the health check path to `/health` (Micronaut exposes this by default via `micronaut-management` — add it as a dependency if not present).

**5. Deploy**

Click **Deploy**. Render will:
1. Clone your repository
2. Run `./gradlew shadowJar` to produce the fat JAR
3. Launch the JAR with your environment variables injected
4. Serve it on your `.onrender.com` subdomain

### Subsequent Deploys

Push to your connected branch — Render auto-deploys on every commit.

---

## API Documentation

Once running, interactive docs are available at:

- **Swagger UI**: `/swagger/`
- **Redoc**: `/redoc/`
- **OpenAPI spec**: `/swagger/api_v2-0.1.yml`

---

## Reference Links

- [Micronaut 5.1.3 User Guide](https://docs.micronaut.io/5.1.3/guide/index.html)
- [Micronaut API Reference](https://docs.micronaut.io/5.1.3/api/index.html)
- [Micronaut Configuration Reference](https://docs.micronaut.io/5.1.3/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
- [Shadow Gradle Plugin](https://gradleup.com/shadow/)
- [Micronaut Gradle Plugin](https://micronaut-projects.github.io/micronaut-gradle-plugin/latest/)
- [GraalVM Gradle Plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Micronaut AOT](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)
- [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/index.html)
- [Micronaut Serialization Jackson](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)
- [Micronaut Redoc View](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/#redoc)
- [Redoc on GitHub](https://github.com/Redocly/redoc#generate-beautiful-api-documentation-from-openapi)
