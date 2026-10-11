# SmartPark - Frontend Web Client

> **Single-Page Application | Responsive UI | Vercel Ready**

The client interface for Drivers, Parking Operators, and Administrators.

---

## 🚀 Quick Start (Local Development)

You can serve the static files using any local HTTP web server:

```bash
# Using Python 3:
python -m http.server 3000

# OR using Node.js:
npx serve . -p 3000
```
Open **http://localhost:3000** in your browser.

---

## 🌐 Connecting to the Backend

By default, the client can connect to:
- Local Backend: `http://localhost:8080`
- Production Render Backend: `https://<YOUR_RENDER_SERVICE_NAME>.onrender.com`

You can change the target API URL directly in the application header via the **API Server** input field.

---

## ☁️ Deployment to Vercel

### Method 1: Via Vercel Dashboard (GitHub Integration)
1. Import the repository into Vercel.
2. In Project Settings, set **Root Directory** to `frontend`.
3. Set Framework Preset to **Other**.
4. Click **Deploy**.

### Method 2: Via Vercel CLI
```bash
cd frontend
vercel deploy --prod
```

---

## 🎨 Theme & Tech
- **Theme**: Deep Blue (`#1e3a8a`), Emerald Green (`#10b981`), Clean Grayscale
- **Libraries**: Bootstrap 5.3, Bootstrap Icons, Chart.js 4.4 (CDN)
