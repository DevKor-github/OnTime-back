# Oracle Cloud Deployment

This guide deploys the backend to an Oracle Cloud Always Free VM with Docker Compose.

## Target Shape

- OCI region: Seoul
- VM shape: `VM.Standard.A1.Flex`
- OS: Ubuntu 24.04 LTS or Ubuntu 22.04 LTS
- First-pass public API: `http://<oracle-public-ip>:8080`
- Containers:
  - `ontime-backend`
  - `ontime-mysql`
- Secrets directory on VM: `/etc/ontime`

## Oracle Networking

Open these ingress rules in the VM subnet security list or network security group:

- TCP `22` from your IP for SSH
- TCP `8080` from the client/test IP range for the backend

Do not expose MySQL port `3306` publicly.

## VM Setup

SSH into the VM, then install Docker:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Log out and SSH back in so the Docker group membership takes effect.

## Secret Files

Create the server config directory:

```bash
sudo mkdir -p /etc/ontime
sudo chmod 750 /etc/ontime
```

Create `/etc/ontime/mysql.env`:

```bash
sudo tee /etc/ontime/mysql.env > /dev/null <<'EOF'
MYSQL_ROOT_PASSWORD=replace-with-a-strong-mysql-password
EOF
sudo chmod 600 /etc/ontime/mysql.env
```

Create `/etc/ontime/application.properties`:

```properties
spring.application.name=ontime-back

spring.datasource.url=jdbc:mysql://mysql:3306/ontime_db?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=replace-with-the-same-mysql-password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database=mysql
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

spring.flyway.enabled=true
spring.flyway.url=${spring.datasource.url}
spring.flyway.user=${spring.datasource.username}
spring.flyway.password=${spring.datasource.password}
spring.flyway.baseline-on-migrate=true

jwt.secret.key=replace-with-a-long-random-secret
jwt.access.expiration=3600000
jwt.refresh.expiration=1209600000
jwt.access.header=Authorization
jwt.refresh.header=Authorization-refresh

google.web.client-id=replace-me
google.app.client-id=replace-me
spring.security.oauth2.client.registration.google.client-secret=replace-me

spring.security.oauth2.client.registration.kakao.client-id=replace-me

apple.client.id=replace-me
apple.team.id=replace-me
apple.login.key=replace-me
apple.client.secret=

firebase.credentials.path=/etc/ontime/firebase-adminsdk.json
feature.apple-login.enabled=false

logging.level.root=INFO
logging.level.devkor.ontime_back=INFO
```

Copy the Firebase Admin SDK JSON to:

```bash
/etc/ontime/firebase-adminsdk.json
```

Then lock down permissions:

```bash
sudo chown -R root:root /etc/ontime
sudo chmod 600 /etc/ontime/application.properties /etc/ontime/mysql.env /etc/ontime/firebase-adminsdk.json
```

## Deploy

Clone the repository and start the services:

```bash
git clone <repo-url>
cd OnTime-back/ontime-back
docker compose -f docker-compose.prod.yml up -d --build
```

Check status:

```bash
docker compose -f docker-compose.prod.yml ps
docker logs --tail=100 ontime-backend
curl http://localhost:8080/health
```

From your machine, test:

```bash
curl http://<oracle-public-ip>:8080/health
```

## Update Deploy

```bash
cd OnTime-back/ontime-back
git pull
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

## Later

When a domain is available, add Nginx and Let's Encrypt, then expose only ports `80` and `443`.
