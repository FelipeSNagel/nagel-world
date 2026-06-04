# Servidor Craftlandia/Paper

Este modo roda um servidor Paper Java com Geyser/Floodgate para jogadores
Bedrock entrarem por celular, console e Windows Bedrock.

## Plugins base

- Geyser: entrada de jogadores Bedrock.
- Floodgate: login Bedrock sem conta Java.
- LuckPerms: grupos e permissoes.
- EssentialsX + EssentialsX Spawn: `/home`, `/spawn`, `/tpa`, `/pay`, economia.
- GriefPrevention: protecao de terreno por claim.
- VaultUnlocked: ponte de economia/permissoes para outros plugins.
- ViaVersion + ViaBackwards: compatibilidade de versoes.
- QuickShop-Hikari: lojas em bau, quando compativel com a versao baixada.

## Subir

```bash
make craft-start
make craft-logs
```

O servidor usa UDP `19132` para Bedrock. O target `craft-start` para o Bedrock
vanilla antes de subir o Craftlandia para evitar conflito de porta.

## Primeiro admin

Entre no servidor uma vez e veja o nome exato nos logs. Jogador Bedrock via
Floodgate aparece com prefixo `.`, por exemplo `.FelipeNagel`. Se o Gamertag
tiver espaco, o Floodgate troca por `_`.

Depois rode:

```bash
make craft-op USER=".SeuGamertag"
make craft-cmd CMD="lp user .SeuGamertag permission set luckperms.* true"
```

## Whitelist

Para permitir somente pessoas especificas:

```bash
make craft-whitelist USERS=".FelipeNagel,.OutroGamertag"
```

Use o nome exato que aparece no servidor. Para jogador Bedrock via Floodgate,
normalmente e `.Gamertag`.

Para desligar:

```bash
make craft-disable-whitelist
```

## Permissoes padrao

Depois que o servidor estiver rodando, aplique as permissoes iniciais:

```bash
make craft-setup-perms
```

Isso libera para jogadores comuns:

- `/spawn`
- `/mercado`, `/loja`
- `/sethome`, `/home`, `/delhome`
- `/tpa`, `/tpaccept`, `/tpdeny`
- `/balance`, `/pay`, `/balancetop`
- `/worth`, `/sell hand`, `/sell all`, `/sell inventory`
- aliases em portugues: `/saldo`, `/dinheiro`, `/pagar`, `/preco`,
  `/vender`, `/vendermao`, `/vendertudo`, `/venderinventario`
- claims do GriefPrevention
- comandos basicos do QuickShop, se ele estiver carregado

## Mercado

O deploy cria um warp chamado `mercado` e aliases:

```text
/mercado
/loja
```

Eles levam para uma plataforma de mercado em `x=0, y=90, z=0`. O startup do
servidor tambem cria bancas simples nessa area com blocos, barrels e teto
colorido.

Comandos de venda em portugues:

```text
/vender
/vendermao
/vendertudo
/venderinventario
/preco
/saldo
/dinheiro
/pagar Nome valor
```

Observacao: aliases do `commands.yml` do Paper nao traduzem subcomandos. Por
isso usamos `/vendertudo` em vez de `/vender tudo`.

## Protecao de terreno

O GriefPrevention protege regioes por claim. O fluxo normal e:

1. jogador pega uma pa dourada;
2. marca dois cantos do terreno;
3. somente o dono e jogadores confiados conseguem quebrar/colocar blocos ali.

Comandos comuns:

```text
/trust NomeDoJogador
/untrust NomeDoJogador
/trustlist
/abandonclaim
/claimslist
```

Para proteger spawn como admin, use o modo de admin claim do GriefPrevention e
marque a area do spawn.

## Dinheiro e lojas

EssentialsX fornece a economia base:

```text
/balance
/pay Nome valor
/balancetop
/worth
/sell hand
/sell all
/sell inventory
```

O `/sell` vende itens para o servidor usando a tabela
`plugins/Essentials/worth.yml`. Essa e a entrada inicial de dinheiro: minerar,
farmar e vender itens ao servidor.

QuickShop-Hikari, quando carregado, permite criar lojas em bau. Em geral, o
jogador coloca um bau, segura o item e usa o fluxo do plugin para definir preco.

## Backups

```bash
make craft-backup
make craft-backups
make craft-restore FILE="craftlandia-backups/craftlandia-YYYYMMDD-HHMMSS.tar.gz"
```

## Backup em nuvem e restore automatico

O mundo ativo fica em `craftlandia-data` enquanto o servidor roda. A nuvem deve
guardar os backups `.tar.gz`, nao a pasta viva do mundo.

Configure um remote no `rclone` e depois edite o `.env`:

```env
CRAFT_CLOUD_REMOTE=gdrive
CRAFT_CLOUD_PATH=minecraft-nagel/craftlandia-backups
```

Enviar o backup mais recente para a nuvem:

```bash
make craft-backup-cloud
```

Agendar backup diario ja enviando para a nuvem:

```bash
make install-craft-cloud-backup-cron
```

Remover esse agendamento:

```bash
make remove-craft-cloud-backup-cron
```

Ver qual backup mais recente existe na nuvem:

```bash
make craft-cloud-latest
```

Restaurar o backup mais recente da nuvem:

```bash
make craft-restore-cloud-latest
```

Subir de forma resiliente:

```bash
make craft-start-resilient
```

Esse comando usa `craftlandia-data` se ela existir. Se a pasta nao existir, ele
baixa o backup mais recente da nuvem, restaura e sobe o servidor.

Backup automatico diario as 05:00:

```bash
make install-craft-backup-cron
```

Para outro horario:

```bash
./scripts/install-craftlandia-backup-cron.sh "0 4 * * *"
```

Para remover o agendamento:

```bash
make remove-craft-backup-cron
```
