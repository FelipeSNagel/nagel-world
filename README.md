# Nagel Bedrock Server

Servidor Minecraft Bedrock Dedicated Server para jogar no Minecraft original
Bedrock em celulares, tablets, Windows, consoles e outros dispositivos
compativeis.

## Melhor caminho

Para jogar fora da mesma rede, a VPS e o melhor caminho na pratica:

- fica ligada 24/7;
- evita depender do roteador da casa;
- evita problema com CGNAT de internet residencial;
- facilita usar IP fixo ou dominio;
- deixa backup e atualizacao mais previsiveis.

Rodar em casa tambem funciona, mas para acesso externo voce precisa de port
forward UDP `19132` no roteador, IP publico real, firewall liberado e maquina
ligada.

## Requisitos

- Docker
- Docker Compose
- Porta UDP `19132` liberada para entrada

O servidor usa a imagem Docker `itzg/minecraft-bedrock-server`, que baixa e roda
o Bedrock Dedicated Server oficial.

## Configuracao

Edite o arquivo `.env`:

```env
SERVER_NAME=Nagel Bedrock
LEVEL_NAME=NagelWorld
GAMEMODE=survival
DIFFICULTY=normal
MAX_PLAYERS=10
ALLOW_LIST=false
ALLOW_LIST_USERS=
BEDROCK_PORT=19132
VERSION=LATEST
```

Para limitar quem entra, use:

```env
ALLOW_LIST=true
ALLOW_LIST_USERS=Gamertag1,Gamertag2,Gamertag3
```

Ou use o comando:

```bash
make allowlist USERS="Gamertag1,Gamertag2,Gamertag3"
```

Use os Gamertags exatos da conta Microsoft/Xbox. A allowlist controla quem pode
entrar no servidor; isso nao da permissao de administrador.

Para desligar a allowlist:

```bash
make disable-allowlist
```

## Rodar localmente ou na VPS

Servidor Bedrock vanilla atual:

```bash
docker compose up -d
docker compose logs -f bedrock
```

Com Make:

```bash
make start
make logs
```

Servidor Craftlandia/Paper com Geyser, Floodgate e plugins:

```bash
make craft-start
make craft-logs
```

Esse modo para o Bedrock vanilla antes de subir o Craftlandia, porque ambos
usam a porta Bedrock UDP `19132`.

Detalhes do modo Craftlandia/Paper ficam em
[docs/craftlandia.md](docs/craftlandia.md).

Checklist curto para restaurar na VPS/Coolify pelo Git:
[docs/restore-amanha.md](docs/restore-amanha.md).

Para liberar somente jogadores especificos no Craftlandia/Paper:

```bash
make craft-whitelist USERS=".Gamertag1,.Gamertag2"
```

Jogadores Bedrock via Floodgate normalmente aparecem com prefixo `.`, e espacos
do Gamertag viram `_`.

Parar:

```bash
make stop
make craft-stop
```

Atualizar o servidor Bedrock:

```bash
make update
```

Backup:

```bash
make backup
```

O mundo e as configuracoes ficam em `./data`. Backups ficam em `./backups`.

No servidor Craftlandia/Paper, o mundo e os plugins ficam em
`./craftlandia-data`. Backups ficam em `./craftlandia-backups`.

Backup do Craftlandia/Paper:

```bash
make craft-backup
```

Backup do Craftlandia/Paper para nuvem, depois de configurar `rclone` e
`CRAFT_CLOUD_REMOTE` no `.env`:

```bash
make craft-backup-cloud
```

Agendar backup diario do Craftlandia/Paper ja enviando para a nuvem:

```bash
make install-craft-cloud-backup-cron
```

Subir restaurando o ultimo backup da nuvem se `craftlandia-data` nao existir:

```bash
make craft-start-resilient
```

Restaurar backup do Craftlandia/Paper:

```bash
make craft-backups
make craft-restore FILE="craftlandia-backups/craftlandia-YYYYMMDD-HHMMSS.tar.gz"
```

Restaurar backup:

```bash
make backups
make restore FILE="backups/bedrock-YYYYMMDD-HHMMSS.tar.gz"
```

O restore para o servidor, salva a pasta `data` atual como backup de seguranca,
move a pasta atual para `data.before-restore-YYYYMMDD-HHMMSS`, restaura o
arquivo escolhido e sobe o servidor de novo.

## Backup automatico diario

Para agendar backup todo dia as 05:00 no horario local da maquina/VPS:

```bash
make install-backup-cron
```

O agendamento usa `cron` e chama `./scripts/backup.sh`. Durante o backup, o
servidor para por alguns instantes para gerar um arquivo consistente e depois
volta automaticamente.

Para usar outro horario, passe a expressao cron diretamente:

```bash
./scripts/install-backup-cron.sh "0 4 * * *"
```

Para ver o agendamento:

```bash
make cron
```

Para remover:

```bash
make remove-backup-cron
```

Os logs do backup automatico ficam em `./backups/backup.log`. A quantidade de
dias mantidos e controlada por `BACKUP_KEEP_DAYS` no `.env`.

Para agendar backup diario do Craftlandia/Paper:

```bash
make install-craft-backup-cron
```

Os logs ficam em `./craftlandia-backups/backup.log`. A quantidade de dias
mantidos e controlada por `CRAFT_BACKUP_KEEP_DAYS` no `.env`.

## Liberar firewall em Ubuntu

Na VPS Ubuntu com UFW:

```bash
./scripts/open-firewall-ubuntu.sh
```

Ou manualmente:

```bash
sudo ufw allow 19132/udp
sudo ufw status
```

Na tela da VPS/cloud provider, tambem libere entrada UDP `19132`.

## Como conectar

### Celular e tablet

No Minecraft Bedrock:

1. Abra `Jogar`.
2. Va em `Servidores`.
3. Use `Adicionar servidor`.
4. Endereco: IP publico ou dominio da VPS.
5. Porta: `19132`.

### PS5

O PS5 roda Minecraft Bedrock, mas normalmente nao oferece um campo simples para
adicionar IP de servidor privado como no celular. As opcoes praticas sao:

- usar um app tipo BedrockTogether no celular da mesma rede do PS5 para o
  servidor aparecer como jogo LAN no console;
- usar uma solucao de DNS tipo BedrockConnect;
- usar Minecraft Realms se quiser o fluxo mais simples e oficial para console,
  abrindo mao de controlar a VPS.

Para o plano com VPS, a regra e: o servidor fica publico na VPS, os celulares
entram direto pelo IP, e o PS5 entra por uma dessas pontes.

## Comandos de servidor

Exemplo para mandar mensagem no chat:

```bash
make cmd CMD="say Servidor online"
```

Exemplo para dar permissao de operador dentro do console do servidor:

```bash
make cmd CMD="op Gamertag"
```

## Fontes uteis

- Bedrock Dedicated Server oficial: https://www.minecraft.net/en-us/download/server/bedrock
- Docker Bedrock Server: https://itzg.github.io/docker-minecraft-docs/bedrock/
