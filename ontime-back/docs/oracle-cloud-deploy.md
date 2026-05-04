# Oracle Cloud Deployment

This guide deploys the backend to an Oracle Cloud Always Free VM with Docker Compose.

## Target Shape

- OCI region: South Korea North (Chuncheon), `ap-chuncheon-1`
- VM shape: `VM.Standard.A1.Flex`
- OS: Oracle Linux 9
- First-pass public API: `http://<oracle-public-ip>:8080`
- Containers:
  - `ontime-backend`
  - `ontime-mysql`
- Secrets directory on VM: `/etc/ontime`

## Retry VM Creation With OCI CLI

Use this when the Console returns the A1 capacity error and you want to retry later without filling out the browser form again.

Prerequisites on your machine:

- OCI CLI installed and configured with `oci setup config`
- `jq` installed
- An SSH public key, defaulting to `~/.ssh/id_ed25519.pub`
- Your compartment OCID. For the root compartment, this is the tenancy OCID.

Run from `ontime-back/`:

```bash
export COMPARTMENT_ID=ocid1.tenancy.oc1..replace-with-your-tenancy-or-compartment-ocid
export SSH_PUBLIC_KEY_FILE="$HOME/.ssh/id_ed25519.pub"

./scripts/oci-retry-a1-chuncheon.sh
```

The script creates or reuses:

- VCN: `ontime-vcn`
- Public subnet: `ontime-public-subnet`
- Internet gateway: `ontime-igw`
- Default route to the internet gateway
- Security list ingress for SSH `22` and backend `8080`
- A public IPv4 address on the VM launch

By default, SSH and API ingress are open from `0.0.0.0/0` so the first deploy is reachable. Restrict these before a real launch if you know your client IP:

```bash
export SSH_SOURCE_CIDR="$(curl -s https://ifconfig.me)/32"
export API_SOURCE_CIDR="$SSH_SOURCE_CIDR"
./scripts/oci-retry-a1-chuncheon.sh
```

The launch settings match the Console retry:

- Region: `ap-chuncheon-1`
- Availability domain: `Cxab:AP-CHUNCHEON-1-AD-1`
- Shape: `VM.Standard.A1.Flex`
- Shape config: `1` OCPU, `6` GB RAM
- Image: newest available Oracle Linux 9 image compatible with A1 Flex
- Instance name: `ontime`

If OCI is still out of capacity, the script fails at the `oci compute instance launch` step. Rerun the same command later; already-created network resources are reused.

To leave it retrying in the terminal, set retry controls:

```bash
export RETRY_ATTEMPTS=24
export RETRY_SLEEP_SECONDS=900
./scripts/oci-retry-a1-chuncheon.sh
```

That example retries every 15 minutes for up to 6 hours.

## Oracle Networking

Open these ingress rules in the VM subnet security list or network security group:

- TCP `22` from your IP for SSH
- TCP `8080` from the client/test IP range for the backend

Do not expose MySQL port `3306` publicly.

## VM Setup

SSH into the VM as `opc`, then install Docker:

```bash
sudo dnf -y update
sudo dnf -y install dnf-plugins-core git curl
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
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
