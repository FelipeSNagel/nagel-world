# VPS com Google Drive

Este fluxo hospeda o servidor na VPS e usa Google Drive como destino de
backup. O mundo ativo continua no disco da VPS enquanto o servidor roda.

## 1. Configurar rclone no Mac

Instale e configure um remote chamado `gdrive`:

```bash
brew install rclone
rclone config
```

No `rclone config`, crie um remote de tipo `drive`. Teste:

```bash
rclone lsd gdrive:
```

No `.env`, deixe:

```env
CRAFT_CLOUD_REMOTE=gdrive
CRAFT_CLOUD_PATH=minecraft-nagel/craftlandia-backups
```

Envie o backup atual para o Drive:

```bash
make craft-backup-cloud
make craft-cloud-latest
```

## 2. Preparar a VPS

Na VPS Ubuntu:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg make rclone
```

Instale Docker seguindo a documentacao oficial da Docker para Ubuntu. Depois,
confirme:

```bash
docker --version
docker compose version
```

Libere a porta Bedrock no firewall da VPS e no painel do provedor:

```bash
sudo ufw allow 19132/udp
sudo ufw status
```

## 3. Copiar o projeto para a VPS

No Mac, troque `ubuntu@IP_DA_VPS` pelo seu acesso:

```bash
rsync -av \
  --exclude craftlandia-data \
  --exclude craftlandia-backups \
  --exclude data \
  /Users/felipenagel/Projects/games/minecraft-nagel-servers/ \
  ubuntu@IP_DA_VPS:~/minecraft-nagel-servers/
```

Copie tambem a configuracao do rclone:

```bash
ssh ubuntu@IP_DA_VPS 'mkdir -p ~/.config/rclone'
scp ~/.config/rclone/rclone.conf ubuntu@IP_DA_VPS:~/.config/rclone/rclone.conf
```

Na VPS, teste:

```bash
cd ~/minecraft-nagel-servers
rclone lsf gdrive:minecraft-nagel/craftlandia-backups
```

## 4. Restaurar e subir na VPS

Na VPS:

```bash
cd ~/minecraft-nagel-servers
make craft-restore-cloud-latest
make craft-start
```

Ou use o modo resiliente:

```bash
make craft-start-resilient
```

Esse comando usa `craftlandia-data` se ela existir. Se nao existir, baixa o
backup mais recente do Drive, restaura e sobe o servidor.

## 5. Backup automatico para o Drive

Na VPS:

```bash
make install-craft-cloud-backup-cron
make cron
```

Por padrao, isso roda todo dia as 05:00. Para outro horario:

```bash
./scripts/install-craftlandia-cloud-backup-cron.sh "0 4 * * *"
```

Logs:

```bash
tail -f craftlandia-backups/cloud-backup.log
```

## 6. Como conectar

Celular:

```text
IP: IP_PUBLICO_DA_VPS
Porta: 19132
```

PS5:

Use BedrockTogether ou MC Server Connector no celular, apontando para:

```text
IP: IP_PUBLICO_DA_VPS
Porta: 19132
```

Depois entre no PS5 pelo servidor LAN que aparecer.

## 7. Seguranca

Antes de deixar publico, ative whitelist:

```bash
make craft-whitelist USERS=".SeuGamertag,.OutroGamertag"
```

Jogadores Bedrock via Floodgate normalmente aparecem com prefixo `.`.

