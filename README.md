# Zero Human Blog Engine 🌐🤖

An autonomous engine that generates, designs, and deploys complete production-ready websites — without any human intervention.

Built to power [ruckquest.com](https://ruckquest.com) and future AI-driven brands, the system uses state-of-the-art AI, secure cloud infrastructure, and scalable database architecture to create real businesses from scratch.

---

## 🧠 What It Does (Full Pipeline)

- 📥 Accepts a topic (e.g., "rucking", "mindful productivity")
- 🧹 Clears old database entries and resets site data
- 📝 Writes and SEO-optimizes:
  - Home page
  - About page
  - Blog landing page
  - Editorial page
- 🏷️ Generates:
  - Site name
  - Logo design
  - Brand identity
  - Founder profile and bio
- 🖼️ Creates and embeds AI-generated images
- 📈 Optimizes all SEO metadata and alt text automatically
- 🚀 Publishes everything live to a WordPress instance via API
- 🛡️ Secures credentials using AWS Secrets Manager

This system powers real public-facing sites — not demos.

---

## 🛠️ Technology Stack

### 🧠 AI Components
- **Google Gemini AI** (content, images, branding)
- **Google Cloud AI Platform**

### ☁️ Cloud Infrastructure
- **AWS Lambda** (compute orchestration)
- **AWS Systems Manager (SSM)** (configuration management)
- **AWS Secrets Manager** (secure credential storage)

### 💾 Backend & Databases
- **Java 21**
- **MySQL** (relational database for blog content, authors, metadata)
- **Maven** (dependency and build management)

### 🌐 APIs & Integrations
- WordPress REST API (for live publishing)
- Google APIs (AI services, Ads services)
- OkHttp + Apache HTTP Client (HTTP requests)

## 🚀 Key Features

- ✅ **One-Command Website Creation**
- ✅ **Full AI-generated branding, content, and media**
- ✅ **Secure cloud deployment via AWS Lambda**
- ✅ **Integrated SEO best practices at every step**
- ✅ **Designed for real-world production traffic and users**

## 🔒 Security Practices

- All API keys and DB credentials are stored in **AWS Secrets Manager**
- Dynamic environment management via **AWS Systems Manager Parameter Store**
- HTTPS-secured communications
- No hardcoded credentials in codebase

---

## 📈 Real-World Deployments

- [ruckquest.com](https://ruckquest.com) — AI-powered fitness blog  
- [mbt.dsc.mybluehost.me](https://mbt.dsc.mybluehost.me) — AI experimental site

Both live, monetizable blogs created and managed 100% by this engine.

---

## 🧠 Why This Matters

The **Zero Human Blog Engine** represents a step toward fully autonomous companies:
- Content is generated without humans
- Sites are launched and optimized automatically
- Outreach and marketing can be layered on top via sister projects like `marketingAgents`

This is a foundation for future AI-run digital asset creation at scale.

---

## 🛠️ How to Build & Deploy

1. Set up MySQL database
2. Configure AWS Lambda functions with appropriate IAM roles
3. Store all API keys securely in AWS Secrets Manager
4. Deploy application JARs to Lambda layers
5. Run `run-autoblog.sh` to trigger the full site generation cycle

---

## 📜 License

MIT — for personal, educational, and non-commercial use only.

Commercial deployment requires explicit permission.

---

## 🙋‍♂️ Author

Built by [Zach Derhake](https://github.com/zachd1234)

---



