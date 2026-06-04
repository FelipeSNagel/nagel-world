# Coolify na Hostinger

Use o Coolify para rodar o container. Use SSH na VPS para puxar o mundo do
Google Drive antes do deploy.

## 1. Copiar a config do rclone para a VPS

No Mac:

```bash
scp ~/.config/rclone/rclone.conf root@IP_DA_VPS:/tmp/rclone.conf
```

Na VPS:

```bash
mkdir -p ~/.config/rclone
mv /tmp/rclone.conf ~/.config/rclone/rclone.conf
rclone lsf gdrive:minecraft-nagel/craftlandia-backups
```

Se o usuario SSH nao for `root`, troque `root@IP_DA_VPS` pelo usuario correto.

## 2. Restaurar o mundo na VPS

Na VPS:

```bash
apt update
apt install -y rclone tar
mkdir -p /data/minecraft-nagel/craftlandia-backups

latest="$(
  rclone lsf gdrive:minecraft-nagel/craftlandia-backups --files-only \
    | grep '^craftlandia-[0-9]\{8\}-[0-9]\{6\}\.tar\.gz$' \
    | sort \
    | tail -n 1
)"

rclone copyto \
  "gdrive:minecraft-nagel/craftlandia-backups/$latest" \
  "/data/minecraft-nagel/craftlandia-backups/$latest"

cd /data/minecraft-nagel
tar -xzf "craftlandia-backups/$latest"
chown -R 1000:1000 /data/minecraft-nagel/craftlandia-data
```

Se o container ja estiver rodando no Coolify, use o script de restore da VPS
para parar o container, baixar o ultimo backup e iniciar de novo:

```bash
scp scripts/vps-restore-latest-craftlandia-cloud.sh root@IP_DA_VPS:/opt/vps-restore-latest-craftlandia-cloud.sh
ssh root@IP_DA_VPS
chmod +x /opt/vps-restore-latest-craftlandia-cloud.sh
/opt/vps-restore-latest-craftlandia-cloud.sh
```

Se voce copiou o projeto inteiro para a VPS, tambem pode rodar:

```bash
make vps-craft-restore-cloud-latest
```

## 3. Criar o serviço no Coolify

No Coolify:

1. New Resource.
2. Docker Compose Empty.
3. Cole o conteudo de `docker-compose.coolify.yml`.
4. Configure as variaveis, principalmente `CRAFT_RCON_PASSWORD`.
5. Deploy.

O compose usa:

```text
/data/minecraft-nagel/craftlandia-data:/data
```

Entao o mundo restaurado pelo comando acima sera usado pelo servidor.

### Alternativa: restaurar pelo proprio compose

Se voce nao quer usar SSH para restaurar o backup antes do deploy, use
`docker-compose.coolify.auto-restore.yml` no lugar de `docker-compose.coolify.yml`.

No Mac, copie a config do rclone em base64:

```bash
base64 < ~/.config/rclone/rclone.conf | tr -d '\n' | pbcopy
```

No Coolify, crie uma variavel:

```text
RCLONE_CONFIG_B64=cole_aqui_o_valor_copiado
```

Depois cole o conteudo de `docker-compose.coolify.auto-restore.yml` no
`Docker Compose Empty` e faca o deploy.

Esse compose roda primeiro o servico `restore-from-drive`. Ele baixa o ultimo
backup do Drive, restaura em `/data/minecraft-nagel/craftlandia-data` e so
depois inicia o Minecraft.

Nao deixe `RESTORE_MODE=force` ligado no dia a dia, porque ele restaura o backup
de novo a cada deploy. Use `RESTORE_MODE=force` apenas quando quiser sobrescrever
o mundo atual.

## 4. Liberar porta

Libere UDP `19132` no firewall da VPS e no painel da Hostinger.

Conexao:

```text
IP_PUBLICO_DA_VPS
19132
```

## 5. Backup automatico para o Drive

O Coolify roda o container, mas o backup automatico deve rodar no host da VPS,
porque o `rclone` fica fora do container do Minecraft.

Copie o script para a VPS:

```bash
scp scripts/vps-backup-craftlandia-cloud.sh root@IP_DA_VPS:/opt/vps-backup-craftlandia-cloud.sh
```

Na VPS:

```bash
chmod +x /opt/vps-backup-craftlandia-cloud.sh
/opt/vps-backup-craftlandia-cloud.sh
```

Se voce copiar o projeto inteiro para a VPS, tambem pode rodar pelo Makefile:

```bash
make vps-craft-backup-cloud
```

Se funcionar, instale no cron para rodar todo dia as 05:00:

```bash
mkdir -p /data/minecraft-nagel/craftlandia-backups
crontab -l >/tmp/nagel-cron 2>/dev/null || true
sed -i '/nagel-craftlandia-cloud-backup/d' /tmp/nagel-cron
cat >>/tmp/nagel-cron <<'EOF'
0 5 * * * /opt/vps-backup-craftlandia-cloud.sh >> /data/minecraft-nagel/craftlandia-backups/cloud-backup.log 2>&1 # nagel-craftlandia-cloud-backup
EOF
crontab /tmp/nagel-cron
rm -f /tmp/nagel-cron
```

Esse backup sobe o arquivo para:

```text
gdrive:minecraft-nagel/craftlandia-backups
```

## O que fica salvo

Como o backup copia o diretorio inteiro `/data`, ele salva:

- mundos;
- inventarios;
- permissoes do LuckPerms;
- protecoes do GriefPrevention;
- configuracao do Geyser/Floodgate;
- dinheiro, homes, warps e TPs do EssentialsX;
- lojas do QuickShop.
