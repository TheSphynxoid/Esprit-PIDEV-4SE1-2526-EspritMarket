Here is your README translated into English as a plain text:

---

# 🛍️ Esprit Market Frontend

A modern and complete marketplace platform built with Angular 21

---

## 📋 Overview

Esprit Market Frontend is an enterprise web application for a multifunctional marketplace platform. It provides an immersive user experience with full support for vendors, service providers, delivery agents, recruiters, and administrators.

### ✨ Key Features

* Secure Authentication – Sign up, login, password recovery
* Role-based Dashboards – Dedicated dashboards for each role (admin, vendor, deliverer, recruiter, service provider)
* Integrated Marketplace – Buy and sell products and services
* Real-time Delivery Tracking – Google Maps and WebSocket integration
* Order Management – Cart and payment system
* Job Offers – Integrated recruitment platform
* Event Planning – Dedicated module
* Responsive Design – Modern UI with Tailwind CSS

---

## 🚀 Quick Start

### Prerequisites

* Node.js >= 18.x
* npm >= 11.7.0
* Angular CLI >= 21.1.4

### Installation

# 1. Clone the repository

git clone [https://github.com/your-username/esprit-market-frontend.git](https://github.com/your-username/esprit-market-frontend.git)
cd esprit-market-frontend

# 2. Install dependencies

npm install

# 3. Configure environment

# Copy environment file and add your API keys

cp src/environments/environment.ts src/environments/environment.local.ts

---

### Configuration

Edit src/environments/environment.ts and add:

export const environment = {
production: false,
apiUrl: '[http://localhost:8080/api](http://localhost:8080/api)',
googleMapsApiKey: 'YOUR_GOOGLE_MAPS_API_KEY',
wsUrl: 'ws://localhost:8080/ws'
};

---

### Run Development Server

# Start development server

npm start

# Start with local configuration

npm run start:local

The application will be available at [http://localhost:4200](http://localhost:4200)

---

## 🏗️ Project Architecture

### Folder Structure

src/
├── app/
│   ├── features/
│   │   ├── auth/
│   │   ├── frontoffice/
│   │   │   ├── home/
│   │   │   ├── marketplace/
│   │   │   ├── job-offers/
│   │   │   ├── events/
│   │   │   ├── delivery-tracking-live/
│   │   │   └── my-packages/
│   │   └── backoffice/
│   │       ├── admin-dashboard/
│   │       ├── vendor-dashboard/
│   │       ├── deliverer-dashboard/
│   │       ├── recruiter-dashboard/
│   │       └── service-provider-dashboard/
│   ├── services/
│   │   ├── api/
│   │   ├── auth.service.ts
│   │   ├── cart.service.ts
│   │   ├── order.service.ts
│   │   └── ...
│   ├── shared/
│   │   ├── components/
│   │   ├── layout/
│   │   └── directives/
│   ├── guards/
│   └── app.routes.ts
├── environments/
├── styles/
└── assets/

---

### Used Patterns

* Modularization – Feature-based structure
* Standalone Components – No NgModules
* Reactive Programming – RxJS
* Signals – Angular reactive state
* Dependency Injection – inject()
* Route Guards – Protection of routes
* HTTP Interceptors – Token and headers management

---

## 📦 Available Scripts

npm start
npm run start:local
npm run build
npm run watch
npm test
npm run ng -- help

---

## 🔧 Technologies Used

Angular ^21.1.0 – Main framework
TypeScript ~5.9.2 – Language
RxJS ~7.8.0 – Reactive programming
Tailwind CSS ^3.4.1 – Styling
Google Maps ^21.2.4 – Maps and tracking
STOMP.js ^7.3.0 – WebSocket
SockJS ^1.6.1 – WebSocket fallback
PostCSS ^8.5.6 – CSS processing

---

## 📚 API Services

Authentication

* AuthApiService – User session management
* JwtService – JWT tokens
* BasicAuthService – Basic authentication

Business

* MarketplaceApiService – Products and services
* OrderApiService – Orders and cart
* DeliveryApiService – Deliveries
* EventPlanningApiService – Events
* SrvApiService – Services

Utility

* ApiConfigService – API configuration
* JwtInterceptor – Token injection
* BasicAuthInterceptor – Basic auth

---

## 🔐 Security

* Secure JWT authentication
* Route guards
* HTTP interceptors
* Environment variables
* HTTPS recommended

---

## 🧪 Testing

Run tests:
npm test

With coverage:
ng test --code-coverage

Framework: Jasmine with Karma

---

## 🚢 Deployment

### Production Build

npm run build

Files generated in dist/esprit-market-frontend/

### Hosting

Vercel
Firebase Hosting
Nginx
Docker

### nginx.conf example

server {
listen 80;
server_name your-domain.com;
root /var/www/esprit-market-frontend;
index index.html;

```
location / {  
    try_files $uri $uri/ /index.html;  
}  
```

}

---

## 📝 Code Conventions

* camelCase for variables and methods
* kebab-case for files
* standalone components
* inject() for services
* Tailwind CSS
* Prettier formatting

Format code:
npm run prettier

---

## 🤝 Contribution

1. Fork the repository
2. Create a branch
3. Commit changes
4. Push
5. Open Pull Request

---

## 📋 Pre-commit Checklist

* Code formatted
* No errors
* Tests passed
* Build successful
* No unused dependencies
* Environment configured
* Clear commit message

---

## 🐛 Troubleshooting

Port issue:
ng serve --port 4300

Clean npm:
npm cache clean --force
rm -rf node_modules
npm install

Google Maps: check API key and config
WebSocket: check backend and URL

---

## 📞 Support

Email: [support@espritmarket.com](mailto:support@espritmarket.com)
Issues: [https://github.com/espritmarket/frontend/issues](https://github.com/espritmarket/frontend/issues)
Docs: [https://github.com/espritmarket/frontend/wiki](https://github.com/espritmarket/frontend/wiki)

---

## 📄 License

MIT License

---

## 👥 Authors

Esprit Market Team
Contributors

---

## 🙏 Acknowledgements

Angular Team
Tailwind CSS
Google Maps API
STOMP.js

---

If this project helped you, don’t forget to give it a star ⭐

---

Last update: 31/03/2026 🚀
