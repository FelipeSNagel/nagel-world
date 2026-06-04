# Restore amanha na VPS/Coolify

Use este checklist quando for recriar o servidor pelo Git no Coolify.

## 1. Garantir que o backup atual esta no Drive

No Mac, dentro do projeto:

```bash
cd /Users/felipenagel/Projects/games/minecraft-nagel-servers
make craft-backup-cloud
make craft-cloud-latest
```

O segundo comando deve mostrar o ultimo arquivo
`craftlandia-YYYYMMDD-HHMMSS.tar.gz`.

## 2. Gerar a variavel do Google Drive

No Mac:

```bash
base64 < ~/.config/rclone/rclone.conf | tr -d '\n' | pbcopy
```

Isso copia o valor para colar no Coolify. Nao cole esse valor no GitHub nem em
chat.

## 3. Criar o recurso no Coolify

No Coolify:

1. New Resource.
2. Private Repository (with GitHub App).
3. Repository: `FelipeSNagel/nagel-world`.
4. Branch: `main`.
5. Compose file: `docker-compose.coolify.auto-restore.yml`.

## 4. Variaveis no Coolify

Configure estas variaveis no recurso:

```env
RCLONE_CONFIG_B64=cole_aqui_o_valor_copiado_no_passo_2
CRAFT_RCON_PASSWORD=troque-por-uma-senha-forte
CRAFT_MEMORY=2G
RESTORE_MODE=force
```

Use `RESTORE_MODE=force` apenas nesse primeiro deploy de restore. Ele forca o
compose a baixar o ultimo backup do Drive e sobrescrever o mundo atual da VPS.

## 5. Deploy

Clique em Deploy no Coolify.

Nos logs, procure:

```text
Restore complete.
```

Depois disso o container `craftlandia` deve iniciar.

## 6. Depois que funcionar

Remova a variavel:

```env
RESTORE_MODE=force
```

Ou troque para:

```env
RESTORE_MODE=once
```

Isso evita que um redeploy futuro sobrescreva o mundo com o backup antigo.

## 7. Porta

Libere UDP `19132` no firewall da Hostinger/VPS.

Conexao no Minecraft Bedrock:

```text
31.97.252.93
19132
```

